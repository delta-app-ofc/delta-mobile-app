package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.*;

import java.util.*;

public final class SectionData {
    private SectionData() {}

    public static List<UserPropertyResponse> properties(List<UserPropertyResponse> source, Integer user) {
        List<UserPropertyResponse> result = new ArrayList<>();
        if (source != null && user != null)
            for (UserPropertyResponse row : source)
                if (row != null && user.equals(row.userId) && row.propertyId != null && row.propertyId > 0) result.add(row);
        return result;
    }

    public static Integer selectedProperty(List<UserPropertyResponse> properties, Integer selected) {
        if (properties == null || properties.isEmpty()) return null;
        for (UserPropertyResponse property : properties) if (Objects.equals(property.propertyId, selected)) return selected;
        return properties.get(0).propertyId;
    }

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

}
