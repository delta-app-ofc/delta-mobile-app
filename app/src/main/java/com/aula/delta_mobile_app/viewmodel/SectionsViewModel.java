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

import java.util.*;

public class SectionsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<List<LastWaterBillResponse>>> bills = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<List<PropertyRankingResponse>>> rankings = new MutableLiveData<>(ScreenState.idle());
    private final AuthRepository auth;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int billsRequest;

    public SectionsViewModel(Application app) {
        super(app);
        auth = AuthRepository.getInstance(app);
    }

    public void loadRankings() {
        if (rankings.getValue().busy()) return;
        rankings.setValue(ScreenState.loading());
        auth.resource(() -> {
            Integer user = auth.currentProfile().id;
            Response<List<UserPropertyResponse>> links = auth.api().listUserProperties(user).execute();
            if (!links.isSuccessful())
                return ApiErrors.<List<PropertyRankingResponse>>from(links);
            if (links.body() == null)
                return ScreenState.<List<PropertyRankingResponse>>error("Não foi possível consultar suas unidades.");
            Set<Integer> owned = new HashSet<>();
            for (UserPropertyResponse link : links.body())
                if (link != null && user.equals(link.userId) && link.propertyId != null)
                    owned.add(link.propertyId);
            Response<List<PropertyRankingResponse>> response = auth.api().propertyRanking().execute();
            if (!response.isSuccessful())
                return ApiErrors.<List<PropertyRankingResponse>>from(response);
            if (response.body() == null)
                return ScreenState.<List<PropertyRankingResponse>>error("O servidor não retornou o ranking.");
            return ScreenState.success(com.aula.delta_mobile_app.model.SectionData.rankings(response.body(), owned));
        }, value -> main.post(() -> rankings.setValue(value)));
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
            if (request == billsRequest) bills.setValue(value);
        }));
    }

}
