package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public final class MonthlyGoal {
    private MonthlyGoal() {}

    public static String key(int userId, int propertyId, YearMonth month) {
        if (userId <= 0 || propertyId <= 0 || month == null) throw new IllegalArgumentException();
        return "goal:" + userId + ":" + propertyId + ":" + month;
    }

    public static BigDecimal parse(String text) {
        String value = text == null ? "" : text.trim();
        if (!value.matches("[0-9]+([,.][0-9]{1,2})?")) throw new IllegalArgumentException();
        BigDecimal amount = new BigDecimal(value.replace(',', '.'));
        if (amount.signum() <= 0) throw new IllegalArgumentException();
        return amount.stripTrailingZeros();
    }
}
