package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.ApiModels.AddressResponse;
import java.math.BigDecimal;

public final class UnitSummary {
    public final UserPropertyResponse property;
    public final BigDecimal monthlyLiters;
    public final AddressResponse address;

    public UnitSummary(UserPropertyResponse property, BigDecimal monthlyLiters) { this(property, monthlyLiters, null); }
    public UnitSummary(UserPropertyResponse property, BigDecimal monthlyLiters, AddressResponse address) { this.property = property; this.monthlyLiters = monthlyLiters; this.address = address; }
}
