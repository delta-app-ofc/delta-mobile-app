package com.aula.delta_mobile_app.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RoutineScheduleStore {
    private static final String PREFERENCES = "delta_routine_schedules";
    private static final String DATA = "data";
    private final SharedPreferences preferences;
    private final Gson gson = GsonProvider.getInstance();
    private final Type mapType = new TypeToken<Map<String, Entry>>() {}.getType();

    RoutineScheduleStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    void apply(int userId, List<UserHabitResponse> values) {
        Map<String, Entry> entries = read();
        for (UserHabitResponse value : values) {
            if (value == null || value.id == null || (value.schedule != null && !value.schedule.isEmpty())) continue;
            Entry entry = entries.get(key(userId, value.id));
            if (entry == null || entry.schedule == null || entry.schedule.isEmpty()) continue;
            value.frequency = entry.frequency;
            value.daysOfWeek = new ArrayList<>(entry.daysOfWeek);
            value.schedule = copy(entry.schedule);
        }
    }

    void save(int userId, int habitId, Integer frequency, List<Integer> daysOfWeek, List<UserHabitScheduleResponse> schedule) {
        if (userId <= 0 || habitId <= 0 || schedule == null || schedule.isEmpty()) return;
        Map<String, Entry> entries = read();
        entries.put(key(userId, habitId), new Entry(frequency, daysOfWeek, schedule));
        write(entries);
    }

    void remove(int userId, int habitId) {
        Map<String, Entry> entries = read();
        if (entries.remove(key(userId, habitId)) != null) write(entries);
    }

    private Map<String, Entry> read() {
        String json = preferences.getString(DATA, null);
        if (json == null || json.trim().isEmpty()) return new LinkedHashMap<>();
        try {
            Map<String, Entry> result = gson.fromJson(json, mapType);
            return result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
        } catch (RuntimeException invalid) {
            return new LinkedHashMap<>();
        }
    }

    private void write(Map<String, Entry> entries) {
        preferences.edit().putString(DATA, gson.toJson(entries, mapType)).apply();
    }

    private static String key(int userId, int habitId) {
        return userId + ":" + habitId;
    }

    private static List<UserHabitScheduleResponse> copy(List<UserHabitScheduleResponse> source) {
        List<UserHabitScheduleResponse> result = new ArrayList<>();
        for (UserHabitScheduleResponse value : source) {
            if (value == null) continue;
            UserHabitScheduleResponse item = new UserHabitScheduleResponse();
            item.dayOfWeek = value.dayOfWeek;
            item.startTimes = value.startTimes == null ? Collections.emptyList() : new ArrayList<>(value.startTimes);
            result.add(item);
        }
        return result;
    }

    private static final class Entry {
        Integer frequency;
        List<Integer> daysOfWeek;
        List<UserHabitScheduleResponse> schedule;

        Entry() {}

        Entry(Integer frequency, List<Integer> daysOfWeek, List<UserHabitScheduleResponse> schedule) {
            this.frequency = frequency;
            this.daysOfWeek = daysOfWeek == null ? Collections.emptyList() : new ArrayList<>(daysOfWeek);
            this.schedule = copy(schedule);
        }
    }
}
