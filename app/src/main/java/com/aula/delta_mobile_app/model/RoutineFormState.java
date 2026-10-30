package com.aula.delta_mobile_app.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RoutineFormState {
    public enum Mode { CREATE, EDIT }

    public static final String TYPE = "type", FREQUENCY = "frequency", DAYS = "days", DISTRIBUTION = "distribution";

    public static final class DayDraft {
        public final boolean selected;
        public final String quantity;
        public final List<String> times;

        private DayDraft(boolean selected, String quantity, List<String> times) {
            this.selected = selected;
            this.quantity = quantity;
            this.times = Collections.unmodifiableList(new ArrayList<>(times));
        }

        public int activeCount() {
            Integer count = RoutineFormValidator.number(quantity);
            return selected && count != null && count > 0 && count <= RoutineFormValidator.MINUTES_PER_DAY ? count : 0;
        }
    }

    public final long token;
    public final Mode mode;
    public final Integer id, userId, habitId;
    public final LocalDate effectiveFrom;
    public final String frequency;
    public final Map<DayOfWeek, DayDraft> days;
    public final Map<String, RoutineFormValidator.Error> errors;
    public final boolean attempted, dirty, confirmDiscard, legacy;

    private RoutineFormState(long token, Mode mode, Integer id, Integer userId, Integer habitId, LocalDate effectiveFrom, String frequency, Map<DayOfWeek, DayDraft> days,
        boolean attempted, boolean dirty, boolean confirmDiscard, boolean legacy) {
        this.token = token;
        this.mode = mode;
        this.id = id;
        this.userId = userId;
        this.habitId = habitId;
        this.effectiveFrom = effectiveFrom;
        this.frequency = Objects.requireNonNull(frequency);
        this.days = Collections.unmodifiableMap(new EnumMap<>(days));
        this.attempted = attempted;
        this.dirty = dirty;
        this.confirmDiscard = confirmDiscard;
        this.legacy = legacy;
        errors = attempted ? RoutineFormValidator.validate(this) : Collections.emptyMap();
    }

    public static RoutineFormState create(long token) {
        return new RoutineFormState(token, Mode.CREATE, null, null, null, null, "", emptyDays(), false, false, false, false);
    }

    public static RoutineFormState edit(long token, RoutineData.Habit habit) {
        return edit(token, habit, null);
    }

    public static RoutineFormState edit(long token, RoutineData.Habit habit, LocalDate effectiveFrom) {
        Map<DayOfWeek, DayDraft> days = emptyDays();
        Map<DayOfWeek, List<LocalTime>> selected = effectiveFrom == null ? habit.schedule : habit.scheduleAt(effectiveFrom);
        for (Map.Entry<DayOfWeek, List<LocalTime>> entry : selected.entrySet()) {
            List<String> times = new ArrayList<>();
            for (LocalTime time : entry.getValue()) times.add(time.format(DateTimeFormatter.ofPattern("HH:mm")));
            days.put(entry.getKey(), new DayDraft(true, String.valueOf(times.size()), times));
        }
        Integer frequency = effectiveFrom == null ? habit.frequency : habit.frequencyAt(effectiveFrom);
        boolean complete = effectiveFrom == null ? habit.scheduleComplete : habit.scheduleCompleteAt(effectiveFrom);
        return new RoutineFormState(token, Mode.EDIT, habit.id, habit.userId, habit.habitId, effectiveFrom, frequency == null ? "" : frequency.toString(), days,
            false, false, false, !complete);
    }

    public RoutineFormState withHabit(Integer value) {
        return Objects.equals(habitId, value) ? this : copy(value, frequency, days, attempted, true, false);
    }

    public RoutineFormState withFrequency(String value) {
        Objects.requireNonNull(value);
        return frequency.equals(value) ? this : copy(habitId, value, days, attempted, true, false);
    }

    public RoutineFormState selectDay(DayOfWeek day, boolean selected) {
        DayDraft previous = days.get(Objects.requireNonNull(day));
        if (previous.selected == selected) return this;
        return changeDay(day, new DayDraft(selected, previous.quantity, previous.times));
    }

    public RoutineFormState withQuantity(DayOfWeek day, String value) {
        Objects.requireNonNull(value);
        DayDraft previous = days.get(Objects.requireNonNull(day));
        if (previous.quantity.equals(value)) return this;
        List<String> times = new ArrayList<>(previous.times);
        Integer count = RoutineFormValidator.number(value);
        if (count != null && count > 0 && count <= RoutineFormValidator.MINUTES_PER_DAY)
            while (times.size() < count) times.add("");
        return changeDay(day, new DayDraft(previous.selected, value, times));
    }

    public RoutineFormState withTime(DayOfWeek day, int index, String value) {
        Objects.requireNonNull(value);
        DayDraft previous = days.get(Objects.requireNonNull(day));
        if (index < 0 || index >= previous.activeCount()) return this;
        if (previous.times.get(index).equals(value)) return this;
        List<String> times = new ArrayList<>(previous.times);
        times.set(index, value);
        return changeDay(day, new DayDraft(previous.selected, previous.quantity, times));
    }

    public RoutineFormState validate() {
        return attempted ? this : copy(habitId, frequency, days, true, dirty, confirmDiscard);
    }

    public RoutineFormState confirmDiscard(boolean value) {
        return value == confirmDiscard ? this : copy(habitId, frequency, days, attempted, dirty, value);
    }

    public boolean valid() {
        return attempted && errors.isEmpty();
    }

    public static String quantityKey(DayOfWeek day) {
        return "quantity:" + day.getValue();
    }

    public static String timeKey(DayOfWeek day, int index) {
        return "time:" + day.getValue() + ":" + index;
    }

    private RoutineFormState changeDay(DayOfWeek day, DayDraft value) {
        Map<DayOfWeek, DayDraft> next = new EnumMap<>(days);
        next.put(day, value);
        return copy(habitId, frequency, next, attempted, true, false);
    }

    private RoutineFormState copy(Integer habit, String frequency, Map<DayOfWeek, DayDraft> days, boolean attempted, boolean dirty, boolean confirm) {
        return new RoutineFormState(token, mode, id, userId, habit, effectiveFrom, frequency, days, attempted, dirty, confirm, legacy);
    }

    private static Map<DayOfWeek, DayDraft> emptyDays() {
        Map<DayOfWeek, DayDraft> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) days.put(day, new DayDraft(false, "", Collections.emptyList()));
        return days;
    }
}
