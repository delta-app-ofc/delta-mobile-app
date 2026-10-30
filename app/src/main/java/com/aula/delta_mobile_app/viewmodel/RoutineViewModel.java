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
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.RoutineActions;
import com.aula.delta_mobile_app.repository.RoutineRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
    private RoutineUiState.DetailTarget detailTarget;
    private ScreenState<RoutineData.Habit> detail = ScreenState.idle();
    private RoutineFormState form;
    private long formGeneration;
    private ScreenState<RoutineData.Habit> creation = ScreenState.idle();
    private boolean uncertainCreation, checkedCreation, confirmResend, savedAwaitingReload;
    private long creationGeneration, checkGeneration = -1;
    private RoutineUiState.DeleteTarget deleteTarget, deleteCheckTarget;
    private ScreenState<Void> deletion = ScreenState.idle();
    private boolean uncertainDeletion, checkedDeletion;
    private long deletionGeneration, deleteCheckGeneration = -1;

    private void resetCreation() {
        creationGeneration++;
        creation = ScreenState.idle();
        uncertainCreation = checkedCreation = confirmResend = savedAwaitingReload = false;
        checkGeneration = -1;
    }

    private void resetDeletion() {
        deletionGeneration++;
        deleteTarget = deleteCheckTarget = null;
        deletion = ScreenState.idle();
        uncertainDeletion = checkedDeletion = false;
        deleteCheckGeneration = -1;
    }

    public void saveForm() {
        if (cleared || !ensureSession() || form == null || creation.busy() || uncertainCreation
            || creation.retryAt > clock.millis() || !validateForm()) return;
        RoutineFormState submitted = form;
        long operation = ++creationGeneration;
        creation = ScreenState.loading();
        publish();
        if (cleared || form != submitted || operation != creationGeneration) return;
        Consumer<ScreenState<RoutineData.Habit>> callback = result -> {
            if (!cleared) dispatch.accept(() -> {
                if (cleared || operation != creationGeneration || form != submitted || !creation.busy() || !ensureSession()) return;
                creation = result == null || (result.status != ScreenState.Status.SUCCESS && result.status != ScreenState.Status.ERROR)
                    || (result.status == ScreenState.Status.SUCCESS && result.data == null)
                    ? ScreenState.error("Não foi possível confirmar o resultado da alteração.", RoutineRepository.UNCERTAIN, 0) : result;
                if (creation.status == ScreenState.Status.SUCCESS) {
                    form = null;
                    savedAwaitingReload = true;
                    generation++;
                    loading = false;
                    reading = ScreenState.idle();
                    load(true);
                } else if (creation.code == 401 || creation.code == 403) {
                    reading = ScreenState.error(creation.message, creation.code, creation.retryAt);
                    data = null;
                    form = null;
                    publish();
                } else {
                    uncertainCreation = creation.code == RoutineRepository.UNCERTAIN || creation.code >= 500;
                    checkedCreation = false;
                    publish();
                }
            });
        };
        if (submitted.mode == RoutineFormState.Mode.CREATE) repository.create(submitted, callback);
        else repository.update(submitted, callback);
    }

    public void checkCreation() {
        if (cleared || !ensureSession() || form == null || !uncertainCreation || creation.busy() || loading || reading.retryAt > clock.millis()) return;
        checkedCreation = false;
        checkGeneration = generation + 1;
        load(true);
    }

    public void requestResend() {
        if (cleared || form == null || !uncertainCreation || !checkedCreation || loading) return;
        confirmResend = true;
        publish();
    }

    public void cancelResend() {
        confirmResend = false;
        if (!cleared) publish();
    }

    public void confirmResend() {
        if (cleared || !ensureSession() || !confirmResend || !checkedCreation || loading || form == null) return;
        uncertainCreation = checkedCreation = confirmResend = false;
        creation = ScreenState.idle();
        saveForm();
    }

    public boolean requestDelete() {
        if (cleared || !ensureSession() || form != null || deleteTarget != null || detailTarget == null || detail.status != ScreenState.Status.SUCCESS
            || detail.data == null || detail.data.id != detailTarget.id || detail.data.userId != detailTarget.userId) return false;
        deleteTarget = new RoutineUiState.DeleteTarget(detail.data);
        deletion = ScreenState.idle();
        uncertainDeletion = checkedDeletion = false;
        deleteCheckGeneration = -1;
        deleteCheckTarget = null;
        resetDetail();
        publish();
        return true;
    }

    public void cancelDelete() {
        if (cleared || deleteTarget == null || deletion.busy()) return;
        resetDeletion();
        publish();
    }

    public void confirmDelete() {
        if (cleared || !ensureSession() || deleteTarget == null || deletion.busy() || deletion.retryAt > clock.millis()
            || (uncertainDeletion && !checkedDeletion)) return;
        if (uncertainDeletion) {
            uncertainDeletion = checkedDeletion = false;
            deletion = ScreenState.idle();
        }
        RoutineUiState.DeleteTarget submitted = deleteTarget;
        long operation = ++deletionGeneration;
        deletion = ScreenState.loading();
        publish();
        if (cleared || deleteTarget != submitted || operation != deletionGeneration) return;
        repository.delete(submitted.id, submitted.userId, result -> {
            if (!cleared) dispatch.accept(() -> {
                if (cleared || operation != deletionGeneration || deleteTarget != submitted || !deletion.busy() || !ensureSession()) return;
                deletion = result == null || (result.status != ScreenState.Status.SUCCESS && result.status != ScreenState.Status.ERROR)
                    ? ScreenState.error("Não foi possível confirmar o resultado da exclusão.", RoutineRepository.UNCERTAIN, 0) : result;
                if (deletion.status == ScreenState.Status.SUCCESS) {
                    deleteTarget = null;
                    uncertainDeletion = checkedDeletion = false;
                    savedAwaitingReload = true;
                    generation++;
                    loading = false;
                    reading = ScreenState.idle();
                    load(true);
                } else if (deletion.code == 401 || deletion.code == 403) {
                    data = null;
                    resetDeletion();
                    reading = ScreenState.error(result.message, result.code, result.retryAt);
                    publish();
                } else {
                    uncertainDeletion = deletion.code == RoutineRepository.UNCERTAIN || deletion.code >= 500;
                    checkedDeletion = false;
                    publish();
                }
            });
        });
    }

    public void checkDeletion() {
        if (cleared || !ensureSession() || deleteTarget == null || !uncertainDeletion || deletion.busy() || loading
            || reading.retryAt > clock.millis()) return;
        checkedDeletion = false;
        deleteCheckTarget = deleteTarget;
        deleteCheckGeneration = generation + 1;
        load(true);
    }
    private long generation;
    private long detailGeneration;
    private boolean signedIn, loading;
    private boolean detailLoading;
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
            form = null;
            resetCreation();
            resetDeletion();
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
        resetDetail();
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
        resetDeletion();
        resetDetail();
        publish();
    }

    public boolean openDetails(int id, LocalTime startTime) {
        if (cleared || !ensureSession() || loading || deleteTarget != null || reading.status != ScreenState.Status.SUCCESS || data == null || startTime == null) return false;
        RoutineData.Habit habit = null;
        for (RoutineData.Habit value : data.habits)
            if (value.id == id) habit = value;
        if (habit == null || !habit.scheduleComplete || !habit.schedule.getOrDefault(selectedDate.getDayOfWeek(), java.util.Collections.emptyList()).contains(startTime)) return false;
        boolean same = detailTarget != null && detailTarget.id == id && detailTarget.userId == habit.userId && detailTarget.date.equals(selectedDate)
            && detailTarget.startTime.equals(startTime);
        if (same && (detailLoading || detail.status == ScreenState.Status.SUCCESS)) return true;
        if (!same) {
            resetDetail();
            detailTarget = new RoutineUiState.DetailTarget(id, habit.userId, selectedDate, startTime);
        }
        loadDetail();
        return detailTarget != null;
    }

    public void retryDetail() {
        if (!cleared && detailTarget != null && detail.status == ScreenState.Status.ERROR) loadDetail();
    }

    public void closeDetails() {
        if (cleared || detailTarget == null) return;
        resetDetail();
        publish();
    }

    public boolean beginCreateForm() {
        if (cleared || !ensureSession() || form != null || deleteTarget != null) return false;
        form = RoutineFormState.create(++formGeneration);
        resetCreation();
        publish();
        return true;
    }

    public boolean beginEditForm() {
        if (cleared || !ensureSession() || form != null || deleteTarget != null || detailTarget == null || detail.status != ScreenState.Status.SUCCESS || detail.data == null
            || detail.data.id != detailTarget.id || detail.data.userId != detailTarget.userId) return false;
        form = RoutineFormState.edit(++formGeneration, detail.data, selectedDate);
        resetCreation();
        resetDetail();
        publish();
        return true;
    }

    public void setFormHabit(Integer value) {
        editForm(current -> current.withHabit(value));
    }

    public void setFormFrequency(String value) {
        editForm(current -> current.withFrequency(value));
    }

    public void selectFormDay(DayOfWeek day, boolean selected) {
        editForm(current -> current.selectDay(day, selected));
    }

    public void setFormQuantity(DayOfWeek day, String value) {
        editForm(current -> current.withQuantity(day, value));
    }

    public void setFormTime(DayOfWeek day, int index, String value) {
        editForm(current -> current.withTime(day, index, value));
    }

    public boolean validateForm() {
        if (cleared || !ensureSession() || form == null) return false;
        form = form.validate();
        publish();
        return form != null && form.valid();
    }

    public void requestFormClose(long token) {
        if (cleared || form == null || form.token != token || creation.busy()) return;
        form = form.dirty ? form.confirmDiscard(true) : null;
        publish();
    }

    public void cancelFormDiscard(long token) {
        if (cleared || form == null || form.token != token) return;
        form = form.confirmDiscard(false);
        publish();
    }

    public void discardForm(long token) {
        if (cleared || form == null || form.token != token || creation.busy()) return;
        form = null;
        resetCreation();
        publish();
    }

    private void editForm(UnaryOperator<RoutineFormState> update) {
        if (cleared || !ensureSession() || form == null || creation.busy()) return;
        RoutineFormState next = update.apply(form);
        if (next == form) return;
        form = next;
        publish();
    }

    private void resetDetail() {
        detailGeneration++;
        detailLoading = false;
        detailTarget = null;
        detail = ScreenState.idle();
    }

    private void loadDetail() {
        if (cleared || !ensureSession() || detailTarget == null || detailLoading || detail.retryAt > clock.millis()) return;
        RoutineUiState.DetailTarget target = detailTarget;
        long request = ++detailGeneration;
        long account = generation;
        detailLoading = true;
        detail = ScreenState.loading();
        publish();
        if (cleared || request != detailGeneration || detailTarget != target || !detailLoading) return;
        repository.detail(target.id, result -> {
            if (!cleared) dispatch.accept(() -> completeDetail(account, request, target, result));
        });
    }

    private void completeDetail(long account, long request, RoutineUiState.DetailTarget target, ScreenState<RoutineData.Habit> result) {
        if (cleared || account != generation || request != detailGeneration || target != detailTarget || !detailLoading || !ensureSession()) return;
        detailLoading = false;
        if (result == null || (result.status != ScreenState.Status.SUCCESS && result.status != ScreenState.Status.ERROR)
            || (result.status == ScreenState.Status.SUCCESS && (result.data == null || result.data.id != target.id || result.data.userId != target.userId))) {
            detail = ScreenState.error("Não foi possível carregar o detalhe do hábito.");
        } else {
            detail = result;
            if (result.status == ScreenState.Status.ERROR && (result.code == 401 || result.code == 403)) {
                data = null;
                form = null;
                reading = ScreenState.error(result.message, result.code, result.retryAt);
            }
        }
        publish();
    }

    private void load(boolean refresh) {
        if (cleared || !ensureSession() || loading || (!refresh && reading.status == ScreenState.Status.SUCCESS)) return;
        if (reading.status == ScreenState.Status.ERROR && reading.retryAt > clock.millis()) return;
        if (uncertainCreation) checkedCreation = false;
        if (uncertainDeletion) checkedDeletion = false;
        resetDetail();
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
        if (request == checkGeneration) {
            checkedCreation = result != null && result.status == ScreenState.Status.SUCCESS && result.data != null;
            checkGeneration = -1;
        }
        RoutineUiState.DeleteTarget checkedTarget = request == deleteCheckGeneration ? deleteCheckTarget : null;
        if (request == deleteCheckGeneration) {
            deleteCheckGeneration = -1;
            deleteCheckTarget = null;
        }
        if (result != null && result.status == ScreenState.Status.SUCCESS && result.data != null) savedAwaitingReload = false;
        if (result == null || (result.status != ScreenState.Status.SUCCESS && result.status != ScreenState.Status.ERROR)
            || (result.status == ScreenState.Status.SUCCESS && result.data == null)) {
            reading = ScreenState.error("Não foi possível carregar a rotina.");
        } else {
            reading = result;
            if (result.status == ScreenState.Status.SUCCESS) data = result.data;
            else if (result.code == 401 || result.code == 403) {
                data = null;
                form = null;
                resetDeletion();
            }
        }
        if (checkedTarget != null && checkedTarget == deleteTarget && uncertainDeletion) {
            if (reading.status == ScreenState.Status.SUCCESS && data != null) {
                boolean present = false;
                for (RoutineData.Habit habit : data.habits) if (habit.id == checkedTarget.id && habit.userId == checkedTarget.userId) present = true;
                if (present) checkedDeletion = true;
                else {
                    deleteTarget = null;
                    uncertainDeletion = checkedDeletion = false;
                    deletion = ScreenState.success(null);
                }
            } else checkedDeletion = false;
        }
        publish();
    }

    private boolean ensureSession() {
        if (signedIn && authenticated.getAsBoolean()) return true;
        generation++;
        loading = false;
        data = null;
        form = null;
        resetCreation();
        resetDeletion();
        reading = ScreenState.error("Entre novamente para continuar.", 401, 0);
        resetDetail();
        publish();
        return false;
    }

    private void publish() {
        state.setValue(new RoutineUiState(selectedDate, reading, data, detailTarget, detail, form, creation, uncertainCreation, checkedCreation, confirmResend,
            savedAwaitingReload, deleteTarget, deletion, uncertainDeletion, checkedDeletion));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        generation++;
        loading = false;
        data = null;
        form = null;
        resetDeletion();
        resetDetail();
        session.removeObserver(sessionObserver);
        clearDispatch.run();
    }
}
