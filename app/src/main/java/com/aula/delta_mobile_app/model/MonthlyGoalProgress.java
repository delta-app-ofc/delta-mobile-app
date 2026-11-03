package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonthlyGoalProgress {
    private MonthlyGoalProgress() {}

    public static BigDecimal percentage(BigDecimal goal, BigDecimal consumed) {
        if (goal == null || goal.signum() <= 0 || consumed == null || consumed.signum() < 0) return null;
        return consumed.multiply(BigDecimal.valueOf(100)).divide(goal, 0, RoundingMode.FLOOR);
    }
}
