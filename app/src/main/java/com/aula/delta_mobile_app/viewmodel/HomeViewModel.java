package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.HomeDashboard;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.HomeRepository;

import java.time.LocalDate;
import java.util.*;

public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<String> name = new MutableLiveData<>("");
    private final MutableLiveData<HomeDashboard> dashboard = new MutableLiveData<>();
    public final MutableLiveData<ScreenState<com.aula.delta_mobile_app.model.AuthModels.Profile>>
    user = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<WaterCostResponse>> cost = new MutableLiveData<>(ScreenState.idle());
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HomeRepository repository;
    private Integer propertyId;
    private int selection, costRequest;

    public HomeViewModel(Application app) {
        super(app);
        repository = new HomeRepository(AuthRepository.getInstance(app));
    }

    public LiveData<String> name() {
        return name;
    }

    public LiveData<HomeDashboard> dashboard() {
        return dashboard;
    }

    public void selectProperty(Integer id) {
        if (Objects.equals(id, propertyId)) return;
        propertyId = id;
        selection++;
        costRequest++;
        cost.setValue(ScreenState.idle());
        dashboard.setValue(HomeDashboard.unavailable());
    }

    public void clearReadings() {
        costRequest++;
        cost.setValue(ScreenState.idle());
        dashboard.setValue(HomeDashboard.unavailable());
    }

    public void update(List<ConsumptionDailyResponse> readings) {
        List<ConsumptionDailyResponse> selected = new ArrayList<>();
        for (ConsumptionDailyResponse reading : readings)
            if (reading != null && Objects.equals(propertyId, reading.propertyId))
                selected.add(reading);
        HomeDashboard summary = HomeDashboard.from(selected, LocalDate.now());
        dashboard.setValue(summary);
        costRequest++;
        if (propertyId == null || summary.monthLiters == null) {
            cost.setValue(ScreenState.idle());
            return;
        }
        if (summary.monthLiters.signum() == 0) {
            WaterCostResponse zero = new WaterCostResponse();
            zero.propertyId = propertyId;
            zero.consumptionM3 = java.math.BigDecimal.ZERO;
            zero.estimatedCost = java.math.BigDecimal.ZERO;
            zero.referenceDate = LocalDate.now().toString();
            cost.setValue(ScreenState.success(zero));
            return;
        }
        int attempt = selection, request = costRequest;
        cost.setValue(ScreenState.loading());
        repository.waterCost(propertyId, summary.monthLiters, LocalDate.now().toString(), value -> main.post(() -> {
            if (attempt == selection && request == costRequest)
                cost.setValue(value);
        }));
    }

    public void loadProfile() {
        if (user.getValue().busy()) return;
        user.setValue(ScreenState.loading());
        repository.me(value -> main.post(() -> {
            user.setValue(value);
            if (value.status == ScreenState.Status.SUCCESS)
                name.setValue(value.data.name);
        }));
    }
}
