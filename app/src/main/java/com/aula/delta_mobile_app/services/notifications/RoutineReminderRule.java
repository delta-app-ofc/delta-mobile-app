package com.aula.delta_mobile_app.services.notifications;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;

public final class RoutineReminderRule {
    public int userId;
    public int habitId;
    public int dayOfWeek;
    public String habitName;
    public String startTime;
    public String effectiveFrom;
    public String effectiveUntil;

    public RoutineReminderRule() {}

    public String key() {
        return userId + ":" + habitId + ":" + dayOfWeek + ":" + startTime + ":" + value(effectiveFrom) + ":" + value(effectiveUntil);
    }

    public boolean valid() {
        if (userId <= 0 || habitId <= 0 || dayOfWeek < 1 || dayOfWeek > 7 || startTime == null) return false;
        try {
            LocalTime.parse(startTime);
            LocalDate from = effectiveFrom == null ? null : LocalDate.parse(effectiveFrom);
            LocalDate until = effectiveUntil == null ? null : LocalDate.parse(effectiveUntil);
            return until == null || from == null || !until.isBefore(from);
        } catch (DateTimeException invalid) {
            return false;
        }
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
