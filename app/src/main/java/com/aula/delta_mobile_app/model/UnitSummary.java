package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import java.math.BigDecimal;

public final class UnitSummary {
    public final UserPropertyResponse property;
    public final BigDecimal monthlyLiters;

    public UnitSummary(UserPropertyResponse property, BigDecimal monthlyLiters) { this.property = property; this.monthlyLiters = monthlyLiters; }
}
