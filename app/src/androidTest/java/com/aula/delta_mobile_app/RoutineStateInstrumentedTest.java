package com.aula.delta_mobile_app;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
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

import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.RoutineActions;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.view.fragments.RoutineDeleteDialogFragment;
import com.aula.delta_mobile_app.view.fragments.RoutineEditorDialogFragment;
import com.aula.delta_mobile_app.view.fragments.RoutineFragment;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class RoutineStateInstrumentedTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private static final class Gateway implements RoutineActions {
        final List<Consumer<ScreenState<RoutineData>>> loads = new ArrayList<>();
        final List<Consumer<ScreenState<RoutineData.Habit>>> details = new ArrayList<>(), creates = new ArrayList<>();
        final List<Consumer<ScreenState<Void>>> deletes = new ArrayList<>();

        @Override
        public void load(Consumer<ScreenState<RoutineData>> callback) {
            loads.add(callback);
        }

        @Override
        public void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback) {
            details.add(callback);
        }

        @Override
        public void create(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
            creates.add(callback);
        }

        @Override
        public void delete(int id, int userId, Consumer<ScreenState<Void>> callback) {
            deletes.add(callback);
        }
    }

    private final Gateway gateway = new Gateway();
    private ActivityScenario<LoginActivity> scenario;
    private Application application;
    private volatile RoutineViewModel model;
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
                            return type.cast(new RoutineViewModel(gateway, CLOCK, action -> main.post(action), new MutableLiveData<>(true), () -> true));
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
    public void openControlledRoutineWithoutProductionNetwork() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(lifecycle);
        scenario = ActivityScenario.launch(new Intent(context, LoginActivity.class).putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true));
        scenario.onActivity(activity -> {
            NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
            ((FragmentNavigator.Destination) host.getNavController().getGraph().findNode(R.id.loginFragment)).setClassName(RoutineFragment.class.getName());
            host.getNavController().navigate(R.id.loginFragment, null, new NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build());
        });
        await(() -> routine() != null && routine().getView() != null && model != null && gateway.loads.size() == 1);
        replyLoad(0, data(response(105, 73, 1, "Banho longo", "07:00")));
        await(() -> model.state().getValue().reading.status == ScreenState.Status.SUCCESS && model.state().getValue().day.executions.size() == 1);
    }

    @After
    public void closeHost() {
        if (scenario != null) scenario.close();
        if (application != null) application.unregisterActivityLifecycleCallbacks(lifecycle);
    }

    @Test
    public void rotationKeepsAgendaSelectionAndDraftDuringPendingCreate() throws Exception {
        RoutineViewModel original = model;
        LocalDate selected = LocalDate.of(2026, 11, 2);
        run(() -> {
            model.selectDate(selected);
            assertTrue(model.beginCreateForm());
            model.setFormHabit(4);
            model.selectFormDayDerived(DayOfWeek.MONDAY, true);
            model.putFormTime(DayOfWeek.MONDAY, -1, "20:00");
            new RoutineEditorDialogFragment().showNow(routine().getChildFragmentManager(), "routine_editor");
            model.saveForm();
        });
        await(() -> editor() != null && editor().getView() != null && model.state().getValue().creation.busy());
        capture("routine_editor_pending.png");
        scenario.recreate();
        await(() -> editor() != null && editor().getView() != null && model.state().getValue().creation.busy());
        run(() -> {
            assertSame(original, model);
            assertEquals(selected, model.state().getValue().selectedDate);
            assertEquals("20:00", model.state().getValue().form.days.get(DayOfWeek.MONDAY).times.get(0));
            assertEquals(1, gateway.creates.size());
        });
        RoutineData.Habit created = data(response(106, 73, 4, "Regar plantas", "20:00")).habits.get(0);
        run(() -> gateway.creates.get(0).accept(ScreenState.success(created)));
        await(() -> gateway.loads.size() == 2);
        replyLoad(1, data(response(105, 73, 1, "Banho longo", "07:00"), response(106, 73, 4, "Regar plantas", "20:00")));
        await(() -> model.state().getValue().form == null && model.state().getValue().day.executions.size() == 2);
        capture("routine_agenda_after_create.png");
    }

    @Test
    public void deleteConfirmationSurvivesRotationAndCancelNeverDeletes() throws Exception {
        RoutineViewModel original = model;
        openDelete();
        scenario.recreate();
        await(() -> deleteDialog() != null && deleteDialog().getView() != null);
        run(() -> {
            assertSame(original, model);
            assertEquals(105, model.state().getValue().deleteTarget.id);
            assertTrue(gateway.deletes.isEmpty());
        });
        clickDeleteButton(R.id.routine_delete_cancel);
        await(() -> model.state().getValue().deleteTarget == null);
        assertTrue(gateway.deletes.isEmpty());
        openDelete();
        capture("routine_delete_confirmation.png");
        clickDeleteButton(R.id.routine_delete_confirm);
        await(() -> model.state().getValue().deletion.busy() && gateway.deletes.size() == 1);
        scenario.recreate();
        await(() -> deleteDialog() != null && deleteDialog().getView() != null && model.state().getValue().deletion.busy());
        run(() -> {
            assertSame(original, model);
            assertEquals(1, gateway.deletes.size());
            gateway.deletes.get(0).accept(ScreenState.success(null));
        });
        await(() -> gateway.loads.size() == 2);
        replyLoad(1, RoutineData.from(Collections.emptyList(), CLOCK));
        await(() -> model.state().getValue().deleteTarget == null && model.state().getValue().day.confirmedEmpty());
    }

    private void openDelete() {
        run(() -> assertTrue(model.openDetails(105, LocalTime.of(7, 0))));
        await(() -> !gateway.details.isEmpty());
        int request = gateway.details.size() - 1;
        run(() -> gateway.details.get(request).accept(ScreenState.success(data(response(105, 73, 1, "Banho longo", "07:00")).habits.get(0))));
        await(() -> model.state().getValue().details != null);
        run(() -> {
            assertTrue(model.requestDelete());
            new RoutineDeleteDialogFragment().showNow(routine().getChildFragmentManager(), "routine_delete");
        });
        await(() -> deleteDialog() != null && deleteDialog().getView() != null);
    }

    private RoutineFragment routine() {
        LoginActivity activity = activity();
        if (activity == null) return null;
        NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
        Fragment current = host == null ? null : host.getChildFragmentManager().getPrimaryNavigationFragment();
        return current instanceof RoutineFragment ? (RoutineFragment) current : null;
    }

    private RoutineEditorDialogFragment editor() {
        RoutineFragment parent = routine();
        Fragment found = parent == null ? null : parent.getChildFragmentManager().findFragmentByTag("routine_editor");
        return found instanceof RoutineEditorDialogFragment ? (RoutineEditorDialogFragment) found : null;
    }

    private RoutineDeleteDialogFragment deleteDialog() {
        RoutineFragment parent = routine();
        Fragment found = parent == null ? null : parent.getChildFragmentManager().findFragmentByTag("routine_delete");
        return found instanceof RoutineDeleteDialogFragment ? (RoutineDeleteDialogFragment) found : null;
    }

    private void clickDeleteButton(int id) {
        run(() -> {
            assertNotNull(deleteDialog());
            assertNotNull(deleteDialog().getView());
            assertTrue(deleteDialog().getView().findViewById(id).performClick());
        });
    }

    private LoginActivity activity() {
        LoginActivity[] result = {null};
        if (scenario != null) scenario.onActivity(value -> result[0] = value);
        return result[0];
    }

    private void replyLoad(int request, RoutineData value) {
        run(() -> gateway.loads.get(request).accept(ScreenState.success(value)));
        await(() -> model.state().getValue().reading.status == ScreenState.Status.SUCCESS);
    }

    private RoutineData data(UserHabitResponse... values) {
        return RoutineData.from(Arrays.asList(values), CLOCK);
    }

    private UserHabitResponse response(int id, int user, int type, String name, String... times) {
        UserHabitResponse value = new UserHabitResponse();
        value.id = id;
        value.userId = user;
        value.habitId = type;
        value.habitName = name;
        value.frequency = times.length;
        value.daysOfWeek = Collections.singletonList(1);
        UserHabitScheduleResponse schedule = new UserHabitScheduleResponse();
        schedule.dayOfWeek = 1;
        schedule.startTimes = Arrays.asList(times);
        value.schedule = Collections.singletonList(schedule);
        return value;
    }

    private void capture(String name) throws IOException {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("routine-evidence");
        assertNotNull(directory);
        assertTrue(directory.exists() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
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
        fail("Timed out waiting for routine state");
    }
}
