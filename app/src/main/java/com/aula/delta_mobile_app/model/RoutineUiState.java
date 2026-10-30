package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RoutineUiState {
    public static final class DetailTarget {
        public final int id, userId;
        public final LocalDate date;
        public final LocalTime startTime;

        public DetailTarget(int id, int userId, LocalDate date, LocalTime startTime) {
            this.id = id;
            this.userId = userId;
            this.date = Objects.requireNonNull(date);
            this.startTime = Objects.requireNonNull(startTime);
        }
    }

    public static final class HabitDetails {
        public final RoutineData.Habit habit;
        public final List<LocalTime> times;
        public final Integer executionCount;
        public final BigDecimal executionLiters, dailyLiters;
        public final boolean scheduleComplete, selectedExecutionPresent;

        private HabitDetails(DetailTarget target, RoutineData.Habit habit) {
            this.habit = habit;
            scheduleComplete = habit.scheduleComplete;
            times = habit.schedule.getOrDefault(target.date.getDayOfWeek(), Collections.emptyList());
            executionCount = scheduleComplete ? times.size() : null;
            selectedExecutionPresent = scheduleComplete && times.contains(target.startTime);
            executionLiters = habit.litersPerExecution;
            dailyLiters = !scheduleComplete ? null : times.isEmpty() ? BigDecimal.ZERO : executionLiters == null ? null
                : executionLiters.multiply(BigDecimal.valueOf(times.size()));
        }
    }

    public final LocalDate selectedDate;
    public final YearMonth displayedMonth;
    public final ScreenState<RoutineData> reading;
    public final RoutineData data;
    public final RoutineData.Day day;
    public final boolean stale;
    public final DetailTarget detailTarget;
    public final ScreenState<RoutineData.Habit> detail;
    public final HabitDetails details;
    public final RoutineFormState form;
    public final ScreenState<RoutineData.Habit> creation;
    public final boolean uncertainCreation, checkedCreation, confirmResend, savedAwaitingReload;

    public RoutineUiState(LocalDate selectedDate, ScreenState<RoutineData> reading, RoutineData previous) {
        this(selectedDate, reading, previous, null, ScreenState.idle());
    }

    public RoutineUiState(LocalDate selectedDate, ScreenState<RoutineData> reading, RoutineData previous, DetailTarget target, ScreenState<RoutineData.Habit> detail) {
        this(selectedDate, reading, previous, target, detail, null);
    }

    public RoutineUiState(LocalDate selectedDate, ScreenState<RoutineData> reading, RoutineData previous, DetailTarget target, ScreenState<RoutineData.Habit> detail, RoutineFormState form) {
        this(selectedDate, reading, previous, target, detail, form, ScreenState.idle(), false, false, false, false);
    }

    public RoutineUiState(LocalDate selectedDate, ScreenState<RoutineData> reading, RoutineData previous, DetailTarget target, ScreenState<RoutineData.Habit> detail,
        RoutineFormState form, ScreenState<RoutineData.Habit> creation, boolean uncertainCreation, boolean checkedCreation, boolean confirmResend, boolean savedAwaitingReload) {
        this.creation = Objects.requireNonNull(creation);
        this.uncertainCreation = uncertainCreation;
        this.checkedCreation = checkedCreation;
        this.confirmResend = confirmResend;
        this.savedAwaitingReload = savedAwaitingReload;
        this.selectedDate = Objects.requireNonNull(selectedDate);
        displayedMonth = YearMonth.from(selectedDate);
        this.reading = Objects.requireNonNull(reading);
        data = reading.status == ScreenState.Status.SUCCESS ? Objects.requireNonNull(reading.data) : previous;
        day = data == null ? null : data.day(selectedDate);
        stale = data != null && reading.status != ScreenState.Status.SUCCESS;
        detailTarget = target;
        this.detail = Objects.requireNonNull(detail);
        details = target != null && detail.status == ScreenState.Status.SUCCESS && detail.data != null ? new HabitDetails(target, detail.data) : null;
        this.form = form;
    }
}
