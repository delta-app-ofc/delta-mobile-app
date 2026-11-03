package com.aula.delta_mobile_app.services.notifications;

import android.content.Context;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.RoutineRepository;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RoutineReminderSync {
    private static final AtomicBoolean LOADING = new AtomicBoolean();

    private RoutineReminderSync() {}

    public static void refresh(Context context) {
        Context app = context.getApplicationContext();
        if (!RoutineReminderScheduler.enabled(app)) return;
        AuthRepository auth = AuthRepository.getInstance(app);
        if (!auth.hasSession()) {
            RoutineReminderScheduler.cancelAll(app);
            return;
        }
        if (!LOADING.compareAndSet(false, true)) return;
        long session = auth.sessionGeneration();
        try {
            new RoutineRepository(auth).load(result -> {
                LOADING.set(false);
                if (session != auth.sessionGeneration() || !auth.hasSession()) return;
                if (result != null && result.status == ScreenState.Status.SUCCESS) {
                    RoutineData data = result.data;
                    if (data != null) RoutineReminderScheduler.reconcile(app, data, true);
                }
            });
        } catch (RuntimeException failure) {
            LOADING.set(false);
        }
    }
}
