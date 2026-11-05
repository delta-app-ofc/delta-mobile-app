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

            var api=com.aula.delta_mobile_app.services.network.RetrofitClient.createMongoService(auth);
            List<com.aula.delta_mobile_app.model.AlertModels.Alert> alerts=new ArrayList<>();
            for(int page=0;page<100;page++) {
                var batch=api.alerts(null,page,100).execute();
                if(!batch.isSuccessful())return ApiErrors.from(batch);
                if(batch.body()==null || batch.body().content==null || batch.body().page==null)return ScreenState.error("Resposta de alertas invalida.");
                alerts.addAll(batch.body().content);
                if(page+1>=batch.body().page.totalPages)break;
                if(page==99)return ScreenState.error("Historico muito extenso; consulte os alertas por filtro.");
            }
            List<LeakMonitoringData.Event> events=new ArrayList<>();
            var end=java.time.Instant.now();var start=end.minusSeconds(86400);
            for(DeviceResponse device:LeakMonitoringData.activeDevices(selected.propertyId,devices.body())) {
                List<LeakMonitoringData.Sample> samples=new ArrayList<>();
                java.math.BigDecimal latest=null;
                for(int page=0;page<10;page++) {
                    var batch=api.evidence(device.deviceId,start.toString(),end.toString(),page,100).execute();
                    if(!batch.isSuccessful())return ApiErrors.from(batch);
                    if(batch.body()==null || batch.body().content==null || batch.body().page==null)return ScreenState.error("Evidencias indisponiveis.");
                    for(var window:batch.body().content) {
                        if(window.start!=null && window.lpm!=null) {
                            samples.add(new LeakMonitoringData.Sample(java.time.Instant.parse(window.start).toEpochMilli(),java.math.BigDecimal.valueOf(window.lpm)));
                        }
                    }
                    if(page+1>=batch.body().page.totalPages)break;
                    if(page==9)return ScreenState.error("Serie de evidencias acima do limite.");
                }
                samples=LeakMonitoringData.orderedSamples(samples);
                if(!samples.isEmpty())latest=samples.get(samples.size()-1).litersPerMinute;
                for(var alert:alerts) {
                    if(!device.deviceId.equals(alert.deviceId) || !"detector".equals(alert.source) || alert.resolvedAt!=null ||
                        !("vazamento_continuo".equals(alert.type) || "consumo_noturno".equals(alert.type)))continue;
                    long triggered=alert.triggeredAt==null?start.toEpochMilli():java.time.Instant.parse(alert.triggeredAt).toEpochMilli();
                    events.add(new LeakMonitoringData.Event(alert.id,selected.propertyId,device.deviceId,LeakMonitoringData.Status.ACTIVE,
                        latest,null,null,Math.max(start.toEpochMilli(),triggered),end.toEpochMilli(),null,samples));
                    // Duas regras no mesmo medidor nao representam dois pontos fisicos de vazamento.
                    break;
                }
            }
            return ScreenState.success(LeakMonitoringData.monitored(selected.propertyId,selected.propertyName,devices.body(),events));
        }, callback);
    }
}
