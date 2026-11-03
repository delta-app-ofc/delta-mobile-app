package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.*;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

public class HomeUserMetricsTest {
    @Test
    public void variationIsFilteredBySelectedProperty() {
        MonthlyVariationResponse first = new MonthlyVariationResponse();
        first.propertyId = 1;
        first.referenceMonth = "2026-10-01";
        MonthlyVariationResponse second = new MonthlyVariationResponse();
        second.propertyId = 2;
        second.referenceMonth = "2026-10-01";
        assertSame(second, HomeUserMetrics.variation(Arrays.asList(first, second), 2, LocalDate.of(2026, 10, 14)));
    }

    @Test
    public void malformedOrFutureReferenceMonthsAreNotDisplayed() {
        MonthlyVariationResponse row = new MonthlyVariationResponse();
        row.propertyId = 4;
        for (String month : new String[] { "bad-date", "2026-11-01" }) {
            row.referenceMonth = month;
            assertNull(HomeUserMetrics.variation(Arrays.asList(row), 4, LocalDate.of(2026, 10, 14)));
        }
    }
}
