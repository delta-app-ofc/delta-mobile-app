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
    private java.math.BigDecimal costLiters;
    private LocalDate costDate;

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
        costLiters = null;
        costDate = null;
        cost.setValue(ScreenState.idle());
        dashboard.setValue(HomeDashboard.unavailable());
    }

    public void clearReadings() {
        costRequest++;
        costLiters = null;
        costDate = null;
        cost.setValue(ScreenState.idle());
        dashboard.setValue(HomeDashboard.unavailable());
    }

    public void update(List<ConsumptionDailyResponse> readings) {
        List<ConsumptionDailyResponse> selected = new ArrayList<>();
        for (ConsumptionDailyResponse reading : readings)
            if (reading != null && Objects.equals(propertyId, reading.propertyId))
                selected.add(reading);
        LocalDate today = LocalDate.now();
        HomeDashboard summary = HomeDashboard.from(selected, today);
        dashboard.setValue(summary);
        ScreenState<WaterCostResponse> currentCost = cost.getValue();
        if (costLiters != null && summary.monthLiters != null && costLiters.compareTo(summary.monthLiters) == 0 && today.equals(costDate)
            && (currentCost.busy() || currentCost.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < currentCost.retryAt)) return;
        costRequest++;
        if (propertyId == null || summary.monthLiters == null) {
            cost.setValue(ScreenState.idle());
            return;
        }
        costLiters = summary.monthLiters;
        costDate = today;
        if (summary.monthLiters.signum() == 0) {
            WaterCostResponse zero = new WaterCostResponse();
            zero.propertyId = propertyId;
            zero.consumptionM3 = java.math.BigDecimal.ZERO;
            zero.estimatedCost = java.math.BigDecimal.ZERO;
            zero.referenceDate = today.toString();
            cost.setValue(ScreenState.success(zero));
            return;
        }
        int attempt = selection, request = costRequest;
        cost.setValue(ScreenState.loading());
        repository.waterCost(propertyId, summary.monthLiters, today.toString(), value -> main.post(() -> {
            if (attempt == selection && request == costRequest)
                cost.setValue(value);
        }));
    }

    public void loadProfile() {
        loadProfile(true);
    }

    public void loadProfile(boolean force) {
        if (user.getValue().busy() || (!force && user.getValue().status == ScreenState.Status.SUCCESS)) return;
        user.setValue(ScreenState.loading());
        repository.me(value -> main.post(() -> {
            user.setValue(value);
            if (value.status == ScreenState.Status.SUCCESS)
                name.setValue(value.data.name);
        }));
    }
}
