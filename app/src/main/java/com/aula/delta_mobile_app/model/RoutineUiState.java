package com.aula.delta_mobile_app.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public final class RoutineUiState {
    public final LocalDate selectedDate;
    public final YearMonth displayedMonth;
    public final ScreenState<RoutineData> reading;
    public final RoutineData data;
    public final RoutineData.Day day;
    public final boolean stale;

    public RoutineUiState(LocalDate selectedDate, ScreenState<RoutineData> reading, RoutineData previous) {
        this.selectedDate = Objects.requireNonNull(selectedDate);
        displayedMonth = YearMonth.from(selectedDate);
        this.reading = Objects.requireNonNull(reading);
        data = reading.status == ScreenState.Status.SUCCESS ? Objects.requireNonNull(reading.data) : previous;
        day = data == null ? null : data.day(selectedDate);
        stale = data != null && reading.status != ScreenState.Status.SUCCESS;
    }
}
