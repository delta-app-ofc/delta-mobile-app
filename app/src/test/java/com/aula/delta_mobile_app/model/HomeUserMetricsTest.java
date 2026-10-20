package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.*;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

public class HomeUserMetricsTest {
    private ResidentialEfficiencyRankingResponse rank(int userId, String month) {
        ResidentialEfficiencyRankingResponse row = new ResidentialEfficiencyRankingResponse();
        row.userId = userId;
        row.referenceMonth = month;
        return row;
    }

    @Test
    public void rankingUsesLatestAvailableMonthOfAuthenticatedUser() {
        ResidentialEfficiencyRankingResponse selected = rank(4, "2026-10-01");
        assertSame(selected, HomeUserMetrics.ranking(Arrays.asList(rank(4, "2026-09-01"), rank(5, "2026-10-01"), selected, rank(4, "2026-11-01")), 4, LocalDate.of(2026, 10, 14)));
    }

    @Test
    public void anotherUsersRankingNeverFillsTheHome() {
        assertNull(HomeUserMetrics.ranking(Arrays.asList(rank(5, "2026-10-01")), 4, LocalDate.of(2026, 10, 14)));
    }

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
        assertNull(HomeUserMetrics.ranking(Arrays.asList(rank(4, "bad-date"), rank(4, "2026-11-01")), 4, LocalDate.of(2026, 10, 14)));
    }
}
