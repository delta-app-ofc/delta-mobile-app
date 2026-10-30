package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RoutineData {
    public static final class Habit {
        public static final class ScheduleVersion {
            public final LocalDate effectiveFrom, effectiveUntil;
            public final Integer frequency;
            public final Map<DayOfWeek, List<LocalTime>> schedule;

            private ScheduleVersion(LocalDate effectiveFrom, LocalDate effectiveUntil, Integer frequency, Map<DayOfWeek, List<LocalTime>> schedule) {
                this.effectiveFrom = effectiveFrom;
                this.effectiveUntil = effectiveUntil;
                this.frequency = frequency;
                this.schedule = Collections.unmodifiableMap(schedule);
            }

            private boolean includes(LocalDate date) {
                return !date.isBefore(effectiveFrom) && (effectiveUntil == null || !date.isAfter(effectiveUntil));
            }
        }

        public final int id, userId, habitId;
        public final String name, category, description;
        public final Integer frequency;
        public final Map<DayOfWeek, List<LocalTime>> schedule;
        public final boolean scheduleComplete;
        public final BigDecimal litersPerExecution;
        public final List<ScheduleVersion> scheduleVersions;
        private final boolean versioned, versionsComplete;

        private Habit(ApiModels.UserHabitResponse value) {
            if (value == null || value.id == null || value.userId == null || value.habitId == null)
                throw new IllegalArgumentException("Habit identity is required");
            id = value.id;
            userId = value.userId;
            habitId = value.habitId;
            name = value.habitName;
            category = value.habitCategory;
            description = value.habitDescription;
            List<ScheduleVersion> parsedVersions = parseVersions(value.scheduleVersions);
            versioned = value.scheduleVersions != null;
            versionsComplete = !versioned || parsedVersions != null;
            scheduleVersions = parsedVersions == null ? Collections.emptyList() : Collections.unmodifiableList(parsedVersions);
            ScheduleVersion latest = scheduleVersions.isEmpty() ? null : scheduleVersions.get(scheduleVersions.size() - 1);
            Map<DayOfWeek, List<LocalTime>> parsed = versioned ? latest == null ? null : latest.schedule : parseSchedule(value.frequency, value.daysOfWeek, value.schedule);
            frequency = versioned && latest != null ? latest.frequency : value.frequency;
            scheduleComplete = parsed != null && versionsComplete;
            schedule = parsed == null ? Collections.emptyMap() : parsed;
            litersPerExecution = estimate(habitId);
        }

        public boolean activeAt(LocalDate date) {
            return !versioned || !versionsComplete || versionAt(date) != null;
        }

        public boolean scheduleCompleteAt(LocalDate date) {
            return versioned ? versionsComplete && (versionAt(date) == null || !versionAt(date).schedule.isEmpty()) : scheduleComplete;
        }

        public Integer frequencyAt(LocalDate date) {
            ScheduleVersion version = versionAt(date);
            return versioned ? version == null ? null : version.frequency : frequency;
        }

        public Map<DayOfWeek, List<LocalTime>> scheduleAt(LocalDate date) {
            ScheduleVersion version = versionAt(date);
            return versioned ? version == null ? Collections.emptyMap() : version.schedule : schedule;
        }

        private ScheduleVersion versionAt(LocalDate date) {
            for (ScheduleVersion version : scheduleVersions) if (version.includes(date)) return version;
            return null;
        }
    }

    public static final class Execution {
        public final Habit habit;
        public final LocalTime startTime;
        public final BigDecimal liters;

        private Execution(Habit habit, LocalTime startTime) {
            this.habit = habit;
            this.startTime = startTime;
            liters = habit.litersPerExecution;
        }
    }

    public static final class HabitTotal {
        public final Habit habit;
        public final int executionCount;
        public final BigDecimal liters;

        private HabitTotal(Habit habit, int executionCount) {
            this.habit = habit;
            this.executionCount = executionCount;
            liters = habit.litersPerExecution == null ? null : habit.litersPerExecution.multiply(BigDecimal.valueOf(executionCount));
        }
    }

    public static final class Day {
        public final LocalDate date;
        public final List<Execution> executions;
        public final List<HabitTotal> summaries;
        public final List<Habit> incompleteHabits;
        public final boolean agendaComplete, totalComplete;
        public final BigDecimal knownLiters, totalLiters;

        private Day(LocalDate date, List<Execution> executions, List<HabitTotal> summaries, List<Habit> incompleteHabits) {
            this.date = date;
            this.executions = Collections.unmodifiableList(executions);
            this.summaries = Collections.unmodifiableList(summaries);
            this.incompleteHabits = Collections.unmodifiableList(incompleteHabits);
            agendaComplete = incompleteHabits.isEmpty();
            BigDecimal known = BigDecimal.ZERO;
            boolean complete = agendaComplete;
            for (HabitTotal summary : summaries) {
                if (summary.liters == null) complete = false;
                else known = known.add(summary.liters);
            }
            knownLiters = known;
            totalComplete = complete;
            totalLiters = complete ? known : null;
        }

        public boolean confirmedEmpty() {
            return agendaComplete && executions.isEmpty();
        }
    }

    public final LocalDate reference;
    public final List<Habit> habits;

    private RoutineData(LocalDate reference, List<Habit> habits) {
        this.reference = reference;
        this.habits = Collections.unmodifiableList(habits);
    }

    public static RoutineData from(List<ApiModels.UserHabitResponse> values, Clock clock) {
        if (values == null) throw new IllegalArgumentException("Habit collection is required");
        LocalDate reference = LocalDate.now(Objects.requireNonNull(clock));
        List<Habit> habits = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (ApiModels.UserHabitResponse value : values) {
            Habit habit = new Habit(value);
            if (!ids.add(habit.id)) throw new IllegalArgumentException("Duplicate habit record");
            habits.add(habit);
        }
        return new RoutineData(reference, habits);
    }

    public Day day(LocalDate date) {
        Objects.requireNonNull(date);
        DayOfWeek weekday = date.getDayOfWeek();
        List<Execution> executions = new ArrayList<>();
        List<HabitTotal> summaries = new ArrayList<>();
        List<Habit> incomplete = new ArrayList<>();
        for (Habit habit : habits) {
            if (!habit.activeAt(date)) continue;
            if (!habit.scheduleCompleteAt(date)) {
                incomplete.add(habit);
                continue;
            }
            List<LocalTime> times = habit.scheduleAt(date).get(weekday);
            if (times == null) continue;
            summaries.add(new HabitTotal(habit, times.size()));
            for (LocalTime time : times) executions.add(new Execution(habit, time));
        }
        executions.sort(Comparator.comparing((Execution value) -> value.startTime).thenComparingInt(value -> value.habit.id));
        summaries.sort(Comparator.comparing((HabitTotal value) -> value.habit.scheduleAt(date).get(weekday).get(0)).thenComparingInt(value -> value.habit.id));
        return new Day(date, executions, summaries, incomplete);
    }

    private static Map<DayOfWeek, List<LocalTime>> parseSchedule(Integer frequency, List<Integer> daysOfWeek, List<ApiModels.UserHabitScheduleResponse> entries) {
        if (frequency == null || daysOfWeek == null || daysOfWeek.isEmpty() || entries == null || entries.isEmpty()) return null;
        Set<Integer> days = new HashSet<>();
        for (Integer day : daysOfWeek)
            if (day == null || day < 1 || day > 7 || !days.add(day)) return null;
        Map<DayOfWeek, List<LocalTime>> schedule = new EnumMap<>(DayOfWeek.class);
        long count = 0;
        for (ApiModels.UserHabitScheduleResponse entry : entries) {
            if (entry == null || entry.dayOfWeek == null || !days.contains(entry.dayOfWeek) || entry.startTimes == null || entry.startTimes.isEmpty()) return null;
            DayOfWeek weekday = DayOfWeek.of(entry.dayOfWeek);
            if (schedule.containsKey(weekday)) return null;
            Set<LocalTime> uniqueTimes = new HashSet<>();
            List<LocalTime> times = new ArrayList<>();
            for (String time : entry.startTimes) {
                if (time == null || !time.matches("[0-2][0-9]:[0-5][0-9]")) return null;
                LocalTime parsed;
                try {
                    parsed = LocalTime.parse(time);
                } catch (DateTimeException invalid) {
                    return null;
                }
                if (!uniqueTimes.add(parsed)) return null;
                times.add(parsed);
            }
            Collections.sort(times);
            schedule.put(weekday, Collections.unmodifiableList(times));
            count += times.size();
        }
        return schedule.size() == days.size() && count == frequency ? Collections.unmodifiableMap(schedule) : null;
    }

    private static List<Habit.ScheduleVersion> parseVersions(List<ApiModels.UserHabitScheduleVersionResponse> values) {
        if (values == null) return Collections.emptyList();
        if (values.isEmpty()) return null;
        List<Habit.ScheduleVersion> versions = new ArrayList<>();
        for (ApiModels.UserHabitScheduleVersionResponse value : values) {
            if (value == null || value.effectiveFrom == null) return null;
            LocalDate from, until = null;
            try {
                from = LocalDate.parse(value.effectiveFrom);
                if (value.effectiveUntil != null) until = LocalDate.parse(value.effectiveUntil);
            } catch (DateTimeException invalid) {
                return null;
            }
            if (until != null && until.isBefore(from)) return null;
            Map<DayOfWeek, List<LocalTime>> schedule = parseSchedule(value.frequency, value.daysOfWeek, value.schedule);
            if (schedule == null) return null;
            versions.add(new Habit.ScheduleVersion(from, until, value.frequency, schedule));
        }
        versions.sort(Comparator.comparing(value -> value.effectiveFrom));
        for (int i = 0; i < versions.size(); i++) {
            Habit.ScheduleVersion current = versions.get(i);
            if (i == versions.size() - 1) {
                if (current.effectiveUntil != null) return null;
            } else {
                Habit.ScheduleVersion next = versions.get(i + 1);
                if (current.effectiveUntil == null || !current.effectiveUntil.plusDays(1).equals(next.effectiveFrom)) return null;
            }
        }
        return versions;
    }

    private static BigDecimal estimate(int habitId) {
        switch (habitId) {
            case 1: return BigDecimal.valueOf(80);
            case 2: return BigDecimal.valueOf(150);
            case 3: return BigDecimal.valueOf(100);
            case 4: return BigDecimal.valueOf(15);
            case 5: return BigDecimal.valueOf(200);
            case 6: return BigDecimal.valueOf(20);
            default: return null;
        }
    }
}
