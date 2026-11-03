package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import com.aula.delta_mobile_app.model.MonthlyGoal;
import com.aula.delta_mobile_app.model.MonthlyGoalContext;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.MonthlyGoalStorage;
import com.aula.delta_mobile_app.repository.MonthlyGoalsRepository;
import com.aula.delta_mobile_app.repository.SQLiteGoalStore;
import java.time.YearMonth;

public final class MonthlyGoalsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<MonthlyGoalContext>> state = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Boolean>> saving = new MutableLiveData<>(ScreenState.idle());
    public YearMonth month = YearMonth.now();
    public String draft;
    private final AuthRepository auth;
    private final MonthlyGoalsRepository repository;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Observer<Boolean> sessionObserver;
    private final long generation;
    private int request;
    private boolean cleared;

    public MonthlyGoalsViewModel(Application app) {
        super(app);
        auth = AuthRepository.getInstance(app);
        repository = new MonthlyGoalsRepository(auth.api(), new MonthlyGoalStorage(SQLiteGoalStore.getInstance(app)));
        generation = auth.sessionGeneration();
        sessionObserver = value -> {
            if (!auth.hasSession() || generation != auth.sessionGeneration()) {
                request++;
                draft = null;
                state.setValue(ScreenState.idle());
                saving.setValue(ScreenState.idle());
            }
        };
        auth.session().observeForever(sessionObserver);
    }

    public void load(Integer property, YearMonth selected) {
        if (state.getValue().busy() || saving.getValue().busy() || draft != null || System.currentTimeMillis() < state.getValue().retryAt) return;
        month = selected;
        int expected = ++request;
        saving.setValue(ScreenState.idle());
        state.setValue(ScreenState.loading());
        auth.resource(() -> repository.load(property, selected), value -> main.post(() -> { if (valid(expected)) state.setValue(value); }));
    }

    public void edit() {
        if (month.isBefore(YearMonth.now()) || state.getValue().status != ScreenState.Status.SUCCESS || saving.getValue().busy()) return;
        if (draft == null) draft = state.getValue().data.liters == null ? "" : state.getValue().data.liters.toPlainString();
        saving.setValue(ScreenState.idle());
    }

    public void cancel() {
        if (saving.getValue().busy()) return;
        draft = null;
        saving.setValue(ScreenState.idle());
    }

    public boolean dirty() {
        if (draft == null || state.getValue().data == null) return false;
        if (state.getValue().data.liters == null) return !draft.trim().isEmpty();
        try { return MonthlyGoal.parse(draft).compareTo(state.getValue().data.liters) != 0; }
        catch (RuntimeException invalid) { return true; }
    }

    public void save() {
        if (month.isBefore(YearMonth.now()) || draft == null || state.getValue().status != ScreenState.Status.SUCCESS || saving.getValue().busy() || System.currentTimeMillis() < saving.getValue().retryAt) return;
        try { MonthlyGoal.parse(draft); }
        catch (RuntimeException invalid) { return; }
        MonthlyGoalContext original = state.getValue().data;
        String input = draft;
        int expected = request;
        saving.setValue(ScreenState.loading());
        auth.resource(() -> repository.save(original, input), value -> main.post(() -> {
            if (!valid(expected)) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                draft = null;
                state.setValue(value);
                saving.setValue(ScreenState.success(true));
            } else saving.setValue(ScreenState.error(value.message, value.code, value.retryAt));
        }));
    }

    private boolean valid(int expected) { return !cleared && expected == request && generation == auth.sessionGeneration() && auth.hasSession(); }

    @Override
    protected void onCleared() {
        cleared = true;
        request++;
        auth.session().removeObserver(sessionObserver);
        main.removeCallbacksAndMessages(null);
    }
}
