package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.PropertyRankingResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.model.SectionData;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Response;

public final class RankingRepository {
    private final ApiService api;

    public RankingRepository(ApiService api) { this.api = api; }

    public ScreenState<List<PropertyRankingResponse>> load() throws IOException {
        Response<Profile> profile = api.me().execute();
        if (!profile.isSuccessful()) return ApiErrors.from(profile);
        if (profile.body() == null || profile.body().id == null || profile.body().id <= 0)
            return ScreenState.error("Não foi possível confirmar sua conta para consultar o ranking.");
        Integer user = profile.body().id;
        Response<List<UserPropertyResponse>> links = api.listUserProperties(user).execute();
        if (!links.isSuccessful()) return ApiErrors.from(links);
        if (links.body() == null) return ScreenState.error("O servidor não confirmou suas unidades.");
        Set<Integer> owned = new HashSet<>();
        for (UserPropertyResponse link : SectionData.properties(links.body(), user)) owned.add(link.propertyId);
        if (owned.isEmpty()) return ScreenState.success(Collections.emptyList());
        Response<List<PropertyRankingResponse>> response = api.propertyRanking().execute();
        if (!response.isSuccessful()) return ApiErrors.from(response);
        if (response.body() == null) return ScreenState.error("O servidor não confirmou o ranking.");
        return ScreenState.success(SectionData.rankings(response.body(), owned));
    }
}
