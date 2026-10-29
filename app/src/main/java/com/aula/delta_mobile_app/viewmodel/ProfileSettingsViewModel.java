package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ProfileSettingsData;
import com.aula.delta_mobile_app.model.ResidentialDraft;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.*;
import java.io.File;
import java.util.Objects;

public final class ProfileSettingsViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<Profile>> profile = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<ProfileSettingsData>> residence = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Boolean>> saving = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<File>> photo = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<String> cepMessage = new MutableLiveData<>("");
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AuthRepository auth;
    private final ProfileSettingsRepository repository;
    private final ProfilePhotoRepository photos;
    private final PostalCodeRepository postal = new PostalCodeRepository();
    private final SavedStateHandle saved;
    private final SignupOptions catalog;
    private final Observer<Boolean> sessionObserver;
    private long generation;
    private int selection, photoRequest, cepRequest;
    private Integer propertyId;
    private boolean cleared, cepLoading;
    public ResidentialDraft draft;

    public ProfileSettingsViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        auth = AuthRepository.getInstance(app);
        repository = new ProfileSettingsRepository(auth);
        photos = new ProfilePhotoRepository(app);
        catalog = SignupOptions.load(app);
        generation = auth.sessionGeneration();
        sessionObserver = valid -> { if (!auth.hasSession() || generation != auth.sessionGeneration()) reset(); };
        auth.session().observeForever(sessionObserver);
    }

    public void loadProfile() {
        if (profile.getValue().busy() || profile.getValue().status == ScreenState.Status.SUCCESS) return;
        long expected = generation;
        profile.setValue(ScreenState.loading());
        repository.profile(value -> main.post(() -> { if (valid(expected)) profile.setValue(value); }));
    }

    public void loadResidence(Integer id, boolean force) {
        if (!force && Objects.equals(propertyId, id) && residence.getValue().status != ScreenState.Status.IDLE) return;
        propertyId = id;
        int request = ++selection;
        long expected = generation;
        draft = null;
        saved.remove("residential_draft");
        postal.cancel();
        cepLoading = false;
        cepMessage.setValue("");
        saving.setValue(ScreenState.idle());
        residence.setValue(ScreenState.loading());
        repository.residence(id, value -> main.post(() -> {
            if (!valid(expected) || request != selection) return;
            residence.setValue(value);
        }));
    }

    public void editResidence() {
        if (residence.getValue().status != ScreenState.Status.SUCCESS || saving.getValue().busy()) return;
        if (draft == null) draft = new ResidentialDraft(residence.getValue().data.address);
    }

    public void updateDraft(String cep, String city, String state) {
        if (draft == null) return;
        boolean changedCep = !draft.cep.equals(cep.replaceAll("\\D", ""));
        draft.cep = cep.replaceAll("\\D", "");
        draft.city = city;
        draft.state = state;
        saved.set("residential_draft", new String[] { draft.cep, city, state });
        if (changedCep) {
            cepRequest++;
            postal.cancel();
            cepLoading = false;
            cepMessage.setValue("");
        }
    }

    public boolean dirty() { return draft != null && residence.getValue().status == ScreenState.Status.SUCCESS && draft.changed(residence.getValue().data.address); }

    public void cancelEdit() {
        if (saving.getValue().busy()) return;
        draft = null;
        saved.remove("residential_draft");
        postal.cancel();
        cepLoading = false;
        cepRequest++;
        cepMessage.setValue("");
        saving.setValue(ScreenState.idle());
    }

    public void lookupCep() {
        if (draft == null || !draft.cep.matches("[0-9]{8}")) return;
        String cep = draft.cep;
        String previousCity = draft.city, previousState = draft.state;
        int request = ++cepRequest;
        long expected = generation;
        cepLoading = true;
        cepMessage.setValue("Consultando CEP…");
        postal.getCep(cep, value -> main.post(() -> {
            if (!valid(expected) || request != cepRequest || draft == null || !cep.equals(draft.cep)) return;
            cepLoading = false;
            if (value.status == ScreenState.Status.SUCCESS) {
                if (Objects.equals(draft.city, previousCity)) draft.city = value.data.city;
                if (Objects.equals(draft.state, previousState)) draft.state = value.data.state;
                saved.set("residential_draft", new String[] { draft.cep, draft.city, draft.state });
                cepMessage.setValue("Cidade e estado preenchidos pelo CEP. Confira antes de salvar.");
            } else cepMessage.setValue(value.message);
        }));
    }

    public void saveResidence() {
        if (draft == null || saving.getValue().busy() || residence.getValue().status != ScreenState.Status.SUCCESS) return;
        if (cepLoading) { saving.setValue(ScreenState.error("Aguarde a consulta de CEP terminar antes de salvar.")); return; }
        String error = draft.validation();
        ProfileSettingsData original = residence.getValue().data;
        Integer region = Objects.equals(draft.city.trim(), original.address.city) && Objects.equals(draft.state.trim(), original.address.state)
            ? original.address.regionId : catalog.region(draft.state, draft.city);
        if (region == null) error = "A localidade precisa de uma região tarifária configurada antes de salvar.";
        if (error != null) { saving.setValue(ScreenState.error(error)); return; }
        long expected = generation;
        int request = selection;
        saving.setValue(ScreenState.loading());
        repository.save(original, draft.request(original.address, region), value -> main.post(() -> {
            if (!valid(expected) || request != selection) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                draft = null;
                saved.remove("residential_draft");
                residence.setValue(value);
                saving.setValue(ScreenState.success(true));
            } else saving.setValue(ScreenState.error(value.message, value.code, value.retryAt));
        }));
    }

    public String captureUri() { return saved.get("profile_capture_uri"); }
    public void captureUri(String uri) { saved.set("profile_capture_uri", uri); }

    public void preparePhoto(Uri uri) {
        long expected = generation;
        int request = ++photoRequest;
        clearPhotoFile();
        photo.setValue(ScreenState.loading());
        photos.prepare(uri, value -> main.post(() -> {
            if (uri.toString().equals(captureUri())) clearCapture();
            if (!valid(expected) || request != photoRequest) { if (value.data != null) value.data.delete(); return; }
            photo.setValue(value);
        }));
    }

    public void discardPhoto() {
        photoRequest++;
        clearPhotoFile();
        photo.setValue(ScreenState.idle());
    }

    private void clearPhotoFile() { File file = photo.getValue().data; if (file != null) file.delete(); }
    private void clearCapture() {
        String uri = captureUri();
        if (uri != null) {
            try { getApplication().getContentResolver().delete(Uri.parse(uri), null, null); } catch (RuntimeException ignored) {}
        }
        saved.remove("profile_capture_uri");
    }
    private boolean valid(long expected) { return !cleared && expected == generation && expected == auth.sessionGeneration() && auth.hasSession(); }

    private void reset() {
        generation = auth.sessionGeneration();
        selection++;
        photoRequest++;
        postal.cancel();
        cepLoading = false;
        main.removeCallbacksAndMessages(null);
        clearPhotoFile();
        draft = null;
        propertyId = null;
        saved.remove("residential_draft");
        clearCapture();
        profile.setValue(ScreenState.idle());
        residence.setValue(ScreenState.idle());
        photo.setValue(ScreenState.idle());
        saving.setValue(ScreenState.idle());
        cepMessage.setValue("");
    }

    @Override
    protected void onCleared() {
        cleared = true;
        auth.session().removeObserver(sessionObserver);
        postal.cancel();
        photos.close();
        clearCapture();
        clearPhotoFile();
        main.removeCallbacksAndMessages(null);
    }
}
