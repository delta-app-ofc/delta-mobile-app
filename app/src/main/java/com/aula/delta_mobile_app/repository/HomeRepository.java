package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public final class HomeRepository {
    private final AuthRepository auth;

    public HomeRepository(AuthRepository auth) {
        this.auth = auth;
    }

    private static <T> ScreenState<T> result(Response<T> response) {
        if (!response.isSuccessful()) return ApiErrors.from(response);
        return response.body() == null ? ScreenState.error("O servidor nÃ£o retornou dados.") : ScreenState.success(response.body());
    }

    public void me(Consumer<ScreenState<Profile>> callback) {
        auth.resource(() -> ScreenState.success(auth.currentProfile()), callback);
    }

    public void waterCost(Integer propertyId, BigDecimal liters, String date, Consumer<ScreenState<WaterCostResponse>> callback) {
        auth.resource( () -> result(auth.api().waterCost(propertyId, liters.movePointLeft(3), date).execute()), callback);
    }

    public void monthlyVariation(Integer propertyId, Consumer<ScreenState<List<MonthlyVariationResponse>>> callback) {
        auth.resource(() -> result(auth.api().monthlyVariation(propertyId).execute()), callback);
    }
}
