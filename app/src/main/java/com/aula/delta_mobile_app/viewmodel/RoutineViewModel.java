package com.aula.delta_mobile_app.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.RoutineActions;
import com.aula.delta_mobile_app.repository.RoutineRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class RoutineViewModel extends ViewModel {
    public static final class Factory implements ViewModelProvider.Factory {
        private final AuthRepository auth;
        private final Clock clock;

        public Factory(Context context) {
            auth = AuthRepository.getInstance(context.getApplicationContext());
            clock = Clock.systemDefaultZone();
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(RoutineViewModel.class)) throw new IllegalArgumentException("Unsupported ViewModel");
            Handler main = new Handler(Looper.getMainLooper());
            return modelClass.cast(new RoutineViewModel(new RoutineRepository(auth, clock), clock, action -> main.post(action), auth.session(), auth::hasSession,
                () -> main.removeCallbacksAndMessages(null)));
        }
    }

    private final RoutineActions repository;
    private final Clock clock;
    private final Consumer<Runnable> dispatch;
    private final LiveData<Boolean> session;
    private final BooleanSupplier authenticated;
    private final Runnable clearDispatch;
    private final Observer<Boolean> sessionObserver;
    private final MutableLiveData<RoutineUiState> state;
    private LocalDate selectedDate;
    private RoutineData data;
    private ScreenState<RoutineData> reading = ScreenState.idle();
    private long generation;
    private boolean signedIn, loading;
    private volatile boolean cleared;

    public RoutineViewModel(RoutineActions repository, Clock clock, Consumer<Runnable> dispatch, LiveData<Boolean> session, BooleanSupplier authenticated) {
        this(repository, clock, dispatch, session, authenticated, () -> {});
    }

    private RoutineViewModel(RoutineActions repository, Clock clock, Consumer<Runnable> dispatch, LiveData<Boolean> session, BooleanSupplier authenticated, Runnable clearDispatch) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.dispatch = Objects.requireNonNull(dispatch);
        this.session = Objects.requireNonNull(session);
        this.authenticated = Objects.requireNonNull(authenticated);
        this.clearDispatch = Objects.requireNonNull(clearDispatch);
        selectedDate = LocalDate.now(clock);
        state = new MutableLiveData<>(new RoutineUiState(selectedDate, reading, null));
        sessionObserver = value -> {
            if (cleared) return;
            signedIn = Boolean.TRUE.equals(value);
            invalidate();
        };
        session.observeForever(sessionObserver);
    }

    public LiveData<RoutineUiState> state() {
        return state;
    }

    public void selectDate(LocalDate date) {
        if (cleared || Objects.equals(selectedDate, date)) return;
        selectedDate = Objects.requireNonNull(date);
        publish();
    }

    public void showMonth(YearMonth month) {
        if (cleared) return;
        selectDate(Objects.requireNonNull(month).atDay(Math.min(selectedDate.getDayOfMonth(), month.lengthOfMonth())));
    }

    public void changeMonth(int offset) {
        if (!cleared) showMonth(YearMonth.from(selectedDate).plusMonths(offset));
    }

    public void load() {
        load(false);
    }

    public void refresh() {
        load(true);
    }

    public void retry() {
        if (!cleared && reading.status == ScreenState.Status.ERROR) load(true);
    }

    public void invalidate() {
        if (cleared) return;
        generation++;
        loading = false;
        data = null;
        reading = ScreenState.idle();
        publish();
    }

    private void load(boolean refresh) {
        if (cleared || !ensureSession() || loading || (!refresh && reading.status == ScreenState.Status.SUCCESS)) return;
        if (reading.status == ScreenState.Status.ERROR && reading.retryAt > clock.millis()) return;
        long request = ++generation;
        loading = true;
        reading = ScreenState.loading();
        publish();
        if (cleared || request != generation || !loading) return;
        repository.load(value -> {
            if (!cleared) dispatch.accept(() -> complete(request, value));
        });
    }

    private void complete(long request, ScreenState<RoutineData> result) {
        if (cleared || request != generation || !loading || !ensureSession()) return;
        loading = false;
        if (result == null || (result.status != ScreenState.Status.SUCCESS && result.status != ScreenState.Status.ERROR)
            || (result.status == ScreenState.Status.SUCCESS && result.data == null)) {
            reading = ScreenState.error("Não foi possível carregar a rotina.");
        } else {
            reading = result;
            if (result.status == ScreenState.Status.SUCCESS) data = result.data;
            else if (result.code == 401 || result.code == 403) data = null;
        }
        publish();
    }

    private boolean ensureSession() {
        if (signedIn && authenticated.getAsBoolean()) return true;
        generation++;
        loading = false;
        data = null;
        reading = ScreenState.error("Entre novamente para continuar.", 401, 0);
        publish();
        return false;
    }

    private void publish() {
        state.setValue(new RoutineUiState(selectedDate, reading, data));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        generation++;
        loading = false;
        data = null;
        session.removeObserver(sessionObserver);
        clearDispatch.run();
    }
}
