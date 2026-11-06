package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.model.ApiModels.PropertyResponse;
import com.aula.delta_mobile_app.model.PostalCodeResponse;
import com.aula.delta_mobile_app.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class UnitsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<List<UnitSummary>>> units = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<PropertyResponse>> creation = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Integer>> deletion = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<PostalCodeResponse>> postalState = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public UnitDraft draft;
    private UnitsRepository.Creation progress = new UnitsRepository.Creation();
    private final AuthRepository auth;
    private final UnitsRepository repository;
    private final PostalCodeRepository postal = new PostalCodeRepository();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SavedStateHandle saved;
    private final long generation;
    private final Observer<Boolean> sessionObserver;
    private int postalRequest;
    private boolean cleared;

    public UnitsViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        auth = AuthRepository.getInstance(app);
        repository = new UnitsRepository(auth.api());
        generation = auth.sessionGeneration();
        draft = UnitDraft.restore(saved.get("unit_draft"));
        int[] checkpoint = saved.get("unit_creation");
        if (checkpoint != null && checkpoint.length == 4) {
            progress.userId = checkpoint[0] == 0 ? null : checkpoint[0];
            progress.addressId = checkpoint[1] == 0 ? null : checkpoint[1];
            progress.addressUncertain = checkpoint[2] == 1;
            progress.propertyUncertain = checkpoint[3] == 1;
        }
        sessionObserver = valid -> {
            if (!auth.hasSession() || generation != auth.sessionGeneration()) {
                postalRequest++;
                postal.cancel();
                saved.remove("unit_draft");
                saved.remove("unit_creation");
                draft = new UnitDraft();
                progress = new UnitsRepository.Creation();
                units.setValue(ScreenState.idle());
                creation.setValue(ScreenState.idle());
                deletion.setValue(ScreenState.idle());
                postalState.setValue(ScreenState.idle());
            }
        };
        auth.session().observeForever(sessionObserver);
    }

    private boolean valid() { return !cleared && auth.hasSession() && generation == auth.sessionGeneration(); }
    public boolean busy() { return creation.getValue().busy() || deletion.getValue().busy(); }
    public boolean addressLocked() { return progress.addressId != null || progress.addressUncertain || progress.propertyUncertain; }

    public void load() {
        if (!valid() || units.getValue().busy() || busy() || System.currentTimeMillis() < units.getValue().retryAt) return;
        units.setValue(ScreenState.loading());
        auth.screenResource(() -> repository.load(LocalDate.now()), value -> main.post(() -> { if (valid()) units.setValue(value); }));
    }

    public void beginNew() {
        if (busy()) return;
        draft = new UnitDraft();
        progress = new UnitsRepository.Creation();
        saved.remove("unit_creation");
        creation.setValue(ScreenState.idle());
        postalRequest++;
        postal.cancel();
        postalState.setValue(ScreenState.idle());
        changed();
    }

    public void changed() { saved.set("unit_draft", draft.values()); changes.setValue(changes.getValue() + 1); }

    public void update(String name, String cep) {
        if (busy()) return;
        draft.name = name;
        String normalized = cep.replaceAll("\\D", "");
        if (!Objects.equals(draft.cep, normalized) && !addressLocked()) {
            draft.cep = normalized;
            draft.city = draft.state = draft.confirmedCep = "";
            draft.latitude = draft.longitude = null;
            postalRequest++;
            postal.cancel();
            postalState.setValue(ScreenState.idle());
        }
        changed();
    }

    public void lookup() {
        if (!valid() || busy() || addressLocked() || !draft.cep.matches("[0-9]{8}")) return;
        String cep = draft.cep;
        int request = ++postalRequest;
        postalState.setValue(ScreenState.loading());
        postal.getCep(cep, value -> main.post(() -> {
            if (!valid() || request != postalRequest || !cep.equals(draft.cep)) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                draft.city = value.data.city;
                draft.state = value.data.state;
                draft.confirmedCep = cep;
                if (value.data.location != null && value.data.location.coordinates != null) {
                    Double latitude = value.data.location.coordinates.latitude, longitude = value.data.location.coordinates.longitude;
                    if (latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude) && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) {
                        draft.latitude = java.math.BigDecimal.valueOf(latitude);
                        draft.longitude = java.math.BigDecimal.valueOf(longitude);
                    }
                }
            }
            postalState.setValue(value);
            changed();
        }));
    }

    public void create() {
        if (!valid() || busy() || postalState.getValue().busy() || System.currentTimeMillis() < creation.getValue().retryAt) return;
        String error = draft.validation();
        if (error != null) { creation.setValue(ScreenState.error(error)); return; }
        UnitDraft input = UnitDraft.restore(draft.values());
        UnitsRepository.Creation checkpoint = progress;
        saved.set("unit_creation", new int[] { checkpoint.userId == null ? 0 : checkpoint.userId, checkpoint.addressId == null ? 0 : checkpoint.addressId, 1, 1 });
        creation.setValue(ScreenState.loading());
        auth.resource(() -> repository.create(input, checkpoint), value -> main.post(() -> {
            if (!valid()) return;
            saved.set("unit_creation", new int[] { checkpoint.userId == null ? 0 : checkpoint.userId, checkpoint.addressId == null ? 0 : checkpoint.addressId, checkpoint.addressUncertain ? 1 : 0, checkpoint.propertyUncertain ? 1 : 0 });
            creation.setValue(value);
        }));
    }

    public void consumeCreation() { beginNew(); }

    public void delete(int propertyId) {
        if (!valid() || busy() || System.currentTimeMillis() < deletion.getValue().retryAt) return;
        deletion.setValue(ScreenState.loading());
        auth.resource(() -> repository.delete(propertyId), value -> main.post(() -> {
            if (!valid()) return;
            deletion.setValue(value.status == ScreenState.Status.SUCCESS ? ScreenState.success(propertyId) : ScreenState.error(value.message, value.code, value.retryAt));
        }));
    }

    public void consumeDeletion() { if (!deletion.getValue().busy()) deletion.setValue(ScreenState.idle()); }

    @Override protected void onCleared() { cleared = true; postalRequest++; postal.cancel(); auth.session().removeObserver(sessionObserver); main.removeCallbacksAndMessages(null); }
}
