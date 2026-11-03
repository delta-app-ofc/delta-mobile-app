package com.aula.delta_mobile_app.services.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RoutineReminderAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !RoutineReminderScheduler.ACTION_REMINDER.equals(intent.getAction())) return;
        RoutineReminderScheduler.onAlarm(context, intent.getStringExtra(RoutineReminderScheduler.EXTRA_RULE_KEY),
            intent.getLongExtra(RoutineReminderScheduler.EXTRA_TRIGGER_AT, 0));
    }
}
