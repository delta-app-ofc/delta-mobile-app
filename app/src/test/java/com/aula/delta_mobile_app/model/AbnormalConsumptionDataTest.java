package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AbnormalConsumptionData.*;
import com.aula.delta_mobile_app.model.ApiModels.ConsumptionDailyResponse;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AbnormalConsumptionDataTest {
    private ConsumptionDailyResponse reading(int property, String day, String liters) {
        ConsumptionDailyResponse value = new ConsumptionDailyResponse();
        value.propertyId = property;
        value.fullDate = day;
        value.totalLiters = new BigDecimal(liters);
        return value;
    }

    @Test
    public void successfulEmptySourceShowsZeroButNeverFutureConsumption() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Collections.emptyList(), LocalDate.of(2026, 10, 17));
        assertEquals(BigDecimal.ZERO, data.day(data.reference));
        assertNull(data.day(data.reference.plusDays(1)));
        for (Bucket bucket : data.periods(Period.MONTH))
            assertEquals(BigDecimal.ZERO, bucket.liters);
    }

    @Test
    public void sevenCalendarDaysDoNotBecomeSevenMostRecentRecords() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Arrays.asList(reading(1, "2026-10-01", "99"), reading(1, "2026-10-17", "20")), LocalDate.of(2026, 10, 17));
        assertNull(data.day(data.reference.minusDays(6)));
        assertEquals(new BigDecimal("20"), data.day(data.reference));
    }

    @Test
    public void recordsOfOtherPropertiesNegativeInvalidAndFutureAreNotCounted() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Arrays.asList(reading(2, "2026-10-17", "100"), reading(1, "invalid", "4"), reading(1, "2026-10-18", "5"),
            reading(1, "2026-10-16", "-1")), LocalDate.of(2026, 10, 17));
        assertTrue(data.readings.isEmpty());
        assertFalse(data.emptyResponse);
        assertNull(data.day(data.reference));
    }

    @Test
    public void identicalDuplicatesAreNotDoubledAndConflictsBecomeGaps() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Arrays.asList(reading(1, "2026-10-17", "10"), reading(1, "2026-10-17", "10.0"),
            reading(1, "2026-10-16", "2"), reading(1, "2026-10-16", "3")), LocalDate.of(2026, 10, 17));
        assertEquals(0, new BigDecimal("10").compareTo(data.day(data.reference)));
        assertNull(data.day(data.reference.minusDays(1)));
    }

    @Test
    public void partialWeekComparesEquivalentElapsedDays() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Arrays.asList(reading(1, "2026-10-19", "10"), reading(1, "2026-10-20", "20"), reading(1, "2026-10-12", "5"),
            reading(1, "2026-10-13", "5"), reading(1, "2026-10-14", "900")), LocalDate.of(2026, 10, 20));
        List<Bucket> buckets = data.periods(Period.WEEK);
        assertEquals(new BigDecimal("30"), buckets.get(3).liters);
        assertEquals(new BigDecimal("10"), buckets.get(2).liters);
        assertEquals(LocalDate.of(2026, 10, 13), buckets.get(2).end);
        assertEquals("S-1", buckets.get(2).label);
    }

    @Test
    public void shortMonthsUseCommonComparableDurationAndExposeCutoff() {
        AbnormalConsumptionData data = AbnormalConsumptionData.from(1, Collections.emptyList(), LocalDate.of(2026, 3, 31));
        List<Bucket> buckets = data.periods(Period.MONTH);
        for (Bucket bucket : buckets)
            assertEquals(27, ChronoUnit.DAYS.between(bucket.start, bucket.end));
        assertEquals(LocalDate.of(2026, 3, 28), buckets.get(3).end);
    }

    @Test
    public void leapYearComparisonKeepsEquivalentDuration() {
        List<Bucket> buckets = AbnormalConsumptionData.from(1, Collections.emptyList(), LocalDate.of(2024, 2, 29)).periods(Period.YEAR);
        for (Bucket bucket : buckets)
            assertEquals(59, ChronoUnit.DAYS.between(bucket.start, bucket.end));
    }

    @Test
    public void percentagesUseRealReferenceAndNeverDivideByZero() {
        assertEquals(new BigDecimal("67"), AbnormalConsumptionData.variation(new BigDecimal("500"), new BigDecimal("300")));
        assertEquals(new BigDecimal("17"), AbnormalConsumptionData.variation(new BigDecimal("2100"), new BigDecimal("1800")));
        assertNull(AbnormalConsumptionData.variation(BigDecimal.ZERO, BigDecimal.ZERO));
        assertNull(AbnormalConsumptionData.variation(BigDecimal.ONE, null));
    }
}
