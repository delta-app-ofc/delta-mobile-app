package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.CreateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UpdateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleVersionResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RoutineDataTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
    private static final String EXPANDED = "{\"id\":105,\"userId\":42,\"habitId\":1,\"habitName\":\"Banho longo\",\"frequency\":5,"
        + "\"daysOfWeek\":[1,3],\"schedule\":[{\"dayOfWeek\":1,\"startTimes\":[\"07:00\",\"20:00\"]},"
        + "{\"dayOfWeek\":3,\"startTimes\":[\"08:00\",\"14:00\",\"21:00\"]}]}";
    private final Gson gson = new Gson();

    @Test
    public void expandedCollectionAndDetailPreserveDistributionAndIndividualTimes() {
        UserHabitResponse[] collection = gson.fromJson("[" + EXPANDED + "]", UserHabitResponse[].class);
        UserHabitResponse detail = gson.fromJson(EXPANDED, UserHabitResponse.class);
        assertEquals(Integer.valueOf(105), detail.id);
        assertEquals(Integer.valueOf(42), detail.userId);
        assertEquals(Integer.valueOf(1), detail.habitId);
        assertEquals("Banho longo", detail.habitName);
        assertEquals(Integer.valueOf(5), detail.frequency);
        assertEquals(Arrays.asList(1, 3), detail.daysOfWeek);
        assertEquals(2, detail.schedule.size());
        assertEquals(Integer.valueOf(1), detail.schedule.get(0).dayOfWeek);
        assertEquals(Arrays.asList("07:00", "20:00"), detail.schedule.get(0).startTimes);
        assertEquals(Integer.valueOf(3), detail.schedule.get(1).dayOfWeek);
        assertEquals(Arrays.asList("08:00", "14:00", "21:00"), detail.schedule.get(1).startTimes);
        assertEquals(gson.toJson(detail), gson.toJson(collection[0]));
    }

    @Test
    public void responseAlsoPreservesPublishedMetadataAndLegacyValues() {
        UserHabitResponse value = gson.fromJson("{\"habitCategory\":\"Higiene\",\"habitDescription\":\"Referencia\","
            + "\"startTime\":\"07:00:00\",\"endTime\":\"07:20:00\",\"estimatedLiters\":12.345}", UserHabitResponse.class);
        assertEquals("Higiene", value.habitCategory);
        assertEquals("Referencia", value.habitDescription);
        assertEquals("07:00:00", value.startTime);
        assertEquals("07:20:00", value.endTime);
        assertEquals(new BigDecimal("12.345"), value.estimatedLiters);
        assertNull(value.schedule);
    }

    @Test
    public void missingOrNullTransportFieldsStayAbsentRatherThanBecomingDefaults() {
        UserHabitResponse legacy = gson.fromJson("{\"id\":105,\"userId\":42,\"habitId\":1,\"frequency\":5,\"daysOfWeek\":[1,3]}", UserHabitResponse.class);
        UserHabitResponse absent = gson.fromJson("{\"schedule\":null,\"frequency\":null,\"estimatedLiters\":null}", UserHabitResponse.class);
        assertNull(legacy.schedule);
        assertNull(legacy.startTime);
        assertNull(legacy.estimatedLiters);
        assertEquals(Integer.valueOf(5), legacy.frequency);
        assertEquals(Arrays.asList(1, 3), legacy.daysOfWeek);
        assertNull(absent.schedule);
        assertNull(absent.frequency);
        assertNull(absent.estimatedLiters);
        UserHabitResponse partial = gson.fromJson("{\"schedule\":[{\"dayOfWeek\":1}]}", UserHabitResponse.class);
        assertNull(partial.schedule.get(0).startTimes);
    }

    @Test
    public void readingFieldsDoNotChangeExistingWritePayloads() {
        CreateUserHabitRequest create = new CreateUserHabitRequest();
        create.habitId = 1;
        create.frequency = 5;
        create.daysOfWeek = Arrays.asList(1, 3);
        UpdateUserHabitRequest update = new UpdateUserHabitRequest();
        update.habitId = 1;
        update.frequency = 5;
        update.daysOfWeek = Arrays.asList(1, 3);
        String expected = "{\"habitId\":1,\"frequency\":5,\"daysOfWeek\":[1,3]}";
        assertEquals(gson.fromJson(expected, Object.class), gson.fromJson(gson.toJson(create), Object.class));
        assertEquals(gson.fromJson(expected, Object.class), gson.fromJson(gson.toJson(update), Object.class));
    }

    @Test
    public void todayUsesInjectedClockZoneWithoutConvertingRoutineTimes() {
        UserHabitResponse habit = singleDay(105, 1, 3, "07:00");
        RoutineData local = data(habit);
        RoutineData utc = RoutineData.from(Collections.singletonList(habit), CLOCK.withZone(ZoneOffset.UTC));
        assertEquals(LocalDate.of(2026, 10, 28), local.reference);
        assertEquals(LocalDate.of(2026, 10, 29), utc.reference);
        assertEquals(LocalTime.of(7, 0), local.day(MONDAY.plusDays(2)).executions.get(0).startTime);
        assertEquals(LocalTime.of(7, 0), utc.day(MONDAY.plusDays(2)).executions.get(0).startTime);
    }

    @Test
    public void multipleExecutionsAreGroupedAndSummedOnlyOncePerHabit() {
        UserHabitResponse shower = gson.fromJson(EXPANDED, UserHabitResponse.class);
        RoutineData routine = data(singleDay(106, 4, 1, "07:00"), shower);
        RoutineData.Day monday = routine.day(MONDAY);
        assertTrue(monday.agendaComplete);
        assertTrue(monday.totalComplete);
        assertFalse(monday.confirmedEmpty());
        assertEquals(3, monday.executions.size());
        assertEquals(105, monday.executions.get(0).habit.id);
        assertEquals(106, monday.executions.get(1).habit.id);
        assertEquals(LocalTime.of(20, 0), monday.executions.get(2).startTime);
        assertEquals(new BigDecimal("80"), monday.executions.get(0).liters);
        assertEquals(new BigDecimal("15"), monday.executions.get(1).liters);
        assertEquals(2, monday.summaries.size());
        assertEquals(2, monday.summaries.get(0).executionCount);
        assertEquals(new BigDecimal("160"), monday.summaries.get(0).liters);
        assertEquals(new BigDecimal("175"), monday.totalLiters);
        assertEquals(monday.totalLiters, monday.knownLiters);
        assertEquals(new BigDecimal("240"), routine.day(MONDAY.plusDays(2)).totalLiters);
    }

    @Test
    public void allSixCatalogTypesUseApprovedLitersPerExecution() {
        int[] expected = {80, 150, 100, 15, 200, 20};
        List<UserHabitResponse> values = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) values.add(singleDay(100 + i, i + 1, 1, "07:00"));
        RoutineData.Day day = RoutineData.from(values, CLOCK).day(MONDAY);
        for (int i = 0; i < expected.length; i++) assertEquals(BigDecimal.valueOf(expected[i]), day.executions.get(i).liters);
        assertEquals(new BigDecimal("565"), day.totalLiters);
    }

    @Test
    public void emptyCollectionAndKnownDayWithoutExecutionsConfirmZero() {
        RoutineData.Day empty = data().day(MONDAY);
        RoutineData.Day unscheduled = data(singleDay(105, 1, 1, "07:00")).day(MONDAY.plusDays(1));
        for (RoutineData.Day day : Arrays.asList(empty, unscheduled)) {
            assertTrue(day.confirmedEmpty());
            assertTrue(day.agendaComplete);
            assertTrue(day.totalComplete);
            assertEquals(BigDecimal.ZERO, day.totalLiters);
            assertTrue(day.executions.isEmpty());
            assertTrue(day.summaries.isEmpty());
        }
    }

    @Test
    public void unknownTypeHasUnavailableEstimateWithoutGuessingByNameOrLegacyLiters() {
        UserHabitResponse unknown = singleDay(107, 99, 1, "08:00");
        unknown.habitName = "Banho longo";
        unknown.estimatedLiters = new BigDecimal("999");
        RoutineData routine = data(singleDay(105, 1, 1, "07:00"), unknown);
        RoutineData.Day day = routine.day(MONDAY);
        assertTrue(day.agendaComplete);
        assertFalse(day.totalComplete);
        assertNull(day.totalLiters);
        assertNull(day.executions.get(1).liters);
        assertNull(day.summaries.get(1).liters);
        assertEquals(new BigDecimal("80"), day.knownLiters);
        assertTrue(routine.day(MONDAY.plusDays(1)).confirmedEmpty());
        assertEquals(BigDecimal.ZERO, routine.day(MONDAY.plusDays(1)).totalLiters);
    }

    @Test
    public void legacyScheduleIsNotReconstructedFromFrequencyDaysOrSingleTime() {
        UserHabitResponse legacy = singleDay(108, 1, 1, "07:00");
        legacy.frequency = 5;
        legacy.daysOfWeek = Arrays.asList(1, 3);
        legacy.schedule = null;
        legacy.startTime = "07:00";
        legacy.endTime = "08:00";
        legacy.estimatedLiters = new BigDecimal("1000");
        RoutineData routine = data(singleDay(105, 4, 1, "09:00"), legacy);
        RoutineData.Day monday = routine.day(MONDAY);
        assertFalse(monday.agendaComplete);
        assertFalse(monday.totalComplete);
        assertNull(monday.totalLiters);
        assertEquals(new BigDecimal("15"), monday.knownLiters);
        assertEquals(1, monday.executions.size());
        assertEquals(108, monday.incompleteHabits.get(0).id);
        assertFalse(monday.incompleteHabits.get(0).scheduleComplete);
        assertTrue(monday.incompleteHabits.get(0).schedule.isEmpty());
        RoutineData.Day tuesday = routine.day(MONDAY.plusDays(1));
        assertTrue(tuesday.executions.isEmpty());
        assertFalse(tuesday.confirmedEmpty());
        assertFalse(tuesday.totalComplete);
        assertNull(tuesday.totalLiters);
    }

    @Test
    public void inconsistentDistributionsAreKeptIncompleteWithoutPartialProjection() {
        List<Consumer<UserHabitResponse>> invalid = Arrays.asList(
            value -> value.frequency = null,
            value -> value.frequency = 0,
            value -> value.frequency = -1,
            value -> value.frequency = 5,
            value -> value.daysOfWeek = null,
            value -> value.daysOfWeek = Collections.emptyList(),
            value -> value.daysOfWeek = Arrays.asList(1, 1),
            value -> value.daysOfWeek = Arrays.asList(1, null),
            value -> value.daysOfWeek = Arrays.asList(0, 1),
            value -> value.daysOfWeek = Arrays.asList(1, 8),
            value -> value.daysOfWeek = Arrays.asList(1, 3),
            value -> value.daysOfWeek = Collections.singletonList(3),
            value -> value.schedule = null,
            value -> value.schedule = Collections.emptyList(),
            value -> value.schedule.add(null),
            value -> value.schedule.get(0).dayOfWeek = null,
            value -> value.schedule.get(0).dayOfWeek = 8,
            value -> value.schedule.add(schedule(1, "08:00")),
            value -> value.schedule.get(0).startTimes = null,
            value -> value.schedule.get(0).startTimes = Collections.emptyList(),
            value -> value.schedule.get(0).startTimes = Arrays.asList("07:00", "07:00"));
        for (int i = 0; i < invalid.size(); i++) {
            UserHabitResponse habit = singleDay(105, 1, 1, "07:00");
            invalid.get(i).accept(habit);
            RoutineData.Day day = data(habit).day(MONDAY);
            assertFalse("case " + i, day.agendaComplete);
            assertNull("case " + i, day.totalLiters);
            assertEquals("case " + i, 1, day.incompleteHabits.size());
            assertTrue("case " + i, day.executions.isEmpty());
        }
    }

    @Test
    public void malformedTimesNeverBecomeValidOrInventedExecutions() {
        for (String time : Arrays.asList(null, "", "7:00", "07:0", "07:00:00", "24:00", "29:00", "12:60", "07:00Z", "07:00-03:00", " 07:00", "07:00 ")) {
            RoutineData.Day day = data(singleDay(105, 1, 1, "06:00", time)).day(MONDAY);
            assertFalse(String.valueOf(time), day.agendaComplete);
            assertTrue(String.valueOf(time), day.executions.isEmpty());
            assertNull(String.valueOf(time), day.totalLiters);
        }
    }

    @Test
    public void midnightAndLastMinuteAreValidAndSortedWithinTheSameDay() {
        RoutineData.Day day = data(singleDay(105, 1, 1, "23:59", "00:00")).day(MONDAY);
        assertTrue(day.agendaComplete);
        assertEquals(LocalTime.MIDNIGHT, day.executions.get(0).startTime);
        assertEquals(LocalTime.of(23, 59), day.executions.get(1).startTime);
        assertEquals(new BigDecimal("160"), day.totalLiters);
    }

    @Test
    public void weekdayProjectionCrossesWeekMonthAndYearBoundaries() {
        UserHabitResponse habit = singleDay(105, 1, 1, "07:00");
        habit.frequency = 7;
        habit.daysOfWeek = Arrays.asList(7, 6, 5, 4, 3, 2, 1);
        habit.schedule.clear();
        for (int i = 7; i >= 1; i--) habit.schedule.add(schedule(i, "0" + i + ":00"));
        RoutineData routine = data(habit);
        for (LocalDate start : Arrays.asList(LocalDate.of(2026, 12, 28), LocalDate.of(2026, 11, 30))) {
            for (int i = 0; i < 14; i++) {
                LocalDate date = start.plusDays(i);
                RoutineData.Day day = routine.day(date);
                assertEquals(date, day.date);
                assertEquals(LocalTime.of(date.getDayOfWeek().getValue(), 0), day.executions.get(0).startTime);
                assertEquals(new BigDecimal("80"), day.totalLiters);
            }
        }
    }

    @Test
    public void leapDayAndPastOrFutureWeeksUseTheSameWeeklyTemplate() {
        RoutineData routine = data(singleDay(105, 1, 4, "07:00", "20:00"));
        for (LocalDate date : Arrays.asList(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 7), LocalDate.of(2032, 2, 26))) {
            assertEquals(DayOfWeek.THURSDAY, date.getDayOfWeek());
            assertEquals(2, routine.day(date).executions.size());
            assertEquals(new BigDecimal("160"), routine.day(date).totalLiters);
        }
        assertTrue(routine.day(LocalDate.of(2024, 3, 1)).confirmedEmpty());
    }

    @Test
    public void scheduleVersionsSelectTheInclusiveRangeWithoutRewritingPastDates() {
        UserHabitResponse habit = singleDay(105, 1, 1, "23:00");
        habit.frequency = null;
        habit.daysOfWeek = null;
        habit.schedule = null;
        habit.scheduleVersions = Arrays.asList(version("2026-01-01", "2026-10-28", 1, "07:00"),
            version("2026-10-29", null, 4, "20:00", "21:00"));
        RoutineData routine = data(habit);
        assertEquals(LocalTime.of(7, 0), routine.day(LocalDate.of(2026, 10, 26)).executions.get(0).startTime);
        assertTrue(routine.day(LocalDate.of(2026, 10, 27)).confirmedEmpty());
        assertEquals(Arrays.asList(LocalTime.of(20, 0), LocalTime.of(21, 0)), Arrays.asList(
            routine.day(LocalDate.of(2026, 10, 29)).executions.get(0).startTime,
            routine.day(LocalDate.of(2026, 10, 29)).executions.get(1).startTime));
        assertTrue(routine.day(LocalDate.of(2026, 11, 2)).confirmedEmpty());
        assertTrue(routine.day(LocalDate.of(2025, 12, 29)).confirmedEmpty());
        assertEquals(Integer.valueOf(1), routine.habits.get(0).frequencyAt(LocalDate.of(2026, 10, 26)));
        assertEquals(Integer.valueOf(1), routine.habits.get(0).frequencyAt(LocalDate.of(2026, 10, 28)));
        assertEquals(Integer.valueOf(2), routine.habits.get(0).frequencyAt(LocalDate.of(2026, 10, 29)));
    }

    @Test
    public void editorSeedsTheVersionActiveOnTheSelectedDate() {
        UserHabitResponse habit = singleDay(105, 1, 1, "23:00");
        habit.scheduleVersions = Arrays.asList(version("2026-01-01", "2026-10-28", 1, "07:00"),
            version("2026-10-29", null, 4, "20:00", "21:00"));
        RoutineData.Habit parsed = data(habit).habits.get(0);
        RoutineFormState old = RoutineFormState.edit(1, parsed, LocalDate.of(2026, 10, 26));
        RoutineFormState current = RoutineFormState.edit(2, parsed, LocalDate.of(2026, 10, 29));
        assertEquals("07:00", old.days.get(DayOfWeek.MONDAY).times.get(0));
        assertFalse(old.days.get(DayOfWeek.THURSDAY).selected);
        assertEquals("20:00", current.days.get(DayOfWeek.THURSDAY).times.get(0));
        assertEquals("2", current.frequency);
        assertFalse(current.days.get(DayOfWeek.MONDAY).selected);
        assertEquals(LocalDate.of(2026, 10, 29), current.effectiveFrom);
    }

    @Test
    public void malformedOverlappingOrGappedVersionHistoryStaysIncomplete() {
        List<Consumer<List<UserHabitScheduleVersionResponse>>> invalid = Arrays.asList(
            versions -> versions.clear(),
            versions -> versions.get(0).effectiveFrom = null,
            versions -> versions.get(0).effectiveFrom = "29/10/2026",
            versions -> versions.get(0).effectiveUntil = null,
            versions -> versions.get(0).effectiveUntil = "2025-12-31",
            versions -> versions.get(0).effectiveUntil = "2026-10-27",
            versions -> versions.get(1).effectiveFrom = "2026-10-28",
            versions -> versions.get(1).effectiveUntil = "2027-01-01",
            versions -> versions.get(1).schedule = null);
        for (int i = 0; i < invalid.size(); i++) {
            UserHabitResponse habit = singleDay(105, 1, 1, "23:00");
            habit.scheduleVersions = new ArrayList<>(Arrays.asList(version("2026-01-01", "2026-10-28", 1, "07:00"),
                version("2026-10-29", null, 4, "20:00")));
            invalid.get(i).accept(habit.scheduleVersions);
            RoutineData.Day day = data(habit).day(LocalDate.of(2026, 10, 29));
            assertFalse("case " + i, day.agendaComplete);
            assertEquals("case " + i, 1, day.incompleteHabits.size());
            assertTrue("case " + i, day.executions.isEmpty());
        }
    }

    @Test
    public void projectionIsAnImmutableSnapshotOfMutableTransportObjects() {
        UserHabitResponse habit = singleDay(105, 1, 1, "20:00", "07:00");
        habit.habitName = "Original";
        habit.habitCategory = "Categoria";
        habit.habitDescription = "Descricao";
        List<UserHabitResponse> source = new ArrayList<>(Collections.singletonList(habit));
        RoutineData routine = RoutineData.from(source, CLOCK);
        RoutineData.Day before = routine.day(MONDAY);
        assertEquals(Arrays.asList("20:00", "07:00"), habit.schedule.get(0).startTimes);
        source.clear();
        habit.habitName = "Alterado";
        habit.frequency = 999;
        habit.schedule.get(0).startTimes.clear();
        habit.daysOfWeek.clear();
        RoutineData.Day after = routine.day(MONDAY);
        assertEquals("Original", after.executions.get(0).habit.name);
        assertEquals("Categoria", after.executions.get(0).habit.category);
        assertEquals("Descricao", after.executions.get(0).habit.description);
        assertEquals(Integer.valueOf(2), after.executions.get(0).habit.frequency);
        assertEquals(before.totalLiters, after.totalLiters);
        assertEquals(2, after.executions.size());
        assertThrows(UnsupportedOperationException.class, () -> routine.habits.clear());
        assertThrows(UnsupportedOperationException.class, () -> after.executions.clear());
        assertThrows(UnsupportedOperationException.class, () -> after.summaries.clear());
        assertThrows(UnsupportedOperationException.class, () -> after.incompleteHabits.clear());
        assertThrows(UnsupportedOperationException.class, () -> routine.habits.get(0).schedule.clear());
        assertThrows(UnsupportedOperationException.class, () -> routine.habits.get(0).schedule.get(DayOfWeek.MONDAY).clear());
    }

    @Test
    public void nullCollectionOrUnidentifiableRecordsNeverBecomeAnEmptyDay() {
        assertThrows(IllegalArgumentException.class, () -> RoutineData.from(null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> data((UserHabitResponse) null));
        UserHabitResponse missingId = singleDay(105, 1, 1, "07:00");
        missingId.id = null;
        assertThrows(IllegalArgumentException.class, () -> data(missingId));
        UserHabitResponse missingOwner = singleDay(105, 1, 1, "07:00");
        missingOwner.userId = null;
        assertThrows(IllegalArgumentException.class, () -> data(missingOwner));
        UserHabitResponse missingType = singleDay(105, 1, 1, "07:00");
        missingType.habitId = null;
        assertThrows(IllegalArgumentException.class, () -> data(missingType));
    }

    @Test
    public void repeatedRecordIdsAreRejectedRatherThanDoubleCounted() {
        UserHabitResponse habit = singleDay(105, 1, 1, "07:00");
        assertThrows(IllegalArgumentException.class, () -> data(habit, habit));
        assertThrows(IllegalArgumentException.class, () -> data(habit, singleDay(105, 4, 1, "08:00")));
    }

    private RoutineData data(UserHabitResponse... habits) {
        return RoutineData.from(Arrays.asList(habits), CLOCK);
    }

    private UserHabitResponse singleDay(int id, int type, int weekday, String... times) {
        UserHabitResponse value = new UserHabitResponse();
        value.id = id;
        value.userId = 42;
        value.habitId = type;
        value.frequency = times.length;
        value.daysOfWeek = new ArrayList<>(Collections.singletonList(weekday));
        value.schedule = new ArrayList<>(Collections.singletonList(schedule(weekday, times)));
        return value;
    }

    private UserHabitScheduleResponse schedule(int weekday, String... times) {
        UserHabitScheduleResponse value = new UserHabitScheduleResponse();
        value.dayOfWeek = weekday;
        value.startTimes = new ArrayList<>(Arrays.asList(times));
        return value;
    }

    private UserHabitScheduleVersionResponse version(String from, String until, int weekday, String... times) {
        UserHabitScheduleVersionResponse value = new UserHabitScheduleVersionResponse();
        value.effectiveFrom = from;
        value.effectiveUntil = until;
        value.frequency = times.length;
        value.daysOfWeek = Collections.singletonList(weekday);
        value.schedule = Collections.singletonList(schedule(weekday, times));
        return value;
    }
}
