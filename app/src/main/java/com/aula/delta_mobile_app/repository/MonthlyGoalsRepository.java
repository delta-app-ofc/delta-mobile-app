package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.MonthlyGoalContext;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.model.SectionData;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import retrofit2.Response;

public final class MonthlyGoalsRepository {
    private final ApiService api;
    private final MonthlyGoalStorage storage;
    private final Clock clock;

    public MonthlyGoalsRepository(ApiService api, MonthlyGoalStorage storage) { this(api, storage, Clock.systemDefaultZone()); }

    public MonthlyGoalsRepository(ApiService api, MonthlyGoalStorage storage, Clock clock) { this.api = api; this.storage = storage; this.clock = clock; }

    public ScreenState<MonthlyGoalContext> load(Integer selected, YearMonth month) throws IOException {
        if (selected == null) return ScreenState.error("Selecione a residência atual na tela inicial para definir sua meta.");
        Response<Profile> profile = api.me().execute();
        if (!profile.isSuccessful()) return ApiErrors.from(profile);
        if (profile.body() == null || profile.body().id == null || profile.body().id <= 0) return ScreenState.error("Não foi possível confirmar sua conta.");
        int user = profile.body().id;
        Response<List<UserPropertyResponse>> response = api.listUserProperties(user).execute();
        if (!response.isSuccessful()) return ApiErrors.from(response);
        if (response.body() == null) return ScreenState.error("Não foi possível confirmar seus imóveis.");
        List<UserPropertyResponse> properties = SectionData.properties(response.body(), user);
        if (properties.isEmpty()) return ScreenState.error("Associe um imóvel à sua conta para definir uma meta.");
        Integer propertyId = selected;
        for (UserPropertyResponse property : properties) if (Objects.equals(property.propertyId, propertyId)) {
            try {
                BigDecimal liters = storage.read(user, propertyId, month);
                YearMonth currentMonth = YearMonth.now(clock);
                BigDecimal currentMonthLiters = month.isAfter(currentMonth) ? storage.read(user, propertyId, currentMonth) : null;
                return ScreenState.success(new MonthlyGoalContext(user, property, properties, month, liters, currentMonthLiters));
            } catch (RuntimeException failure) { return ScreenState.error("Não foi possível ler a meta salva neste aparelho."); }
        }
        return ScreenState.error("Este imóvel não está associado à sua conta.");
    }

    public ScreenState<MonthlyGoalContext> save(MonthlyGoalContext original, String input) throws IOException {
        if (original.month.isBefore(YearMonth.now(clock))) return ScreenState.error("As metas dos meses passados são apenas para consulta.");
        ScreenState<MonthlyGoalContext> current = load(original.property.propertyId, original.month);
        if (current.status != ScreenState.Status.SUCCESS) return current;
        if (current.data.userId != original.userId) return ScreenState.error("A conta mudou. Reabra a tela de metas.");
        BigDecimal previous = current.data.liters;
        if (previous == null ? original.liters != null : original.liters == null || previous.compareTo(original.liters) != 0)
            return ScreenState.error("A meta mudou em outra tela. Cancele a edição e atualize os dados.");
        try {
            if (original.month.isBefore(YearMonth.now(clock))) return ScreenState.error("As metas dos meses passados são apenas para consulta.");
            BigDecimal saved = storage.save(original.userId, original.property.propertyId, original.month, input);
            return ScreenState.success(new MonthlyGoalContext(original.userId, current.data.property, current.data.properties, original.month, saved, current.data.currentMonthLiters));
        } catch (IOException | RuntimeException failure) { return ScreenState.error("Não foi possível salvar e confirmar a meta neste aparelho. Tente novamente."); }
    }
}
