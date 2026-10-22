package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class DashboardMetricsTest {
    private ApiModels.ConsumptionDailyResponse reading(int property, String day, String liters) {
        ApiModels.ConsumptionDailyResponse r = new ApiModels.ConsumptionDailyResponse();
        r.propertyId = property;
        r.fullDate = day;
        r.totalLiters = new BigDecimal(liters);
        return r;
    }

    @Test
    public void filtersPropertyInvalidAndFutureReadings() {
        DashboardMetrics m = DashboardMetrics.from(Arrays.asList(reading(1, "2026-10-18", "187"), reading(2, "2026-10-18", "999"), reading(1, "2026-10-19", "800"),
            reading(1, "bad", "55"), reading(1, "2026-10-18", "-1")), 1, LocalDate.of(2026, 10, 18), 0);
        assertEquals(new BigDecimal("187"), m.current);
        assertNull(m.previous);
    }

    @Test
    public void weeklyWindowsDoNotOverlapAcrossYear() {
        DashboardMetrics m = DashboardMetrics.from(Arrays.asList(reading(1, "2025-12-29", "10"), reading(1, "2026-01-04", "20"), reading(1, "2025-12-28", "7"),
            reading(1, "2025-12-22", "3"), reading(1, "2025-12-21", "999")), 1, LocalDate.of(2026, 1, 4), 1);
        assertEquals(new BigDecimal("30"), m.current);
        assertEquals(new BigDecimal("10"), m.previous);
    }

    @Test
    public void monthlyComparisonUsesSameElapsedDays() {
        DashboardMetrics m = DashboardMetrics.from(Arrays.asList(reading(1, "2026-10-18", "100"), reading(1, "2026-09-18", "50"), reading(1, "2026-09-19", "900")), 1,
            LocalDate.of(2026, 10, 18), 2);
        assertEquals(new BigDecimal("100"), m.current);
        assertEquals(new BigDecimal("50"), m.previous);
    }

    @Test
    public void sixMonthHistoryConvertsToCubicMetersAndKeepsGaps() {
        DashboardMetrics m = DashboardMetrics.from(Arrays.asList(reading(1, "2026-05-01", "1000"), reading(1, "2026-05-02", "500"), reading(1, "2026-04-30", "9000"),
            reading(1, "2026-10-18", "0")), 1, LocalDate.of(2026, 10, 18), 0);
        assertEquals(0, new BigDecimal("1.5").compareTo(m.months[0]));
        assertNull(m.months[1]);
        assertEquals(0, BigDecimal.ZERO.compareTo(m.months[5]));
    }

    @Test
    public void emptySuccessHasZeroCurrentAndUnavailableComparison() {
        DashboardMetrics m = DashboardMetrics.from(Collections.emptyList(), 1, LocalDate.of(2026, 10, 18), 0);
        assertEquals(BigDecimal.ZERO, m.current);
        assertNull(m.previous);
    }

    @Test
    public void monthlyComparisonClampsToFebruary() {
        DashboardMetrics m = DashboardMetrics.from(Arrays.asList(reading(1, "2026-02-28", "80"), reading(1, "2026-03-01", "100")), 1, LocalDate.of(2026, 3, 31), 2);
        assertEquals(new BigDecimal("80"), m.previous);
        assertEquals(new BigDecimal("100"), m.current);
    }
}
