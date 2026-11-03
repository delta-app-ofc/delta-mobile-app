package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class MonthlyGoalGuidanceTest {
    @Test
    public void recalculatesRemainingAllowanceAndProjectedReduction() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        MonthlyGoalGuidance initial = MonthlyGoalGuidance.calculate(new BigDecimal("300"), new BigDecimal("50"), date);
        assertEquals(new BigDecimal("11.90"), initial.dailyLiters);
        assertEquals(0, initial.savingsLiters.signum());
        assertEquals(0, initial.reductionPercent.signum());
        MonthlyGoalGuidance increased = MonthlyGoalGuidance.calculate(new BigDecimal("300"), new BigDecimal("200"), date);
        assertEquals(new BigDecimal("4.76"), increased.dailyLiters);
        assertEquals(new BigDecimal("50.00"), increased.reductionPercent);
        assertEquals(new BigDecimal("300.00"), increased.savingsLiters);
        assertFalse(increased.exceeded);
    }

    @Test
    public void zeroConsumptionNeverDividesByZero() {
        MonthlyGoalGuidance result = MonthlyGoalGuidance.calculate(new BigDecimal("200"), BigDecimal.ZERO, LocalDate.of(2026, 11, 2));
        assertEquals(new BigDecimal("6.89"), result.dailyLiters);
        assertEquals(0, result.reductionPercent.signum());
        assertEquals(0, result.savingsLiters.signum());
    }

    @Test
    public void exceededGoalNeverRecommendsNegativeConsumption() {
        MonthlyGoalGuidance result = MonthlyGoalGuidance.calculate(new BigDecimal("100"), new BigDecimal("120"), LocalDate.of(2026, 11, 30));
        assertTrue(result.exceeded);
        assertEquals(0, result.dailyLiters.signum());
        assertEquals(new BigDecimal("16.67"), result.reductionPercent);
        assertEquals(new BigDecimal("20.00"), result.savingsLiters);
    }

    @Test
    public void handlesLeapYearLastDayAndGoalExactlyReached() {
        LocalDate date = LocalDate.of(2028, 2, 29);
        MonthlyGoalGuidance result = MonthlyGoalGuidance.calculate(new BigDecimal("100"), new BigDecimal("60"), date);
        assertEquals(new BigDecimal("40.00"), result.dailyLiters);
        MonthlyGoalGuidance reached = MonthlyGoalGuidance.calculate(new BigDecimal("100"), new BigDecimal("100"), date);
        assertEquals(0, reached.dailyLiters.signum());
        assertEquals(0, reached.reductionPercent.signum());
        assertFalse(reached.exceeded);
    }

    @Test
    public void missingOrInvalidInputsDoNotBecomeConfirmedZero() {
        LocalDate date = LocalDate.of(2026, 11, 2);
        assertNull(MonthlyGoalGuidance.calculate(null, BigDecimal.ZERO, date));
        assertNull(MonthlyGoalGuidance.calculate(BigDecimal.TEN, null, date));
        assertNull(MonthlyGoalGuidance.calculate(BigDecimal.ZERO, BigDecimal.TEN, date));
        assertNull(MonthlyGoalGuidance.calculate(BigDecimal.TEN, new BigDecimal("-1"), date));
        assertNull(MonthlyGoalGuidance.calculate(BigDecimal.TEN, BigDecimal.ZERO, null));
    }
}
