package com.aula.delta_mobile_app.model;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RoutineFormValidator {
    public enum Error { REQUIRED, INVALID_TYPE, INVALID_NUMBER, POSITIVE, TOO_MANY_TIMES, INVALID_TIME, DUPLICATE_TIME, FREQUENCY_MISMATCH }

    public static final int MINUTES_PER_DAY = 24 * 60;

    private RoutineFormValidator() {}

    public static Map<String, Error> validate(RoutineFormState form) {
        Map<String, Error> errors = new LinkedHashMap<>();
        if (form.habitId == null) errors.put(RoutineFormState.TYPE, Error.REQUIRED);
        else if (form.habitId < 1 || form.habitId > 6) errors.put(RoutineFormState.TYPE, Error.INVALID_TYPE);
        Integer frequency = count(form.frequency, RoutineFormState.FREQUENCY, errors);
        boolean selected = false, complete = true;
        long sum = 0;
        for (DayOfWeek day : DayOfWeek.values()) {
            RoutineFormState.DayDraft draft = form.days.get(day);
            if (!draft.selected) continue;
            selected = true;
            String quantityKey = RoutineFormState.quantityKey(day);
            Integer quantity = count(draft.quantity, quantityKey, errors);
            if (quantity == null || quantity < 1) {
                complete = false;
                continue;
            }
            if (quantity > MINUTES_PER_DAY) {
                errors.put(quantityKey, Error.TOO_MANY_TIMES);
                complete = false;
                continue;
            }
            sum += quantity;
            Map<String, Integer> seen = new HashMap<>();
            for (int i = 0; i < quantity; i++) {
                String value = i < draft.times.size() ? draft.times.get(i) : "";
                String key = RoutineFormState.timeKey(day, i);
                if (value.trim().isEmpty()) errors.put(key, Error.REQUIRED);
                else if (!value.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) errors.put(key, Error.INVALID_TIME);
                else {
                    Integer previous = seen.putIfAbsent(value, i);
                    if (previous != null) {
                        errors.put(RoutineFormState.timeKey(day, previous), Error.DUPLICATE_TIME);
                        errors.put(key, Error.DUPLICATE_TIME);
                    }
                }
            }
        }
        if (!selected) errors.put(RoutineFormState.DAYS, Error.REQUIRED);
        else if (complete && frequency != null && frequency > 0 && sum != frequency) errors.put(RoutineFormState.DISTRIBUTION, Error.FREQUENCY_MISMATCH);
        return Collections.unmodifiableMap(errors);
    }

    public static Integer number(String raw) {
        String value = raw.trim();
        if (!value.matches("-?[0-9]+")) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static Integer count(String raw, String key, Map<String, Error> errors) {
        if (raw.trim().isEmpty()) {
            errors.put(key, Error.REQUIRED);
            return null;
        }
        Integer value = number(raw);
        if (value == null) errors.put(key, Error.INVALID_NUMBER);
        else if (value < 1) errors.put(key, Error.POSITIVE);
        return value;
    }
}
