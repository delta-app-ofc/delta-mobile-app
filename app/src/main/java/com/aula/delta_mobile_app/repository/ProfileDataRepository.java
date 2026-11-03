package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import java.io.IOException;
import retrofit2.Response;

public final class ProfileDataRepository {
    private final ApiService api;
    public ProfileDataRepository(ApiService api) { this.api = api; }

    public ScreenState<Profile> load() throws IOException {
        Response<Profile> response = api.me().execute();
        if (!response.isSuccessful()) return ApiErrors.from(response);
        Profile value = response.body();
        if (value == null || value.id == null || value.id <= 0 || value.name == null || value.name.trim().isEmpty() || value.email == null || value.email.trim().isEmpty())
            return ScreenState.error("O servidor não retornou os dados completos do seu perfil. Tente novamente.");
        return ScreenState.success(value);
    }
}
