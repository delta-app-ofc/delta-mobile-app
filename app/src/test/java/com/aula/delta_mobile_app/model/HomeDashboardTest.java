package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.ConsumptionDailyResponse;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public class HomeDashboardTest {
    private ConsumptionDailyResponse reading(String date, String liters, String cost) {
        ConsumptionDailyResponse item = new ConsumptionDailyResponse();
        item.fullDate = date;
        item.totalLiters = liters == null ? null : new BigDecimal(liters);
        item.costValue = cost == null ? null : new BigDecimal(cost);
        return item;
    }

    @Test
    public void totalsUseCurrentMonthAndActualTodayRatherThanLatestReading() {
        HomeDashboard result = HomeDashboard.from(Arrays.asList(reading("2026-09-30", "900", "90"), reading("2026-10-01", "100", "10"), reading("2026-10-13", "200", "20"),
            reading("2026-10-15", "500", "50"), reading("invalid", "1000", "100")), LocalDate.of(2026, 10, 14));
        assertEquals(BigDecimal.ZERO, result.todayLiters);
        assertEquals(new BigDecimal("300"), result.monthLiters);
        assertEquals(new BigDecimal("30"), result.monthCost);
        assertEquals(3, result.history.size());
    }

    @Test
    public void zeroReadingKeepsItsPresenceFlag() {
        HomeDashboard result = HomeDashboard.from(Arrays.asList(reading("2026-10-14", "0", null)), LocalDate.of(2026, 10, 14));
        assertEquals(BigDecimal.ZERO, result.todayLiters);
        assertNull(result.monthCost);
        assertTrue(result.hasTodayReading);
        assertTrue(result.hasMonthReading);
    }

    @Test
    public void chartKeepsSevenMostRecentValidReadingsInDateOrder() {
        java.util.List<ConsumptionDailyResponse> readings = new java.util.ArrayList<>();
        for (int day = 10; day >= 1; day--)
            readings.add(reading(String.format("2026-10-%02d", day), "100", "1"));
        HomeDashboard result = HomeDashboard.from(readings, LocalDate.of(2026, 10, 14));
        assertEquals(7, result.history.size());
        assertEquals("2026-10-04", result.history.get(0).fullDate);
        assertEquals("2026-10-10", result.history.get(6).fullDate);
        assertEquals(new BigDecimal("1000"), result.monthLiters);
    }

    @Test
    public void historicalReadingsDoNotBecomeCurrentConsumptionOrCost() {
        HomeDashboard result = HomeDashboard.from(Arrays.asList(reading("2026-09-15", "230", "54")), LocalDate.of(2026, 10, 17));
        assertEquals(BigDecimal.ZERO, result.todayLiters);
        assertEquals(BigDecimal.ZERO, result.monthLiters);
        assertNull(result.monthCost);
        assertEquals(1, result.history.size());
        assertEquals("2026-09-15", result.history.get(0).fullDate);
    }

    @Test
    public void readingsWithoutVolumesDoNotAddConsumption() {
        HomeDashboard result = HomeDashboard.from(Arrays.asList(reading("2026-10-17", null, null)), LocalDate.of(2026, 10, 17));
        assertEquals(BigDecimal.ZERO, result.todayLiters);
        assertEquals(BigDecimal.ZERO, result.monthLiters);
        assertTrue(result.history.isEmpty());
    }

    @Test
    public void successfulEmptyResponseShowsZeroForBothPeriods() {
        HomeDashboard result = HomeDashboard.from(java.util.Collections.emptyList(), LocalDate.of(2026, 10, 17));
        assertEquals(BigDecimal.ZERO, result.todayLiters);
        assertEquals(BigDecimal.ZERO, result.monthLiters);
        assertFalse(result.hasTodayReading);
        assertFalse(result.hasMonthReading);
    }

    @Test
    public void failedOrPendingRequestMustNotMasqueradeAsZeroConsumption() {
        HomeDashboard result = HomeDashboard.unavailable();
        assertNull(result.todayLiters);
        assertNull(result.monthLiters);
    }
}
