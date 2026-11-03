package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class MonthlyGoalGuidance {
    public final BigDecimal dailyLiters, reductionPercent, savingsLiters;
    public final boolean exceeded;

    private MonthlyGoalGuidance(BigDecimal dailyLiters, BigDecimal reductionPercent, BigDecimal savingsLiters, boolean exceeded) {
        this.dailyLiters = dailyLiters;
        this.reductionPercent = reductionPercent;
        this.savingsLiters = savingsLiters;
        this.exceeded = exceeded;
    }

    public static MonthlyGoalGuidance calculate(BigDecimal goal, BigDecimal consumed, LocalDate today) {
        if (goal == null || goal.signum() <= 0 || consumed == null || consumed.signum() < 0 || today == null) return null;
        int remainingDays = today.lengthOfMonth() - today.getDayOfMonth() + 1;
        BigDecimal remaining = goal.subtract(consumed).max(BigDecimal.ZERO);
        BigDecimal daily = remaining.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.DOWN);
        BigDecimal projected = consumed.multiply(BigDecimal.valueOf(today.lengthOfMonth())).divide(BigDecimal.valueOf(today.getDayOfMonth()), MathContext.DECIMAL128);
        BigDecimal savings = projected.subtract(goal).max(BigDecimal.ZERO);
        BigDecimal reduction = projected.signum() == 0 ? BigDecimal.ZERO : savings.multiply(BigDecimal.valueOf(100)).divide(projected, 2, RoundingMode.HALF_UP);
        return new MonthlyGoalGuidance(daily, reduction, savings.setScale(2, RoundingMode.HALF_UP), consumed.compareTo(goal) > 0);
    }
}
