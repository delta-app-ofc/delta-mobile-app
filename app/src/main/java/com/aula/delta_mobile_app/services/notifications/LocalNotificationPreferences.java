package com.aula.delta_mobile_app.services.notifications;

import android.content.Context;
import android.content.SharedPreferences;

public final class LocalNotificationPreferences {
    private static final String FILE = "delta_local_notifications";
    private static final String ENABLED = "enabled";
    private static final String PENDING_ENABLE = "pending_enable";
    private static final String USER_ID = "user_id";
    private static final String RULES = "rules";

    private LocalNotificationPreferences() {}

    static SharedPreferences get(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return get(context).getBoolean(ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(ENABLED, enabled).apply();
    }

    public static boolean pendingEnable(Context context) {
        return get(context).getBoolean(PENDING_ENABLE, false);
    }

    public static void setPendingEnable(Context context, boolean pending) {
        get(context).edit().putBoolean(PENDING_ENABLE, pending).apply();
    }

    static int userId(Context context) {
        return get(context).getInt(USER_ID, 0);
    }

    static String rules(Context context) {
        return get(context).getString(RULES, "[]");
    }

    static void saveRules(Context context, int userId, String rules) {
        get(context).edit().putInt(USER_ID, userId).putString(RULES, rules).apply();
    }

    static void clearRules(Context context) {
        get(context).edit().remove(USER_ID).putString(RULES, "[]").apply();
    }
}
