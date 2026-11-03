package com.aula.delta_mobile_app.services.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoApiService {
    @GET("v1/forecast")
    Call<JsonObject> forecast(@Query("latitude") double latitude, @Query("longitude") double longitude, @Query("hourly") String hourly,
        @Query("forecast_days") int forecastDays, @Query("timezone") String timezone);
}
