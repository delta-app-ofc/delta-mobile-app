package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AbnormalConsumptionData;
import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public final class AbnormalConsumptionRepository {
    private final AuthRepository auth;

    public AbnormalConsumptionRepository(AuthRepository auth) {
        this.auth = auth;
    }

    public void load(Integer requested, Consumer<ScreenState<AbnormalConsumptionData>> callback) {
        auth.resource(() -> {
            int user = auth.currentProfile().id;
            Response<List<UserPropertyResponse>> links = auth.api().listUserProperties(user).execute();
            if (!links.isSuccessful()) return ApiErrors.from(links);
            if (links.body() == null)
                return ScreenState.error("Não foi possível consultar seus imóveis.");
            Integer selected = null;
            for (UserPropertyResponse link : links.body())
                if (link != null && Objects.equals(user, link.userId) && link.propertyId != null && (requested == null || requested.equals(link.propertyId))) {
                selected = link.propertyId;
                break;
            }
            if (selected == null)
                return ScreenState.error("Selecione um imóvel associado à sua conta.");
            Response<List<ConsumptionDailyResponse>> response = auth.api().consumptionDaily(selected).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            if (response.body() == null)
                return ScreenState.error("Não foi possível consultar o histórico de consumo.");
            AbnormalConsumptionData data = AbnormalConsumptionData.from(selected, response.body(), LocalDate.now());
            if (!response.body().isEmpty() && data.readings.isEmpty())
                return ScreenState.error("Não foram retornadas leituras válidas para este imóvel.");
            return ScreenState.success(data);
        }, callback);
    }
}
