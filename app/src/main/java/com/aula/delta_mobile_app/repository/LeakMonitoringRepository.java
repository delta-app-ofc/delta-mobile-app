package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.LeakMonitoringData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.util.*;
import java.util.function.Consumer;

public final class LeakMonitoringRepository {
    private final AuthRepository auth;

    public LeakMonitoringRepository(AuthRepository auth) {
        this.auth = auth;
    }

    public void load(Integer requestedProperty, Consumer<ScreenState<LeakMonitoringData>> callback) {
        auth.resource(() -> {
            int userId = auth.currentProfile().id;
            Response<List<UserPropertyResponse>> response = auth.api().listUserProperties(userId).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            if (response.body() == null)
                return ScreenState.error("Não foi possível consultar seus imóveis.");
            UserPropertyResponse selected = null;
            for (UserPropertyResponse link : response.body()) {
                if (link == null || link.propertyId == null || link.userId == null || link.userId != userId) continue;
                if (requestedProperty == null) {
                    selected = link;
                    break;
                }
                if (requestedProperty.equals(link.propertyId)) {
                    selected = link;
                    break;
                }
            }
            if (selected == null && requestedProperty != null)
                return ScreenState.error("Este imóvel não está associado à sua conta.");
            if (selected == null)
                return ScreenState.success(LeakMonitoringData.unavailable(null, null, Collections.emptyList()));
            Response<List<DeviceResponse>> devices = auth.api().listDevices().execute();
            if (!devices.isSuccessful()) return ApiErrors.from(devices);
            if (devices.body() == null)
                return ScreenState.error("Não foi possível consultar os medidores do imóvel.");

            return ScreenState.success(LeakMonitoringData.unavailable(selected.propertyId, selected.propertyName, devices.body()));
        }, callback);
    }
}
