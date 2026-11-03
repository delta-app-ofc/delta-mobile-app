package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
        auth.screenResource(() -> ScreenState.success(auth.currentProfile()), callback);
    }

    public void waterCost(Integer propertyId, BigDecimal liters, String date, Consumer<ScreenState<WaterCostResponse>> callback) {
        auth.screenResource(() -> result(auth.api().waterCost(propertyId, liters.movePointLeft(3), date).execute()), callback);
    }

    public void monthlyVariation(Integer propertyId, Consumer<ScreenState<List<MonthlyVariationResponse>>> callback) {
        auth.resource(() -> result(auth.api().monthlyVariation(propertyId).execute()), callback);
    }

    public void todayTemperature(Integer propertyId, Consumer<ScreenState<List<Double>>> callback) {
        auth.resource(() -> {
            Response<PropertyResponse> property = auth.api().getProperty(propertyId).execute();
            if (!property.isSuccessful()) return ApiErrors.from(property);
            if (property.body() == null || property.body().addressId == null) return ScreenState.error("Endereço da residência indisponível.");
            Response<AddressResponse> address = auth.api().getAddress(property.body().addressId).execute();
            if (!address.isSuccessful()) return ApiErrors.from(address);
            if (address.body() == null || address.body().latitude == null || address.body().longitude == null)
                return ScreenState.error("Localização da residência indisponível.");
            retrofit2.Response<com.google.gson.JsonObject> response = com.aula.delta_mobile_app.services.network.RetrofitClient.getOpenMeteoService().forecast(
                address.body().latitude.doubleValue(), address.body().longitude.doubleValue(), "temperature_2m", 1, "auto").execute();
            if (!response.isSuccessful() || response.body() == null) return ScreenState.error("Temperatura de hoje indisponível.");
            com.google.gson.JsonObject hourly = response.body().getAsJsonObject("hourly");
            if (hourly == null || !hourly.has("time") || !hourly.has("temperature_2m")) return ScreenState.error("Temperatura de hoje indisponível.");
            com.google.gson.JsonArray times = hourly.getAsJsonArray("time"), values = hourly.getAsJsonArray("temperature_2m");
            LocalDate today = LocalDate.now();
            List<Double> temperatures = new java.util.ArrayList<>(Collections.nCopies(24, null));
            int count = Math.min(times.size(), values.size()), samples = 0;
            for (int i = 0; i < count; i++) {
                String timestamp = times.get(i).getAsString();
                if (!timestamp.startsWith(today.toString()) || values.get(i).isJsonNull()) continue;
                double value = values.get(i).getAsDouble();
                if (!Double.isFinite(value)) continue;
                int hour = java.time.LocalTime.parse(timestamp.substring(11, 16)).getHour();
                temperatures.set(hour, value);
                samples++;
            }
            return samples == 0 ? ScreenState.error("Temperatura de hoje indisponível.") : ScreenState.success(temperatures);
        }, callback);
    }
}
