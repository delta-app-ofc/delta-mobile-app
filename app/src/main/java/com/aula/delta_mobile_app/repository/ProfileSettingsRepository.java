package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ProfileSettingsData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import retrofit2.Response;

public final class ProfileSettingsRepository {
    private final AuthRepository auth;

    public ProfileSettingsRepository(AuthRepository auth) { this.auth = auth; }

    public void profile(Consumer<ScreenState<Profile>> callback) {
        auth.resource(() -> {
            Response<Profile> response = auth.api().me().execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            Profile value = response.body();
            return value == null || value.id == null || value.id <= 0 ? ScreenState.error("Não foi possível confirmar seu perfil.") : ScreenState.success(value);
        }, callback);
    }

    public void saveProfile(Profile original, UpdateProfileRequest request, Consumer<ScreenState<Profile>> callback) {
        auth.resource(() -> {
            Response<Profile> loaded = auth.api().me().execute();
            if (!loaded.isSuccessful()) return ApiErrors.from(loaded);
            Profile current = loaded.body();
            if (current == null) return ScreenState.error("Não foi possível confirmar seu perfil.");
            if (!Objects.equals(current.id, original.id) || !Objects.equals(current.name, original.name) || !Objects.equals(current.email, original.email)
                || !Objects.equals(current.phone, original.phone) || !Objects.equals(current.birthDate, original.birthDate))
                return ScreenState.error("O perfil mudou em outra sessão. Cancele a edição e atualize os dados.");
            Response<Profile> response = auth.api().updateProfile(request).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            Profile value = response.body();
            if (value == null || !Objects.equals(value.id, original.id) || !Objects.equals(value.name, request.name) || !Objects.equals(value.email, request.email)
                || !Objects.equals(value.phone, request.phone) || !Objects.equals(value.birthDate, request.birthDate))
                return ScreenState.error("O servidor não confirmou todas as alterações. Atualize o perfil para conferir.");
            return ScreenState.success(value);
        }, callback);
    }

    public void residence(Integer propertyId, Consumer<ScreenState<ProfileSettingsData>> callback) {
        auth.resource(() -> {
            if (propertyId == null) return ScreenState.error("Selecione um imóvel em Unidades para consultar o endereço.");
            ScreenState<PropertyResponse> selected = ownedProperty(propertyId);
            if (selected.status != ScreenState.Status.SUCCESS) return ScreenState.error(selected.message, selected.code, selected.retryAt);
            if (selected.data.addressId == null) return ScreenState.error("Este imóvel não possui endereço cadastrado.");
            Response<AddressResponse> response = auth.api().getAddress(selected.data.addressId).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            AddressResponse address = response.body();
            if (address == null || !Objects.equals(address.id, selected.data.addressId)) return ScreenState.error("Não foi possível confirmar o endereço deste imóvel.");
            return ScreenState.success(new ProfileSettingsData(selected.data, address));
        }, callback);
    }

    public void sensors(Integer propertyId, Consumer<ScreenState<List<DeviceResponse>>> callback) {
        auth.resource(() -> {
            if (propertyId == null) return ScreenState.error("Selecione um imóvel em Unidades para consultar os sensores.");
            ScreenState<PropertyResponse> selected = ownedProperty(propertyId);
            if (selected.status != ScreenState.Status.SUCCESS) return ScreenState.error(selected.message, selected.code, selected.retryAt);
            Response<List<DeviceResponse>> response = auth.api().listDevices().execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            if (response.body() == null) return ScreenState.error("O servidor não confirmou a lista de sensores.");
            List<DeviceResponse> devices = new java.util.ArrayList<>();
            for (DeviceResponse device : response.body()) if (device != null && Objects.equals(device.propertyId, propertyId)) devices.add(device);
            return ScreenState.success(devices);
        }, callback);
    }

    public void save(ProfileSettingsData original, UpdateAddressRequest request, Consumer<ScreenState<ProfileSettingsData>> callback) {
        auth.resource(() -> {
            ScreenState<PropertyResponse> selected = ownedProperty(original.property.id);
            if (selected.status != ScreenState.Status.SUCCESS) return ScreenState.error(selected.message, selected.code, selected.retryAt);
            if (!Objects.equals(selected.data.addressId, original.address.id)) return ScreenState.error("O endereço do imóvel mudou. Atualize os dados antes de editar.");
            Response<AddressResponse> current = auth.api().getAddress(original.address.id).execute();
            if (!current.isSuccessful()) return ApiErrors.from(current);
            AddressResponse value = current.body();
            if (value == null || !sameAddress(original.address, value)) return ScreenState.error("O endereço foi atualizado em outra sessão. Cancele a edição e atualize os dados.");
            Response<AddressResponse> response = auth.api().updateAddress(original.address.id, request).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            AddressResponse saved = response.body();
            if (saved == null || !Objects.equals(saved.id, original.address.id) || !Objects.equals(saved.cep, request.cep)
                || !Objects.equals(saved.city, request.city) || !Objects.equals(saved.state, request.state)
                || !sameNumber(saved.latitude, request.latitude) || !sameNumber(saved.longitude, request.longitude))
                return ScreenState.error("O servidor não confirmou todas as alterações. Atualize o endereço para conferir.");
            return ScreenState.success(new ProfileSettingsData(selected.data, saved));
        }, callback);
    }

    private ScreenState<PropertyResponse> ownedProperty(int propertyId) throws java.io.IOException {
        int userId = auth.currentProfile().id;
        Response<List<UserPropertyResponse>> links = auth.api().listUserProperties(userId).execute();
        if (!links.isSuccessful()) return ApiErrors.from(links);
        if (links.body() == null || links.body().stream().noneMatch(link -> link != null && Objects.equals(link.userId, userId) && Objects.equals(link.propertyId, propertyId)))
            return ScreenState.error("Este imóvel não está associado à sua conta.");
        Response<PropertyResponse> response = auth.api().getProperty(propertyId).execute();
        if (!response.isSuccessful()) return ApiErrors.from(response);
        return response.body() == null || !Objects.equals(response.body().id, propertyId) ? ScreenState.error("Imóvel indisponível.") : ScreenState.success(response.body());
    }

    private boolean sameAddress(AddressResponse a, AddressResponse b) {
        return Objects.equals(a.cep, b.cep) && Objects.equals(a.city, b.city) && Objects.equals(a.state, b.state)
            && Objects.equals(a.regionId, b.regionId) && sameNumber(a.latitude, b.latitude) && sameNumber(a.longitude, b.longitude);
    }

    private boolean sameNumber(java.math.BigDecimal a, java.math.BigDecimal b) { return a == null ? b == null : b != null && a.compareTo(b) == 0; }
}
