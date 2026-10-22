package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public final class DashboardMetrics {
    public final BigDecimal current, previous;
    public final BigDecimal[] months = new BigDecimal[6];
    public final YearMonth firstMonth;

    private DashboardMetrics(BigDecimal current, BigDecimal previous, YearMonth first) {
        this.current = current;
        this.previous = previous;
        this.firstMonth = first;
    }

    public static DashboardMetrics from(List<ApiModels.ConsumptionDailyResponse> values, Integer property, LocalDate today, int period) {
        LocalDate start = period == 0 ? today : period == 1 ? today.minusDays(6) : today.withDayOfMonth(1);
        LocalDate priorStart = period == 0 ? today.minusDays(1) : period == 1 ? start.minusDays(7) : start.minusMonths(1);
        LocalDate priorEnd = period == 0 ? priorStart : period == 1 ? start.minusDays(1) : priorStart.plusDays(Math.min(today.getDayOfMonth(),
            YearMonth.from(priorStart).lengthOfMonth()) - 1);
        BigDecimal current = BigDecimal.ZERO, previous = null;
        YearMonth first = YearMonth.from(today).minusMonths(5);
        DashboardMetrics result = new DashboardMetrics(null, null, first);
        if (values != null)
            for (ApiModels.ConsumptionDailyResponse item : values) {
            if (item == null || !Objects.equals(property, item.propertyId) || item.totalLiters == null || item.totalLiters.signum() < 0 || item.fullDate == null) continue;
            LocalDate date;
            try {
                date = LocalDate.parse(item.fullDate);
            } catch (java.time.DateTimeException invalid) {
                continue;
            }
            if (date.isAfter(today)) continue;
            if (!date.isBefore(start)) current = current.add(item.totalLiters);
            if (!date.isBefore(priorStart) && !date.isAfter(priorEnd))
                previous = previous == null ? item.totalLiters : previous.add(item.totalLiters);
            int index = (int)
            java.time.temporal.ChronoUnit.MONTHS.between(first, YearMonth.from(date));
            if (index >= 0 && index < 6)
                result.months[index] = (result.months[index] == null ? BigDecimal.ZERO : result.months[index]).add(item.totalLiters.movePointLeft(3));
        }
        DashboardMetrics completed = new DashboardMetrics(current, previous, first);
        System.arraycopy(result.months, 0, completed.months, 0, 6);
        return completed;
    }
}
