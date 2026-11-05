package com.aula.delta_mobile_app.repository;
import com.aula.delta_mobile_app.model.AlertModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.MongoApiService;
import com.aula.delta_mobile_app.services.network.*;
import java.util.function.Consumer;
public final class AlertsRepository {
    private final AuthRepository auth;
    private final MongoApiService api;
    public AlertsRepository(AuthRepository auth) { this.auth=auth; api=RetrofitClient.createMongoService(auth); }
    public void load(String severity, int page, Consumer<ScreenState<Page<Alert>>> callback) {
        auth.resource(() -> {
            retrofit2.Response<Page<Alert>> response=api.alerts(severity,page,20).execute();
            if (!response.isSuccessful()) return ApiErrors.from(response);
            if (response.body()==null || response.body().content==null || response.body().page==null) return ScreenState.error("Resposta de alertas inválida.");
            return ScreenState.success(response.body());
        },callback);
    }
    public void dismiss(String id, Consumer<ScreenState<Boolean>> callback) {
        auth.resource(() -> {
            if (id==null || !id.matches("[a-fA-F0-9]{24}")) return ScreenState.error("Alerta inválido.");
            retrofit2.Response<Void> response=api.dismiss(id).execute();
            return response.isSuccessful() ? ScreenState.success(true) : ApiErrors.from(response);
        }, callback);
    }
}
