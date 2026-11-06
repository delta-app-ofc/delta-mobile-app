package com.aula.delta_mobile_app.services.notifications;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.view.activities.main.MainActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoutineReminderScheduler {
    static final String CHANNEL_ID = "routine_reminders";
    static final String ACTION_REMINDER = "com.aula.delta_mobile_app.action.ROUTINE_REMINDER";
    static final String EXTRA_RULE_KEY = "routine_reminder_rule_key";
    static final String EXTRA_TRIGGER_AT = "routine_reminder_trigger_at";
    private static final Type RULE_LIST = new TypeToken<List<RoutineReminderRule>>() {}.getType();
    private static final Gson GSON = new Gson();

    private RoutineReminderScheduler() {}

    public static synchronized void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    public static boolean enabled(Context context) {
        return LocalNotificationPreferences.enabled(context);
    }

    public static synchronized void enable(Context context) {
        LocalNotificationPreferences.setEnabled(context, true);
        LocalNotificationPreferences.setPendingEnable(context, false);
        scheduleStored(context, true);
    }

    public static synchronized void disable(Context context) {
        LocalNotificationPreferences.setEnabled(context, false);
        LocalNotificationPreferences.setPendingEnable(context, false);
        List<RoutineReminderRule> rules = loadRules(context);
        cancelAlarms(context, rules);
        cancelNotifications(context, rules);
    }

    public static synchronized void cancelAll(Context context) {
        List<RoutineReminderRule> rules = loadRules(context);
        cancelAlarms(context, rules);
        cancelNotifications(context, rules);
        LocalNotificationPreferences.setEnabled(context, false);
        LocalNotificationPreferences.setPendingEnable(context, false);
        LocalNotificationPreferences.clearRules(context);
    }

    public static synchronized void reconcile(Context context, RoutineData data) {
        reconcile(context, data, false);
    }

    public static synchronized void reconcile(Context context, RoutineData data, boolean rescheduleUnchanged) {
        if (data == null) return;
        Context app = context.getApplicationContext();
        if (data.habits.isEmpty()) {
            int oldUser = LocalNotificationPreferences.userId(app);
            cancelAlarms(app, loadRules(app));
            LocalNotificationPreferences.saveRules(app, oldUser, "[]");
            return;
        }
        int userId = data.habits.get(0).userId;
        if (userId <= 0) return;
        List<RoutineReminderRule> previous = loadRules(app);
        int previousUser = LocalNotificationPreferences.userId(app);
        Map<String, RoutineReminderRule> merged = new LinkedHashMap<>();
        if (previousUser == userId) {
            for (RoutineReminderRule rule : previous) if (rule != null && rule.valid()) merged.put(rule.key(), rule);
        } else cancelAlarms(app, previous);
        Set<Integer> returnedHabits = new HashSet<>();
        for (RoutineData.Habit habit : data.habits) {
            if (habit == null || habit.userId != userId || habit.id <= 0) continue;
            returnedHabits.add(habit.id);
            if (!habit.scheduleComplete) continue;
            removeHabit(merged, habit.id);
            for (RoutineReminderRule rule : rulesFor(habit)) merged.put(rule.key(), rule);
        }
        merged.values().removeIf(rule -> rule.userId != userId || !returnedHabits.contains(rule.habitId));
        List<RoutineReminderRule> next = sorted(merged.values());
        String oldJson = LocalNotificationPreferences.rules(app);
        String nextJson = GSON.toJson(next, RULE_LIST);
        if (previousUser != userId || !oldJson.equals(nextJson)) {
            cancelAlarms(app, previous);
            LocalNotificationPreferences.saveRules(app, userId, nextJson);
            if (enabled(app)) scheduleRules(app, next);
        } else if (enabled(app)) {
            if (hasBothPermissions(app)) {
                if (rescheduleUnchanged) scheduleRules(app, next);
            }
            else cancelAlarms(app, next);
        }
    }

    public static synchronized void replaceHabit(Context context, RoutineData.Habit habit) {
        if (habit == null || habit.userId <= 0 || habit.id <= 0 || !habit.scheduleComplete) return;
        Context app = context.getApplicationContext();
        if (!enabled(app) && LocalNotificationPreferences.userId(app) != habit.userId) return;
        List<RoutineReminderRule> previous = loadRules(app);
        int previousUser = LocalNotificationPreferences.userId(app);
        Map<String, RoutineReminderRule> next = new LinkedHashMap<>();
        if (previousUser == habit.userId) {
            for (RoutineReminderRule rule : previous) if (rule != null && rule.valid()) next.put(rule.key(), rule);
        } else cancelAlarms(app, previous);
        removeHabit(next, habit.id);
        for (RoutineReminderRule rule : rulesFor(habit)) next.put(rule.key(), rule);
        List<RoutineReminderRule> rules = sorted(next.values());
        cancelAlarms(app, previous);
        LocalNotificationPreferences.saveRules(app, habit.userId, GSON.toJson(rules, RULE_LIST));
        if (enabled(app) && hasBothPermissions(app)) scheduleRules(app, rules);
        else cancelAlarms(app, rules);
    }

    public static synchronized void removeHabit(Context context, int userId, int habitId) {
        Context app = context.getApplicationContext();
        if (LocalNotificationPreferences.userId(app) != userId) return;
        List<RoutineReminderRule> previous = loadRules(app);
        Map<String, RoutineReminderRule> next = new LinkedHashMap<>();
        for (RoutineReminderRule rule : previous) if (rule != null && rule.valid()) next.put(rule.key(), rule);
        removeHabit(next, habitId);
        List<RoutineReminderRule> rules = sorted(next.values());
        cancelAlarms(app, previous);
        LocalNotificationPreferences.saveRules(app, userId, GSON.toJson(rules, RULE_LIST));
        if (enabled(app) && hasBothPermissions(app)) scheduleRules(app, rules);
    }

    public static synchronized void scheduleStored(Context context, boolean force) {
        Context app = context.getApplicationContext();
        if (!enabled(app)) {
            cancelAlarms(app, loadRules(app));
            return;
        }
        if (!AuthRepository.getInstance(app).hasSession()) {
            cancelAll(app);
            return;
        }
        List<RoutineReminderRule> rules = loadRules(app);
        if (force) cancelAlarms(app, rules);
        if (!hasBothPermissions(app)) {
            cancelAlarms(app, rules);
            return;
        }
        createChannel(app);
        scheduleRules(app, rules);
    }

    static synchronized void onAlarm(Context context, String ruleKey, long triggerAt) {
        Context app = context.getApplicationContext();
        if (!enabled(app) || !AuthRepository.getInstance(app).hasSession()) {
            cancelAll(app);
            return;
        }
        RoutineReminderRule rule = findRule(app, ruleKey);
        if (rule == null || !rule.valid()) return;
        LocalDateTime scheduled = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerAt), ZoneId.systemDefault());
        if (!occursOn(rule, scheduled.toLocalDate()) || !LocalTime.parse(rule.startTime).equals(scheduled.toLocalTime())) return;
        if (canPostNotifications(app) && NotificationManagerCompat.from(app).areNotificationsEnabled()) showNotification(app, rule);
        scheduleNext(app, rule, LocalDateTime.now().plusSeconds(1));
    }

    private static List<RoutineReminderRule> rulesFor(RoutineData.Habit habit) {
        List<RoutineReminderRule> rules = new ArrayList<>();
        if (!habit.scheduleVersions.isEmpty()) {
            for (RoutineData.Habit.ScheduleVersion version : habit.scheduleVersions)
                appendRules(rules, habit, version.schedule, version.effectiveFrom.toString(),
                    version.effectiveUntil == null ? null : version.effectiveUntil.toString());
        } else appendRules(rules, habit, habit.schedule, null, null);
        return rules;
    }

    private static void appendRules(List<RoutineReminderRule> rules, RoutineData.Habit habit,
        Map<java.time.DayOfWeek, List<LocalTime>> schedule, String effectiveFrom, String effectiveUntil) {
        for (Map.Entry<java.time.DayOfWeek, List<LocalTime>> day : schedule.entrySet()) {
            if (day.getKey() == null || day.getValue() == null) continue;
            for (LocalTime time : day.getValue()) {
                if (time == null) continue;
                RoutineReminderRule rule = new RoutineReminderRule();
                rule.userId = habit.userId;
                rule.habitId = habit.id;
                rule.habitName = habit.name == null || habit.name.trim().isEmpty() ? "sua atividade" : habit.name.trim();
                rule.dayOfWeek = day.getKey().getValue();
                rule.startTime = time.toString().length() == 5 ? time.toString() : time.toString().substring(0, 5);
                rule.effectiveFrom = effectiveFrom;
                rule.effectiveUntil = effectiveUntil;
                if (rule.valid()) rules.add(rule);
            }
        }
    }

    private static void scheduleRules(Context context, List<RoutineReminderRule> rules) {
        for (RoutineReminderRule rule : rules) scheduleNext(context, rule, LocalDateTime.now());
    }

    private static void scheduleNext(Context context, RoutineReminderRule rule, LocalDateTime after) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !hasBothPermissions(context)) return;
        ZonedDateTime next = nextOccurrence(rule, after);
        if (next == null) return;
        long triggerAt = next.toInstant().toEpochMilli();
        Intent intent = new Intent(context, RoutineReminderAlarmReceiver.class).setAction(ACTION_REMINDER)
            .setData(Uri.parse("delta-reminder://routine/" + Uri.encode(rule.key())))
            .putExtra(EXTRA_RULE_KEY, rule.key()).putExtra(EXTRA_TRIGGER_AT, triggerAt);
        PendingIntent operation = PendingIntent.getBroadcast(context, rule.key().hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
        } catch (SecurityException ignored) {}
    }

    private static ZonedDateTime nextOccurrence(RoutineReminderRule rule, LocalDateTime after) {
        if (!rule.valid()) return null;
        LocalDate from = rule.effectiveFrom == null ? LocalDate.MIN : LocalDate.parse(rule.effectiveFrom);
        LocalDate until = rule.effectiveUntil == null ? LocalDate.MAX : LocalDate.parse(rule.effectiveUntil);
        LocalDate date = after.toLocalDate().isBefore(from) ? from : after.toLocalDate();
        LocalTime time = LocalTime.parse(rule.startTime);
        for (int i = 0; i <= 7 && !date.isAfter(until); i++, date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() != rule.dayOfWeek) continue;
            ZonedDateTime candidate = date.atTime(time).atZone(ZoneId.systemDefault());
            if (candidate.toLocalDateTime().isAfter(after)) return candidate;
        }
        return null;
    }

    private static boolean occursOn(RoutineReminderRule rule, LocalDate date) {
        if (date.getDayOfWeek().getValue() != rule.dayOfWeek) return false;
        if (rule.effectiveFrom != null && date.isBefore(LocalDate.parse(rule.effectiveFrom))) return false;
        return rule.effectiveUntil == null || !date.isAfter(LocalDate.parse(rule.effectiveUntil));
    }

    private static void showNotification(Context context, RoutineReminderRule rule) {
        createChannel(context);
        String habitName = rule.habitName == null || rule.habitName.trim().isEmpty() ? "sua atividade" : rule.habitName.trim();
        Intent open = new Intent(context, MainActivity.class).setAction(MainActivity.ACTION_OPEN_ROUTINE)
            .putExtra(MainActivity.EXTRA_RESTORE_SESSION, true).putExtra(MainActivity.EXTRA_OPEN_ROUTINE, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder publicVersion = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water_drop)
            .setContentTitle(context.getString(R.string.notification_routine_title))
            .setContentText(context.getString(R.string.notification_routine_private_body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_water_drop)
            .setContentTitle(context.getString(R.string.notification_routine_title))
            .setContentText(context.getString(R.string.notification_routine_body, habitName))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notification_routine_body, habitName)))
            .setContentIntent(content).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion.build());
        try {
            NotificationManagerCompat.from(context).notify(rule.key().hashCode(), notification.build());
        } catch (SecurityException ignored) {}
    }

    private static boolean hasBothPermissions(Context context) {
        if (!canPostNotifications(context)) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = manager == null ? null : manager.getNotificationChannel(CHANNEL_ID);
            return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    private static RoutineReminderRule findRule(Context context, String key) {
        for (RoutineReminderRule rule : loadRules(context)) if (rule != null && rule.key().equals(key)) return rule;
        return null;
    }

    private static List<RoutineReminderRule> loadRules(Context context) {
        try {
            List<RoutineReminderRule> rules = GSON.fromJson(LocalNotificationPreferences.rules(context), RULE_LIST);
            List<RoutineReminderRule> valid = new ArrayList<>();
            if (rules != null) for (RoutineReminderRule rule : rules) if (rule != null && rule.valid()) valid.add(rule);
            return valid;
        } catch (RuntimeException invalid) {
            return new ArrayList<>();
        }
    }

    private static void cancelAlarms(Context context, List<RoutineReminderRule> rules) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        for (RoutineReminderRule rule : rules) {
            if (rule == null) continue;
            Intent intent = new Intent(context, RoutineReminderAlarmReceiver.class).setAction(ACTION_REMINDER)
                .setData(Uri.parse("delta-reminder://routine/" + Uri.encode(rule.key())));
            PendingIntent operation = PendingIntent.getBroadcast(context, rule.key().hashCode(), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (operation != null) {
                manager.cancel(operation);
                operation.cancel();
            }
        }
    }

    private static void cancelNotifications(Context context, List<RoutineReminderRule> rules) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        for (RoutineReminderRule rule : rules) if (rule != null) manager.cancel(rule.key().hashCode());
    }

    public static boolean hasNotificationPermission(Context context) {
        return canPostNotifications(context) && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean hasExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private static List<RoutineReminderRule> sorted(java.util.Collection<RoutineReminderRule> rules) {
        List<RoutineReminderRule> values = new ArrayList<>(rules);
        values.sort(Comparator.comparing(RoutineReminderRule::key));
        return values;
    }

    private static void removeHabit(Map<String, RoutineReminderRule> rules, int habitId) {
        rules.values().removeIf(rule -> rule.habitId == habitId);
    }
}
