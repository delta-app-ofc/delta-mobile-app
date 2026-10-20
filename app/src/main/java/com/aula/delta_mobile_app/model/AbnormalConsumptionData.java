package com.aula.delta_mobile_app.model;

import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class AbnormalConsumptionData {
    public enum Period {
        WEEK, MONTH, YEAR
    }

    public static final class Bucket {
        public final LocalDate start, end;
        public final BigDecimal liters;
        public final String label;

        Bucket(LocalDate start, LocalDate end, BigDecimal liters, String label) {
            this.start = start;
            this.end = end;
            this.liters = liters;
            this.label = label;
        }
    }

    public final Integer propertyId;
    public final LocalDate reference;
    public final SortedMap<LocalDate, BigDecimal> readings;
    public final boolean emptyResponse;

    private AbnormalConsumptionData(Integer property, LocalDate day, SortedMap<LocalDate, BigDecimal> values, boolean empty) {
        propertyId = property;
        reference = day;
        readings = Collections.unmodifiableSortedMap(values);
        emptyResponse = empty;
    }

    public static AbnormalConsumptionData from(int property, List<ApiModels.ConsumptionDailyResponse> values, LocalDate day) {
        SortedMap<LocalDate, BigDecimal> readings = new TreeMap<>();
        Set<LocalDate> conflicts = new HashSet<>();
        for (ApiModels.ConsumptionDailyResponse value : values) {
            if (value == null || !Objects.equals(property, value.propertyId) || value.fullDate == null || value.totalLiters == null || value.totalLiters.signum() < 0
                || !Double.isFinite(value.totalLiters.doubleValue())) continue;
            LocalDate date;
            try {
                date = LocalDate.parse(value.fullDate);
            } catch (java.time.DateTimeException invalid) {
                continue;
            }
            if (date.isAfter(day) || conflicts.contains(date)) continue;
            if (readings.containsKey(date) && readings.get(date).compareTo(value.totalLiters) != 0) {
                readings.remove(date);
                conflicts.add(date);
            } else readings.put(date, value.totalLiters);
        }
        return new AbnormalConsumptionData(property, day, readings, values.isEmpty());
    }

    public BigDecimal day(LocalDate date) {
        if (date.isAfter(reference)) return null;
        return readings.containsKey(date) ? readings.get(date) : emptyResponse ? BigDecimal.ZERO : null;
    }

    public List<Bucket> periods(Period period) {
        LocalDate current = period == Period.WEEK ? reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : period == Period.MONTH ? reference.withDayOfMonth(1)
            : reference.withDayOfYear(1);
        long elapsed = java.time.temporal.ChronoUnit.DAYS.between(current, reference);
        for (int i = 3; i >= 0; i--) {
            LocalDate start = period == Period.WEEK ? current.minusWeeks(i) : period == Period.MONTH ? current.minusMonths(i) : current.minusYears(i);
            LocalDate next = period == Period.WEEK ? start.plusWeeks(1) : period == Period.MONTH ? start.plusMonths(1) : start.plusYears(1);
            elapsed = Math.min(elapsed, java.time.temporal.ChronoUnit.DAYS.between(start, next) - 1);
        }
        List<Bucket> result = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            LocalDate start = period == Period.WEEK ? current.minusWeeks(i) : period == Period.MONTH ? current.minusMonths(i) : current.minusYears(i);
            LocalDate fullEnd = period == Period.WEEK ? start.plusWeeks(1).minusDays(1) : period == Period.MONTH ? start.plusMonths(1).minusDays(1)
                : start.plusYears(1).minusDays(1);
            LocalDate end = start.plusDays(elapsed);
            if (end.isAfter(fullEnd)) end = fullEnd;
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<LocalDate, BigDecimal> row : readings.entrySet())
                if (!row.getKey().isBefore(start) && !row.getKey().isAfter(end))
                    total = total.add(row.getValue());
            result.add(new Bucket(start, end, total, i == 0 ? "Atual" : (period == Period.WEEK ? "S-" : period == Period.MONTH ? "M-" : "A-") + i));
        }
        return result;
    }

    public static BigDecimal variation(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 0, RoundingMode.HALF_UP);
    }
}
