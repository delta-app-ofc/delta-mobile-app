package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MonthlyGoalContext {
    public final int userId;
    public final UserPropertyResponse property;
    public final List<UserPropertyResponse> properties;
    public final YearMonth month;
    public final BigDecimal liters;
    public final BigDecimal currentMonthLiters;

    public MonthlyGoalContext(int userId, UserPropertyResponse property, List<UserPropertyResponse> properties, YearMonth month, BigDecimal liters, BigDecimal currentMonthLiters) {
        this.userId = userId;
        this.property = property;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
        this.month = month;
        this.liters = liters;
        this.currentMonthLiters = currentMonthLiters;
    }
}
