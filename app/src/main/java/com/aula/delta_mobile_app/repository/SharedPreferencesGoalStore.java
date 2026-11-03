package com.aula.delta_mobile_app.repository;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesGoalStore implements MonthlyGoalStorage.Store {
    public static final String PREFERENCES = "delta_monthly_goals";
    private final SharedPreferences preferences;

    public SharedPreferencesGoalStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    @Override public String read(String key) { return preferences.getString(key, null); }
    @Override public boolean write(String key, String value) { return preferences.edit().putString(key, value).commit(); }
}
