package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.RankingRepository;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.util.*;

public class SectionsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<List<LastWaterBillResponse>>> bills = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<List<PropertyRankingResponse>>> rankings = new MutableLiveData<>(ScreenState.idle());
    private final AuthRepository auth;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final RankingRepository rankingRepository;
    private int billsRequest, rankingsRequest;
    private boolean cleared;

    public SectionsViewModel(Application app) {
        super(app);
        auth = AuthRepository.getInstance(app);
        rankingRepository = new RankingRepository(auth.api());
    }

    public void loadRankings() {
        ScreenState<List<PropertyRankingResponse>> state = rankings.getValue();
        if (state.busy() || System.currentTimeMillis() < state.retryAt || cleared) return;
        int request = ++rankingsRequest;
        long generation = auth.sessionGeneration();
        rankings.setValue(ScreenState.loading());
        auth.resource(rankingRepository::load, value -> main.post(() -> {
            if (!cleared && request == rankingsRequest && generation == auth.sessionGeneration() && auth.hasSession()) rankings.setValue(value);
        }));
    }

    public void loadBills() {
        if (bills.getValue().busy()) return;
        int request = ++billsRequest;
        bills.setValue(ScreenState.loading());
        auth.resource(() -> {
            Integer user = auth.currentProfile().id;
            Response<List<LastWaterBillResponse>> response = auth.api().listLastWaterBills().execute();
            if (!response.isSuccessful())
                return ApiErrors.<List<LastWaterBillResponse>>from(response);
            if (response.body() == null)
                return ScreenState.<List<LastWaterBillResponse>>error("O servidor não retornou as contas.");
            return ScreenState.success(com.aula.delta_mobile_app.model.SectionData.bills(response.body(), user));
        }, value -> main.post(() -> {
            if (!cleared && request == billsRequest) bills.setValue(value);
        }));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        rankingsRequest++;
        billsRequest++;
        main.removeCallbacksAndMessages(null);
    }
}
