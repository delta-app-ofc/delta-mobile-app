package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HomeDashboard {
    public BigDecimal todayLiters, monthLiters, monthCost;
    public boolean hasTodayReading, hasMonthReading;
    public final List<ApiModels.ConsumptionDailyResponse> history = new ArrayList<>();

    public static HomeDashboard from(List<ApiModels.ConsumptionDailyResponse> readings, LocalDate today) {
        HomeDashboard result = new HomeDashboard();
        result.todayLiters = BigDecimal.ZERO;
        result.monthLiters = BigDecimal.ZERO;
        for (ApiModels.ConsumptionDailyResponse reading : readings) {
            if (reading == null || reading.fullDate == null) continue;
            LocalDate date;
            try {
                date = LocalDate.parse(reading.fullDate);
            } catch (java.time.DateTimeException invalid) {
                continue;
            }
            if (date.isAfter(today)) continue;
            if (reading.totalLiters != null) {
                result.history.add(reading);
                if (date.equals(today)) {
                    result.hasTodayReading = true;
                    result.todayLiters = add(result.todayLiters, reading.totalLiters);
                }
                if (YearMonth.from(date).equals(YearMonth.from(today))) {
                    result.hasMonthReading = true;
                    result.monthLiters = add(result.monthLiters, reading.totalLiters);
                }
            }
            if (YearMonth.from(date).equals(YearMonth.from(today)) && reading.costValue != null)
                result.monthCost = add(result.monthCost, reading.costValue);
        }
        result.history.sort(Comparator.comparing(item -> item.fullDate));
        if (result.history.size() > 7) result.history.subList(0, result.history.size() - 7).clear();
        return result;
    }

    public static HomeDashboard unavailable() {
        return new HomeDashboard();
    }

    private static BigDecimal add(BigDecimal previous, BigDecimal next) {
        return previous == null ? next : previous.add(next);
    }
}
