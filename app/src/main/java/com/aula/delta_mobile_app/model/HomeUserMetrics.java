package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class HomeUserMetrics {
    private HomeUserMetrics() {}

    public static ResidentialEfficiencyRankingResponse ranking(List<ResidentialEfficiencyRankingResponse> rows, Integer userId, LocalDate today) {
        ResidentialEfficiencyRankingResponse latest = null;
        for (ResidentialEfficiencyRankingResponse row : rows) {
            if (row == null || !Objects.equals(userId, row.userId) || !valid(row.referenceMonth, today)) continue;
            if (latest == null || row.referenceMonth.compareTo(latest.referenceMonth) > 0)
                latest = row;
        }
        return latest;
    }

    public static MonthlyVariationResponse variation(List<MonthlyVariationResponse> rows, Integer propertyId, LocalDate today) {
        MonthlyVariationResponse latest = null;
        for (MonthlyVariationResponse row : rows) {
            if (row == null || !Objects.equals(propertyId, row.propertyId) || !valid(row.referenceMonth, today)) continue;
            if (latest == null || row.referenceMonth.compareTo(latest.referenceMonth) > 0)
                latest = row;
        }
        return latest;
    }

    private static boolean valid(String date, LocalDate today) {
        try {
            return date != null && !LocalDate.parse(date).isAfter(today);
        } catch (java.time.DateTimeException invalid) {
            return false;
        }
    }
}
