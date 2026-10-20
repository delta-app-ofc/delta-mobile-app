package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.SignupOptions;
import com.aula.delta_mobile_app.services.network.ApiErrors;

import retrofit2.Response;

import java.util.*;

public class SectionsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<List<LastWaterBillResponse>>> bills = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<List<UserHabitResponse>>> habits = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Boolean>> mutation = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<List<PropertyRankingResponse>>> rankings = new MutableLiveData<>(ScreenState.idle());
    private final AuthRepository auth;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int habitsRequest, billsRequest;

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

    public void loadHabits() {
        if (habits.getValue().busy()) return;
        int request = ++habitsRequest;
        habits.setValue(ScreenState.loading());
        auth.resource(() -> {
            Integer user = auth.currentProfile().id;
            Response<List<UserHabitResponse>> response = auth.api().listUserHabits(user).execute();
            if (!response.isSuccessful())
                return ApiErrors.<List<UserHabitResponse>>from(response);
            if (response.body() == null)
                return ScreenState.<List<UserHabitResponse>>error("O servidor não retornou os hábitos.");
            return ScreenState.success(com.aula.delta_mobile_app.model.SectionData.habits(response.body(), user));
        }, value -> main.post(() -> {
            if (request == habitsRequest) habits.setValue(value);
        }));
    }

    public void saveHabit(Integer id, int habitId, int frequency, List<Integer> days) {
        if (mutation.getValue().busy() || System.currentTimeMillis() < mutation.getValue().retryAt)
            return;
        SignupOptions catalog = SignupOptions.load(getApplication());
        Set<Integer> validDays = new HashSet<>();
        for (SignupOptions.Entry day : catalog.days) validDays.add(day.id);
        if (frequency < 1 || days == null || days.isEmpty() || !validDays.containsAll(days) || catalog.habits.stream().noneMatch(item -> item.id == habitId)) {
            mutation.setValue(ScreenState.error("Selecione um hábito, uma frequência positiva e os dias da semana."));
            return;
        }
        List<Integer> selectedDays = new ArrayList<>(new LinkedHashSet<>(days));
        mutation.setValue(ScreenState.loading());
        auth.resource(() -> {
            Integer user = auth.currentProfile().id;
            if (id != null) {
                Response<UserHabitResponse> existing = auth.api().getUserHabit(user, id).execute();
                if (!existing.isSuccessful()) return ApiErrors.<Boolean>from(existing);
                if (existing.body() == null || !user.equals(existing.body().userId))
                    return ScreenState.error("Este hábito não pertence à sua conta.");
                UpdateUserHabitRequest body = new UpdateUserHabitRequest();
                body.habitId = habitId;
                body.frequency = frequency;
                body.daysOfWeek = selectedDays;
                Response<UserHabitResponse> response = auth.api().updateUserHabit(user, id, body).execute();
                if (!response.isSuccessful()) return ApiErrors.<Boolean>from(response);
                return response.body() == null ? ScreenState.error("O servidor não confirmou a alteração.") : ScreenState.success(true);
            }
            CreateUserHabitRequest body = new CreateUserHabitRequest();
            body.habitId = habitId;
            body.frequency = frequency;
            body.daysOfWeek = selectedDays;
            Response<UserHabitResponse> response = auth.api().createUserHabit(user, body).execute();
            if (!response.isSuccessful()) return ApiErrors.<Boolean>from(response);
            return response.body() == null ? ScreenState.error("O servidor não confirmou o cadastro.") : ScreenState.success(true);
        }, value -> main.post(() -> {
            mutation.setValue(value);
            if (value.status == ScreenState.Status.SUCCESS) loadHabits();
        }));
    }

    public void deleteHabit(int id) {
        if (mutation.getValue().busy() || System.currentTimeMillis() < mutation.getValue().retryAt)
            return;
        mutation.setValue(ScreenState.loading());
        auth.resource(() -> {
            Integer user = auth.currentProfile().id;
            Response<UserHabitResponse> existing = auth.api().getUserHabit(user, id).execute();
            if (!existing.isSuccessful()) return ApiErrors.<Boolean>from(existing);
            if (existing.body() == null || !user.equals(existing.body().userId))
                return ScreenState.error("Este hábito não pertence à sua conta.");
            Response<Void> response = auth.api().deleteUserHabit(user, id).execute();
            return response.isSuccessful() ? ScreenState.success(true) : ApiErrors.<Boolean>from(response);
        }, value -> main.post(() -> {
            mutation.setValue(value);
            if (value.status == ScreenState.Status.SUCCESS) loadHabits();
        }));
    }
}
