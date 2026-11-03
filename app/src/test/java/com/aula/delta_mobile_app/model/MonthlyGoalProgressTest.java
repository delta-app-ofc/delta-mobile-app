package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class MonthlyGoalProgressTest {
    @Test
    public void percentageAlwaysRoundsDown() {
        assertEquals(new BigDecimal("34"), MonthlyGoalProgress.percentage(new BigDecimal("1000"), new BigDecimal("349.99")));
        assertEquals(BigDecimal.ZERO, MonthlyGoalProgress.percentage(new BigDecimal("1000"), BigDecimal.ZERO));
    }

    @Test
    public void exceededGoalKeepsTheActualPercentage() {
        assertEquals(new BigDecimal("125"), MonthlyGoalProgress.percentage(new BigDecimal("1000"), new BigDecimal("1259.99")));
    }

    @Test
    public void missingOrInvalidValuesDoNotBecomeZero() {
        assertNull(MonthlyGoalProgress.percentage(null, BigDecimal.ZERO));
        assertNull(MonthlyGoalProgress.percentage(BigDecimal.TEN, null));
        assertNull(MonthlyGoalProgress.percentage(BigDecimal.ZERO, BigDecimal.TEN));
        assertNull(MonthlyGoalProgress.percentage(BigDecimal.TEN, BigDecimal.ONE.negate()));
    }
}
