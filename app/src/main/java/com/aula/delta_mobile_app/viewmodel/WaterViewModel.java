package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;

public class WaterViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<List<UserPropertyResponse>>> properties = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<List<ConsumptionDailyResponse>>> consumption = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<DeviceResponse>> meter = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<LastWaterBillResponse>> bill = new MutableLiveData<>(ScreenState.idle());
    public Integer propertyId;
    private final AuthRepository auth;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int epoch, selection;

    public WaterViewModel(Application application) {
        super(application);
        auth = AuthRepository.getInstance(application);
    }

    private static <T> ScreenState<T> result(Response<T> response) {
        if (!response.isSuccessful()) return ApiErrors.from(response);
        return response.body() == null ? ScreenState.error("O servidor não confirmou os dados.") : ScreenState.success(response.body());
    }

    public void loadProperties() {
        if (properties.getValue().busy()) return;
        int attempt = epoch;
        properties.setValue(ScreenState.loading());
        auth.resource(() -> result(auth.api().listUserProperties(auth.currentProfile().id).execute()), value -> main.post(() -> {
            if (attempt == epoch) properties.setValue(value);
        }));
    }

    public void select(Integer id) {
        propertyId = id;
        int request = ++selection, attempt = epoch;
        consumption.setValue(ScreenState.loading());
        auth.resource(() -> result(auth.api().consumptionDaily(id).execute()), value -> main.post(() -> {
            if (attempt == epoch && request == selection)
                consumption.setValue(value);
        }));
    }

    public void registerMeter(String code) {
        ScreenState<DeviceResponse> state = meter.getValue();
        if (state.busy() || System.currentTimeMillis() < state.retryAt) return;
        if (propertyId == null) {
            meter.setValue(ScreenState.error("Selecione um imóvel na tela inicial."));
            return;
        }
        if (code == null || code.trim().isEmpty()) {
            meter.setValue(ScreenState.error("Informe o código do medidor."));
            return;
        }
        final int selected = propertyId, attempt = epoch;
        meter.setValue(ScreenState.loading());
        auth.resource(() -> {
            Response<List<UserPropertyResponse>> links = auth.api().listUserProperties(auth.currentProfile().id).execute();
            if (!links.isSuccessful()) return ApiErrors.from(links);
            if (links.body() == null || links.body().stream().noneMatch(link -> Integer.valueOf(selected).equals(link.propertyId)))
                return ScreenState.error("O imóvel não está associado à sua conta.");
            Response<List<DeviceResponse>> devices = auth.api().listDevices().execute();
            if (!devices.isSuccessful()) return ApiErrors.from(devices);
            if (devices.body() == null)
                return ScreenState.error("Não foi possível consultar os medidores.");
            for (DeviceResponse device : devices.body())
                if (code.equals(device.deviceId)) {
                if (!Integer.valueOf(selected).equals(device.propertyId))
                    return ScreenState.error("Este medidor já está vinculado a outro imóvel.");
                if (!Boolean.TRUE.equals(device.isActive))
                    return ScreenState.error("Este medidor está inativo.");
                return ScreenState.success(device);
            }
            CreateDeviceRequest body = new CreateDeviceRequest();
            body.deviceId = code;
            body.propertyId = selected;
            body.isActive = true;
            return result(auth.api().createDevice(body).execute());
        }, value -> main.post(() -> {
            if (attempt == epoch) meter.setValue(value);
        }));
    }

    public void saveBill(String month, String paid, String volume) {
        ScreenState<LastWaterBillResponse> state = bill.getValue();
        if (state.busy() || System.currentTimeMillis() < state.retryAt) return;
        final CreateLastWaterBillRequest body = new CreateLastWaterBillRequest();
        try {
            body.month = java.time.LocalDate.parse(month).withDayOfMonth(1).toString();
            body.totalValue = new BigDecimal(paid.trim().replace(',', '.'));
            body.m3Value = new BigDecimal(volume.trim().replace(',', '.'));
            if (body.totalValue.signum() < 0 || body.m3Value.signum() < 0)
                throw new IllegalArgumentException();
        } catch (RuntimeException invalid) {
            bill.setValue(ScreenState.error("Informe valor e consumo válidos, maiores ou iguais a zero."));
            return;
        }
        int attempt = epoch;
        bill.setValue(ScreenState.loading());
        auth.resource(() -> {
            body.userId = auth.currentProfile().id;

            Response<List<LastWaterBillResponse>> existing = auth.api().listLastWaterBills().execute();
            if (!existing.isSuccessful()) return ApiErrors.from(existing);
            if (existing.body() == null)
                return ScreenState.error("Não foi possível consultar as contas.");
            for (LastWaterBillResponse item : existing.body())
                if (body.userId.equals(item.userId) && body.month.equals(item.month)) {
                UpdateLastWaterBillRequest update = new UpdateLastWaterBillRequest();
                update.userId = body.userId;
                update.month = body.month;
                update.totalValue = body.totalValue;
                update.m3Value = body.m3Value;
                return result(auth.api().updateLastWaterBill(item.id, update).execute());
            }
            return result(auth.api().createLastWaterBill(body).execute());
        }, value -> main.post(() -> {
            if (attempt == epoch) bill.setValue(value);
        }));
    }

    public void consumeMeter() {
        meter.setValue(ScreenState.idle());
    }

    public void consumeBill() {
        bill.setValue(ScreenState.idle());
    }

    public void reset() {
        epoch++;
        selection++;
        propertyId = null;
        properties.setValue(ScreenState.idle());
        consumption.setValue(ScreenState.idle());
        meter.setValue(ScreenState.idle());
        bill.setValue(ScreenState.idle());
    }
}
