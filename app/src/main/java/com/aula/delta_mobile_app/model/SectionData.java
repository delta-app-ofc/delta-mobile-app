package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.*;

import java.util.*;

public final class SectionData {
    private SectionData() {}

    public static List<LastWaterBillResponse> bills(List<LastWaterBillResponse> source, Integer user) {
        List<LastWaterBillResponse> result = new ArrayList<>();
        if (source != null && user != null)
            for (LastWaterBillResponse row : source)
                if (row != null && user.equals(row.userId)) result.add(row);
        result.sort(Comparator.comparing((LastWaterBillResponse row) -> row.month == null ? "" : row.month).reversed());
        return result;
    }

    public static List<UserHabitResponse> habits(List<UserHabitResponse> source, Integer user) {
        List<UserHabitResponse> result = new ArrayList<>();
        if (source != null && user != null)
            for (UserHabitResponse row : source)
                if (row != null && user.equals(row.userId)) result.add(row);
        return result;
    }

    public static List<PropertyRankingResponse> rankings(List<PropertyRankingResponse> source, Set<Integer> properties) {
        List<PropertyRankingResponse> result = new ArrayList<>();
        if (source != null && properties != null)
            for (PropertyRankingResponse row : source)
                if (row != null && row.propertyId != null && properties.contains(row.propertyId))
                    result.add(row);
        result.sort(Comparator.comparing((PropertyRankingResponse row) -> row.rankEfficiency, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }
}
