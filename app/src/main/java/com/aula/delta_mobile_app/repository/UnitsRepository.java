package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import retrofit2.Response;

public final class UnitsRepository {
    public static final class Creation {
        public Integer userId, addressId;
        public boolean addressUncertain, propertyUncertain;
    }

    private final ApiService api;
    public UnitsRepository(ApiService api) { this.api = api; }

    public ScreenState<List<UnitSummary>> load(LocalDate today) throws IOException {
        Response<Profile> me = api.me().execute();
        if (!me.isSuccessful()) return ApiErrors.from(me);
        if (me.body() == null || me.body().id == null || me.body().id <= 0) return ScreenState.error("Não foi possível confirmar sua conta.");
        Response<List<UserPropertyResponse>> links = api.listUserProperties(me.body().id).execute();
        if (!links.isSuccessful()) return ApiErrors.from(links);
        if (links.body() == null) return ScreenState.error("Não foi possível confirmar suas unidades.");
        List<UnitSummary> result = new ArrayList<>();
        for (UserPropertyResponse link : SectionData.properties(links.body(), me.body().id)) {
            BigDecimal liters = null;
            try {
                Response<List<ConsumptionDailyResponse>> readings = api.consumptionDaily(link.propertyId).execute();
                if (readings.isSuccessful() && readings.body() != null) {
                    List<ConsumptionDailyResponse> owned = new ArrayList<>();
                    for (ConsumptionDailyResponse reading : readings.body()) if (reading != null && Objects.equals(reading.propertyId, link.propertyId)) owned.add(reading);
                    liters = HomeDashboard.from(owned, today).monthLiters;
                } else if (readings.errorBody() != null) readings.errorBody().close();
            } catch (IOException unavailable) { }
            result.add(new UnitSummary(link, liters));
        }
        return ScreenState.success(result);
    }

    public ScreenState<PropertyResponse> create(UnitDraft draft, Creation progress) throws IOException {
        String error = draft.validation();
        if (error != null) return ScreenState.error(error);
        Response<Profile> me = api.me().execute();
        if (!me.isSuccessful()) return ApiErrors.from(me);
        if (me.body() == null || me.body().id == null || me.body().id <= 0) return ScreenState.error("Não foi possível confirmar sua conta.");
        if (progress.userId != null && !progress.userId.equals(me.body().id)) return ScreenState.error("A conta mudou. Reabra o cadastro.");
        progress.userId = me.body().id;
        if (progress.addressUncertain || progress.propertyUncertain) return ScreenState.error("O cadastro anterior não foi confirmado. Confira a lista de Unidades antes de iniciar outro cadastro.");
        if (progress.addressId == null) {
            progress.addressUncertain = true;
            Response<AddressResponse> address = api.createAddress(draft.address()).execute();
            if (!address.isSuccessful()) {
                if (address.code() < 500) progress.addressUncertain = false;
                return ApiErrors.from(address);
            }
            if (address.body() == null || address.body().id == null || address.body().id <= 0) return ScreenState.error("O endereço não foi confirmado. Confira suas unidades antes de tentar novamente.");
            progress.addressId = address.body().id;
            progress.addressUncertain = false;
        }
        progress.propertyUncertain = true;
        Response<PropertyResponse> property = api.createProperty(draft.property(progress.userId, progress.addressId)).execute();
        if (!property.isSuccessful()) {
            if (property.code() < 500) progress.propertyUncertain = false;
            return ApiErrors.from(property);
        }
        PropertyResponse created = property.body();
        if (created == null || created.id == null || created.id <= 0 || !Objects.equals(created.addressId, progress.addressId))
            return ScreenState.error("O servidor não confirmou a unidade. Confira a lista antes de tentar novamente.");
        Response<List<UserPropertyResponse>> links = api.listUserProperties(progress.userId).execute();
        if (!links.isSuccessful()) return ApiErrors.from(links);
        if (links.body() == null || SectionData.properties(links.body(), progress.userId).stream().noneMatch(link -> Objects.equals(link.propertyId, created.id)))
            return ScreenState.error("A unidade foi criada, mas o vínculo com sua conta não foi confirmado. Reabra Unidades para conferir.");
        progress.propertyUncertain = false;
        return ScreenState.success(created);
    }

    public ScreenState<Boolean> delete(int propertyId) throws IOException {
        Response<Profile> me = api.me().execute();
        if (!me.isSuccessful()) return ApiErrors.from(me);
        if (me.body() == null || me.body().id == null || me.body().id <= 0) return ScreenState.error("Não foi possível confirmar sua conta.");
        Response<List<UserPropertyResponse>> links = api.listUserProperties(me.body().id).execute();
        if (!links.isSuccessful()) return ApiErrors.from(links);
        if (links.body() == null || SectionData.properties(links.body(), me.body().id).stream().noneMatch(link -> Objects.equals(link.propertyId, propertyId)))
            return ScreenState.error("Esta unidade não está associada à sua conta.");
        Response<Void> response = api.deleteProperty(propertyId).execute();
        if (!response.isSuccessful()) return ApiErrors.from(response);
        return ScreenState.success(true);
    }
}
