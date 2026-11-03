package com.aula.delta_mobile_app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
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

import java.lang.reflect.Field;
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
            model.selectFormDayDerived(DayOfWeek.MONDAY, true);
            model.putFormTime(DayOfWeek.MONDAY, -1, "07:00");
            new RoutineEditorDialogFragment().showNow(parent.getChildFragmentManager(), "routine_form_test");
        });
        await(() -> editor() != null && editor().getView() != null && editor().requireDialog().getWindow().getDecorView().hasWindowFocus());
    }

    @After
    public void closeHost() {
        if (scenario != null) scenario.close();
        if (application != null) application.unregisterActivityLifecycleCallbacks(lifecycle);
    }

    @Test
    public void rotationKeepsSelectedDayTimeAndSimpleSummary() {
        RoutineViewModel original = model;
        RoutineEditorDialogFragment[] before = {null};
        run(() -> before[0] = editor());
        scenario.recreate();
        await(() -> editor() != null && editor() != before[0] && editor().getView() != null);
        run(() -> {
            assertSame(original, model);
            RoutineFormState form = model.state().getValue().form;
            assertEquals("1", form.frequency);
            assertEquals("07:00", form.days.get(DayOfWeek.MONDAY).times.get(0));
            LinearLayout schedule = editor().requireView().findViewById(R.id.routine_form_schedule);
            assertEquals(1, schedule.getChildCount());
            assertTrue(((TextView) editor().requireView().findViewById(R.id.routine_form_summary)).getText().toString().contains("1 execução"));
        });
    }

    @Test
    public void saveValidatesAutomaticallyAndDiscardConfirmationPreservesDraft() {
        run(() -> {
            model.selectFormDayDerived(DayOfWeek.TUESDAY, true);
            editor().requireView().findViewById(R.id.routine_form_save).performClick();
        });
        await(() -> model.state().getValue().form.errors.containsKey(RoutineFormState.quantityKey(DayOfWeek.TUESDAY)));
        run(() -> {
            LinearLayout schedule = editor().requireView().findViewById(R.id.routine_form_schedule);
            View tuesday = schedule.getChildAt(1);
            assertEquals(View.VISIBLE, tuesday.findViewById(R.id.routine_schedule_error).getVisibility());
            model.requestFormClose(model.state().getValue().form.token);
        });
        await(() -> confirmation() != null && confirmation().isShowing());
        clickConfirmation(DialogInterface.BUTTON_NEGATIVE);
        run(() -> {
            assertNotNull(model.state().getValue().form);
            assertFalse(model.state().getValue().form.confirmDiscard);
            model.requestFormClose(model.state().getValue().form.token);
        });
        await(() -> confirmation() != null && confirmation().isShowing());
        clickConfirmation(DialogInterface.BUTTON_POSITIVE);
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

    private AlertDialog confirmation() {
        try {
            Field field = RoutineEditorDialogFragment.class.getDeclaredField("confirmation");
            field.setAccessible(true);
            return (AlertDialog) field.get(editor());
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private void clickConfirmation(int which) {
        run(() -> {
            AlertDialog dialog = confirmation();
            assertNotNull(dialog);
            assertTrue(dialog.getButton(which).performClick());
        });
    }

    private void run(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

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
