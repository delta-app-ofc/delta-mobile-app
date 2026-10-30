package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitScheduleResponse;
import com.aula.delta_mobile_app.model.RoutineFormValidator.Error;

import org.junit.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;

public class RoutineFormValidatorTest {
    @Test
    public void untouchedDraftHasNoErrorsUntilValidationAndRequiredFieldsAreIdentified() {
        RoutineFormState form = RoutineFormState.create(1);
        assertFalse(form.attempted);
        assertFalse(form.dirty);
        assertFalse(form.valid());
        assertTrue(form.errors.isEmpty());
        form = form.validate();
        assertEquals(Error.REQUIRED, form.errors.get(RoutineFormState.TYPE));
        assertEquals(Error.REQUIRED, form.errors.get(RoutineFormState.FREQUENCY));
        assertEquals(Error.REQUIRED, form.errors.get(RoutineFormState.DAYS));
        assertFalse(form.dirty);
    }

    @Test
    public void fiveExecutionsDistributedBetweenTwoDaysPreserveRawNumbersAndValidate() {
        RoutineFormState form = RoutineFormState.create(1).withHabit(1).withFrequency("005");
        form = day(form, DayOfWeek.MONDAY, "07:00", "20:00");
        form = day(form, DayOfWeek.WEDNESDAY, "08:00", "14:00", "21:00").withQuantity(DayOfWeek.MONDAY, "02").validate();
        assertTrue(form.valid());
        assertEquals("005", form.frequency);
        assertEquals("02", form.days.get(DayOfWeek.MONDAY).quantity);
    }

    @Test
    public void frequencyCanExceedSevenAndIsNotTheNumberOfSelectedDays() {
        RoutineFormState form = RoutineFormState.create(1).withHabit(1).withFrequency("8");
        form = day(form, DayOfWeek.MONDAY, "00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00").validate();
        assertTrue(form.valid());
        assertEquals(8, form.days.get(DayOfWeek.MONDAY).activeCount());
    }

    @Test
    public void invalidCatalogTypesAndNumericInputsHaveSpecificErrorsWithoutCoercion() {
        assertEquals(Error.INVALID_TYPE, valid().withHabit(99).validate().errors.get(RoutineFormState.TYPE));
        for (String raw : Arrays.asList("abc", "2.5", "1,5", "+1", "2147483648", "99999999999999999999")) {
            RoutineFormState form = valid().withFrequency(raw).validate();
            assertEquals(raw, form.frequency);
            assertEquals(raw, Error.INVALID_NUMBER, form.errors.get(RoutineFormState.FREQUENCY));
        }
        for (String raw : Arrays.asList("0", "-1"))
            assertEquals(Error.POSITIVE, valid().withFrequency(raw).validate().errors.get(RoutineFormState.FREQUENCY));
    }

    @Test
    public void selectedDayNeedsPositiveNumericQuantityAndTheSumMustMatchFrequency() {
        for (String raw : Arrays.asList("", "0", "-1", "abc", "2147483648")) {
            RoutineFormState form = valid().withQuantity(DayOfWeek.MONDAY, raw).validate();
            assertTrue(form.errors.containsKey(RoutineFormState.quantityKey(DayOfWeek.MONDAY)));
            assertEquals(raw, form.days.get(DayOfWeek.MONDAY).quantity);
        }
        RoutineFormState mismatch = valid().withFrequency("5").validate();
        assertEquals(Error.FREQUENCY_MISMATCH, mismatch.errors.get(RoutineFormState.DISTRIBUTION));
    }

    @Test
    public void minutePrecisionBoundsAllocationWithoutInventingASevenExecutionLimit() {
        RoutineFormState maximum = valid().withQuantity(DayOfWeek.MONDAY, "1440");
        assertEquals(1440, maximum.days.get(DayOfWeek.MONDAY).activeCount());
        assertEquals(1440, maximum.days.get(DayOfWeek.MONDAY).times.size());
        RoutineFormState impossible = valid().withQuantity(DayOfWeek.MONDAY, "1441").validate();
        assertEquals(Error.TOO_MANY_TIMES, impossible.errors.get(RoutineFormState.quantityKey(DayOfWeek.MONDAY)));
        assertEquals(0, impossible.days.get(DayOfWeek.MONDAY).activeCount());
        assertEquals(1, impossible.days.get(DayOfWeek.MONDAY).times.size());
    }

    @Test
    public void eachExecutionNeedsAnExactLocalHourAndInvalidRawTextIsPreserved() {
        for (String raw : Arrays.asList("", " ", "7:00", "07:0", "24:00", "12:60", "07:00:00", "07:00Z", "07:00 ")) {
            RoutineFormState form = valid().withTime(DayOfWeek.MONDAY, 0, raw).validate();
            assertEquals(raw, form.days.get(DayOfWeek.MONDAY).times.get(0));
            assertEquals(raw.trim().isEmpty() ? Error.REQUIRED : Error.INVALID_TIME, form.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 0)));
        }
        assertTrue(day(valid(), DayOfWeek.MONDAY, "00:00").validate().valid());
        assertTrue(day(valid(), DayOfWeek.MONDAY, "23:59").validate().valid());
    }

    @Test
    public void repeatedTimesAreAssociatedWithBothFieldsOnlyWithinTheSameDay() {
        RoutineFormState repeated = day(valid().withFrequency("2"), DayOfWeek.MONDAY, "07:00", "07:00").validate();
        assertEquals(Error.DUPLICATE_TIME, repeated.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 0)));
        assertEquals(Error.DUPLICATE_TIME, repeated.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 1)));
        RoutineFormState separateDays = day(valid().withFrequency("2"), DayOfWeek.TUESDAY, "07:00").validate();
        assertTrue(separateDays.valid());
    }

    @Test
    public void reducingAndIncreasingQuantityRestoresHiddenHoursAndIgnoresInactiveErrors() {
        RoutineFormState form = day(valid().withFrequency("2"), DayOfWeek.MONDAY, "07:00", "7:");
        RoutineFormState reduced = form.withQuantity(DayOfWeek.MONDAY, "1").withFrequency("1").validate();
        assertTrue(reduced.valid());
        assertEquals(Arrays.asList("07:00", "7:"), reduced.days.get(DayOfWeek.MONDAY).times);
        RoutineFormState restored = reduced.withQuantity(DayOfWeek.MONDAY, "2").withFrequency("2");
        assertEquals("7:", restored.days.get(DayOfWeek.MONDAY).times.get(1));
        assertEquals(Error.INVALID_TIME, restored.errors.get(RoutineFormState.timeKey(DayOfWeek.MONDAY, 1)));
    }

    @Test
    public void deselectingADayPreservesItsInputWithoutIncludingItInValidation() {
        RoutineFormState form = day(valid(), DayOfWeek.TUESDAY, "7:").selectDay(DayOfWeek.TUESDAY, false).validate();
        assertTrue(form.valid());
        assertEquals("7:", form.days.get(DayOfWeek.TUESDAY).times.get(0));
        assertEquals(0, form.days.get(DayOfWeek.TUESDAY).activeCount());
        assertFalse(form.selectDay(DayOfWeek.TUESDAY, true).valid());
    }

    @Test
    public void correctionRevalidatesAfterTheFirstAttemptWithoutChangingOtherFields() {
        RoutineFormState invalid = valid().withTime(DayOfWeek.MONDAY, 0, "7:").validate();
        RoutineFormState corrected = invalid.withTime(DayOfWeek.MONDAY, 0, "07:00");
        assertTrue(corrected.valid());
        assertEquals("1", corrected.frequency);
        assertEquals("7:", invalid.days.get(DayOfWeek.MONDAY).times.get(0));
    }

    @Test
    public void editingSeedsTheVerifiedRecordWithoutInventingLegacyDistribution() {
        UserHabitResponse value = new UserHabitResponse();
        value.id = 105;
        value.userId = 73;
        value.habitId = 1;
        value.frequency = 2;
        value.daysOfWeek = Collections.singletonList(1);
        UserHabitScheduleResponse schedule = new UserHabitScheduleResponse();
        schedule.dayOfWeek = 1;
        schedule.startTimes = Arrays.asList("07:00", "20:00");
        value.schedule = Collections.singletonList(schedule);
        RoutineData.Habit habit = RoutineData.from(Collections.singletonList(value), Clock.systemUTC()).habits.get(0);
        RoutineFormState form = RoutineFormState.edit(10, habit);
        assertEquals(RoutineFormState.Mode.EDIT, form.mode);
        assertEquals(Integer.valueOf(105), form.id);
        assertEquals(Integer.valueOf(73), form.userId);
        assertEquals(Arrays.asList("07:00", "20:00"), form.days.get(DayOfWeek.MONDAY).times);
        assertFalse(form.dirty);
        assertTrue(form.validate().valid());
        value.schedule = null;
        RoutineFormState legacy = RoutineFormState.edit(11, RoutineData.from(Collections.singletonList(value), Clock.systemUTC()).habits.get(0));
        assertTrue(legacy.legacy);
        assertEquals("2", legacy.frequency);
        assertFalse(legacy.days.get(DayOfWeek.MONDAY).selected);
        assertTrue(legacy.days.get(DayOfWeek.MONDAY).times.isEmpty());
    }

    @Test
    public void draftCollectionsAndValidationResultsAreImmutableAndDiscardPromptPreservesData() {
        RoutineFormState form = valid().validate().confirmDiscard(true);
        assertTrue(form.confirmDiscard);
        RoutineFormState continued = form.confirmDiscard(false);
        assertFalse(continued.confirmDiscard);
        assertEquals(form.frequency, continued.frequency);
        assertEquals(form.days.get(DayOfWeek.MONDAY).times, continued.days.get(DayOfWeek.MONDAY).times);
        assertThrows(UnsupportedOperationException.class, () -> form.days.clear());
        assertThrows(UnsupportedOperationException.class, () -> form.days.get(DayOfWeek.MONDAY).times.clear());
        assertThrows(UnsupportedOperationException.class, () -> form.errors.clear());
    }

    private RoutineFormState valid() {
        return day(RoutineFormState.create(1).withHabit(1).withFrequency("1"), DayOfWeek.MONDAY, "07:00");
    }

    private RoutineFormState day(RoutineFormState form, DayOfWeek day, String... times) {
        form = form.selectDay(day, true).withQuantity(day, String.valueOf(times.length));
        for (int i = 0; i < times.length; i++) form = form.withTime(day, i, times[i]);
        return form;
    }
}
