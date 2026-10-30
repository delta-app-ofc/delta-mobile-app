package com.aula.delta_mobile_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.RoutineFormValidator;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.RoutineActions;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.view.fragments.RoutineEditorDialogFragment;
import com.aula.delta_mobile_app.view.fragments.RoutineFragment;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.DayOfWeek;
import java.util.Collections;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class RoutineFormInstrumentedTest {
    private ActivityScenario<LoginActivity> scenario;
    private Application application;
    private volatile RoutineViewModel model;
    private final Clock clock = Clock.systemUTC();
    private final Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityPreCreated(@NonNull Activity activity, Bundle saved) {
            if (!(activity instanceof FragmentActivity)) return;
            ((FragmentActivity) activity).getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentPreCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment, Bundle state) {
                    if (!(fragment instanceof RoutineFragment)) return;
                    model = new ViewModelProvider(fragment, new ViewModelProvider.Factory() {
                        @NonNull
                        @Override
                        public <T extends ViewModel> T create(@NonNull Class<T> type) {
                            Handler main = new Handler(Looper.getMainLooper());
                            RoutineActions source = new RoutineActions() {
                                @Override
                                public void load(Consumer<ScreenState<RoutineData>> callback) {
                                    callback.accept(ScreenState.success(RoutineData.from(Collections.emptyList(), clock)));
                                }

                                @Override
                                public void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback) {
                                    throw new AssertionError("Unexpected detail request");
                                }
                            };
                            return type.cast(new RoutineViewModel(source, clock, action -> main.post(action), new MutableLiveData<>(true), () -> true));
                        }
                    }).get(RoutineViewModel.class);
                }
            }, true);
        }

        @Override public void onActivityCreated(@NonNull Activity activity, Bundle saved) {}
        @Override public void onActivityStarted(@NonNull Activity activity) {}
        @Override public void onActivityResumed(@NonNull Activity activity) {}
        @Override public void onActivityPaused(@NonNull Activity activity) {}
        @Override public void onActivityStopped(@NonNull Activity activity) {}
        @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle saved) {}
        @Override public void onActivityDestroyed(@NonNull Activity activity) {}
    };

    @Before
    public void openControlledEditorWithoutProductionEntryPointOrNetwork() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(lifecycle);
        scenario = ActivityScenario.launch(new Intent(context, LoginActivity.class).putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true));
        scenario.onActivity(activity -> {
            NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
            ((FragmentNavigator.Destination) host.getNavController().getGraph().findNode(R.id.loginFragment)).setClassName(RoutineFragment.class.getName());
            host.getNavController().navigate(R.id.loginFragment, null, new NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build());
            host.getChildFragmentManager().executePendingTransactions();
            RoutineFragment parent = (RoutineFragment) host.getChildFragmentManager().getPrimaryNavigationFragment();
            assertTrue(model.beginCreateForm());
            model.setFormHabit(1);
            model.setFormFrequency("02");
            model.selectFormDay(DayOfWeek.MONDAY, true);
            model.setFormQuantity(DayOfWeek.MONDAY, "2");
            model.setFormTime(DayOfWeek.MONDAY, 0, "07:00");
            model.setFormTime(DayOfWeek.MONDAY, 1, "7:0");
            model.validateForm();
            new RoutineEditorDialogFragment().showNow(parent.getChildFragmentManager(), "routine_form_test");
        });
        await(() -> editor() != null && editor().getView() != null);
    }

    @After
    public void closeHost() {
        if (scenario != null) scenario.close();
        if (application != null) application.unregisterActivityLifecycleCallbacks(lifecycle);
    }

    @Test
    public void rotationKeepsRawErrorsCursorAndKeyboardWithLastFieldAndActionReachable() {
        RoutineViewModel original = model;
        RoutineEditorDialogFragment[] originalEditor = {null};
        run(() -> originalEditor[0] = editor());
        run(() -> fields().scrollToPosition(3));
        await(() -> fields().findViewHolderForAdapterPosition(3) != null);
        run(() -> {
            EditText input = row(3);
            input.requestFocus();
            input.setSelection(1);
            ((InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
        await(this::keyboardVisible);
        int[] expected = {0};
        scenario.onActivity(activity -> {
            boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            expected[0] = landscape ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            activity.setRequestedOrientation(landscape ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });
        await(() -> editor() != null && editor() != originalEditor[0] && editor().getView() != null && editor().getResources().getConfiguration().orientation == expected[0]
            && editor().requireView().findFocus() instanceof EditText && keyboardVisible());
        run(() -> {
            assertSame(original, model);
            RoutineFormState form = model.state().getValue().form;
            assertEquals("02", form.frequency);
            assertEquals("7:0", form.days.get(DayOfWeek.MONDAY).times.get(1));
            assertEquals(RoutineFormValidator.Error.INVALID_TIME, form.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 1)));
            EditText input = (EditText) editor().requireView().findFocus();
            assertEquals(RoutineFormState.timeKey(DayOfWeek.MONDAY, 1), input.getTag());
            assertEquals(1, input.getSelectionStart());
            input.requestRectangleOnScreen(new Rect(0, 0, input.getWidth(), input.getHeight()), true);
        });
        await(() -> {
            Rect input = new Rect(), action = new Rect();
            View focused = editor().requireView().findFocus();
            return focused != null && focused.getGlobalVisibleRect(input) && input.height() == focused.getHeight()
                && editor().requireView().findViewById(R.id.routine_form_validate).getGlobalVisibleRect(action) && action.height() >= 48 * editor().getResources().getDisplayMetrics().density;
        });
    }

    @Test
    public void typingDoesNotResetCursorAndDiscardConfirmationSurvivesRecreation() {
        run(() -> fields().scrollToPosition(3));
        await(() -> fields().findViewHolderForAdapterPosition(3) != null);
        run(() -> {
            EditText input = row(3);
            input.setText("20:00");
            input.requestFocus();
            input.setSelection(2);
            model.setFormFrequency("002");
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        run(() -> {
            assertEquals(2, row(3).getSelectionStart());
            assertEquals("20:00", model.state().getValue().form.days.get(DayOfWeek.MONDAY).times.get(1));
            editor().requireView().findViewById(R.id.routine_form_validate).performClick();
            assertTrue(model.state().getValue().form.valid());
            model.requestFormClose(model.state().getValue().form.token);
        });
        scenario.recreate();
        onView(withText(R.string.routine_form_continue)).perform(click());
        run(() -> {
            assertEquals("002", model.state().getValue().form.frequency);
            assertFalse(model.state().getValue().form.confirmDiscard);
            model.requestFormClose(model.state().getValue().form.token);
        });
        onView(withText(R.string.routine_form_discard)).perform(click());
        run(() -> assertNull(model.state().getValue().form));
    }

    private RoutineEditorDialogFragment editor() {
        NavHostFragment host = (NavHostFragment) scenarioActivity().getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
        Fragment parent = host.getChildFragmentManager().getPrimaryNavigationFragment();
        return parent == null ? null : (RoutineEditorDialogFragment) parent.getChildFragmentManager().findFragmentByTag("routine_form_test");
    }

    private LoginActivity scenarioActivity() {
        LoginActivity[] result = {null};
        scenario.onActivity(activity -> result[0] = activity);
        return result[0];
    }

    private RecyclerView fields() { return editor().requireView().findViewById(R.id.routine_form_fields); }

    private EditText row(int position) { return fields().findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.routine_execution_input); }

    private boolean keyboardVisible() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(editor().requireView());
        return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
    }

    private void run(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }

    private void await(BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            run(() -> ready[0] = condition.getAsBoolean());
            if (ready[0]) return;
            SystemClock.sleep(50);
        }
        fail("Timed out waiting for editor state");
    }
}
