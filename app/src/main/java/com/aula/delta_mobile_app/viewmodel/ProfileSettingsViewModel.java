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
import com.aula.delta_mobile_app.model.PersonalDraft;
import com.aula.delta_mobile_app.model.ResidentialDraft;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.*;
import java.io.File;
import java.util.Objects;

public final class ProfileSettingsViewModel extends AndroidViewModel {
    private static final long PHOTO_TIMEOUT_MS = 180_000;
    public final MutableLiveData<ScreenState<Profile>> profile = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<ProfileSettingsData>> residence = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Boolean>> saving = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Boolean>> savingProfile = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<File>> photo = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<String> cepMessage = new MutableLiveData<>("");
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AuthRepository auth;
    private final ProfileSettingsRepository repository;
    private final ProfilePhotoRepository photos;
    private final PostalCodeRepository postal = new PostalCodeRepository();
    private final SavedStateHandle saved;
    private final Observer<Boolean> sessionObserver;
    private long generation;
    private int selection, cepRequest;
    private volatile int photoRequest;
    private Runnable photoTimeout;
    private Integer propertyId;
    private Integer photoUserId;
    private File confirmedPhoto;
    private boolean cleared, cepLoading;
    public ResidentialDraft draft;
    public PersonalDraft personalDraft;

    public ProfileSettingsViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        auth = AuthRepository.getInstance(app);
        repository = new ProfileSettingsRepository(auth);
        photos = new ProfilePhotoRepository(app);
        generation = auth.sessionGeneration();
        sessionObserver = valid -> { if (!auth.hasSession() || generation != auth.sessionGeneration()) reset(); };
        auth.session().observeForever(sessionObserver);
    }

    public void loadProfile() {
        loadProfile(false);
    }

    public void loadProfile(boolean force) {
        if (personalDraft != null || savingProfile.getValue().busy() || profile.getValue().busy() || (!force && profile.getValue().status == ScreenState.Status.SUCCESS)) return;
        long expected = generation;
        profile.setValue(ScreenState.loading());
        repository.profile(value -> main.post(() -> { if (valid(expected)) profile.setValue(value); }));
    }

    public void editProfile() {
        if (profile.getValue().status != ScreenState.Status.SUCCESS || savingProfile.getValue().busy()) return;
        if (personalDraft == null) personalDraft = new PersonalDraft(profile.getValue().data);
        savingProfile.setValue(ScreenState.idle());
    }

    public void cancelProfile() {
        if (savingProfile.getValue().busy()) return;
        personalDraft = null;
        savingProfile.setValue(ScreenState.idle());
    }

    public boolean personalDirty() { return personalDraft != null && profile.getValue().data != null && personalDraft.changed(profile.getValue().data); }

    public void saveProfile() {
        if (personalDraft == null || savingProfile.getValue().busy() || System.currentTimeMillis() < savingProfile.getValue().retryAt) return;
        String error = personalDraft.validation();
        if (error != null) { savingProfile.setValue(ScreenState.error(error)); return; }
        long expected = generation;
        savingProfile.setValue(ScreenState.loading());
        repository.saveProfile(profile.getValue().data, personalDraft.request(), value -> main.post(() -> {
            if (!valid(expected)) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                personalDraft = null;
                profile.setValue(value);
                savingProfile.setValue(ScreenState.success(true));
            } else savingProfile.setValue(ScreenState.error(value.message, value.code, value.retryAt));
        }));
    }

    public void loadResidence(Integer id, boolean force) {
        if (Objects.equals(propertyId, id) && residence.getValue().busy()) return;
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
        if (draft == null) draft = new ResidentialDraft(residence.getValue().data);
    }

    public void residenceUnavailable(ScreenState<?> error) {
        selection++;
        residence.setValue(ScreenState.error(error.message, error.code, error.retryAt));
    }

    public void updateDraft(String name, String cep, String city, String state) {
        if (draft == null) return;
        draft.name = name;
        boolean changedCep = !draft.cep.equals(cep.replaceAll("\\D", ""));
        draft.cep = cep.replaceAll("\\D", "");
        draft.city = city;
        draft.state = state;
        saved.set("residential_draft", new String[] { name, draft.cep, city, state });
        if (changedCep) {
            draft.city = "";
            draft.state = "";
            cepRequest++;
            postal.cancel();
            cepLoading = false;
            cepMessage.setValue("");
        }
    }

    public boolean dirty() { return draft != null && residence.getValue().status == ScreenState.Status.SUCCESS && draft.changed(residence.getValue().data); }

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

    public boolean isConsultingCep() { return cepLoading; }

    public void lookupCep() {
        if (draft == null || !draft.cep.matches("[0-9]{8}")) return;
        String cep = draft.cep;
        String previousCity = draft.city, previousState = draft.state;
        int request = ++cepRequest;
        long expected = generation;
        cepLoading = true;
        cepMessage.setValue("");
        postal.getCep(cep, value -> main.post(() -> {
            if (!valid(expected) || request != cepRequest || draft == null || !cep.equals(draft.cep)) return;
            cepLoading = false;
            if (value.status == ScreenState.Status.SUCCESS) {
                if (Objects.equals(draft.city, previousCity)) draft.city = value.data.city;
                if (Objects.equals(draft.state, previousState)) draft.state = value.data.state;
                saved.set("residential_draft", new String[] { draft.name, draft.cep, draft.city, draft.state });
                cepMessage.setValue("");
            } else cepMessage.setValue(value.message);
        }));
    }

    public void saveResidence() {
        if (draft == null || saving.getValue().busy() || System.currentTimeMillis() < saving.getValue().retryAt || residence.getValue().status != ScreenState.Status.SUCCESS) return;
        if (draft.name == null || draft.name.trim().isEmpty()) { saving.setValue(ScreenState.error("Informe o nome da residência.")); return; }
        ProfileSettingsData original = residence.getValue().data;
        long expected = generation;
        int request = selection;
        String name = draft.name.trim();
        boolean renamed = !Objects.equals(name, original.property.name);
        saving.setValue(ScreenState.loading());
        repository.saveName(original, name, result -> main.post(() -> {
            if (!valid(expected) || request != selection) return;
            if (result.status != ScreenState.Status.SUCCESS) { saving.setValue(ScreenState.error(result.message, result.code, result.retryAt)); return; }
            residence.setValue(result);
            String error = cepLoading ? "A consulta do CEP ainda não terminou." : draft.validation();
            if (error != null) {
                saving.setValue(ScreenState.error((renamed ? "O nome da residência foi salvo, mas o endereço não foi atualizado. " : "O endereço não foi atualizado. ") + error));
                return;
            }
            saveResidenceAddress(result.data, expected, request, renamed);
        }));
    }

    private void saveResidenceAddress(ProfileSettingsData original, long expected, int request, boolean renamed) {
        repository.saveAddress(original, draft.request(original.address), value -> main.post(() -> {
            if (!valid(expected) || request != selection) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                draft = null;
                saved.remove("residential_draft");
                residence.setValue(value);
                saving.setValue(ScreenState.success(true));
            } else saving.setValue(ScreenState.error((renamed ? "O nome da residência foi salvo. " : "") + value.message, value.code, value.retryAt));
        }));
    }

    public String captureUri() { return saved.get("profile_capture_uri"); }
    public void captureUri(String uri) { saved.set("profile_capture_uri", uri); }

    public void preparePhoto(Uri uri) {
        ScreenState<Profile> current = profile.getValue();
        if (current.status != ScreenState.Status.SUCCESS || current.data.id == null || photo.getValue().busy()) return;
        long expected = generation;
        int request = ++photoRequest;
        photo.setValue(ScreenState.loading());
        startPhotoTimeout(expected, request);
        photos.upload(uri, current.data.id, () -> request == photoRequest && auth.hasSession() && auth.sessionGeneration() == expected, value -> main.post(() -> {
            if (!valid(expected) || request != photoRequest) { if (value.data != null) value.data.delete(); return; }
            cancelPhotoTimeout();
            if (uri.toString().equals(captureUri())) clearCapture();
            if (value.status == ScreenState.Status.SUCCESS) { clearPhotoFile(); confirmedPhoto = value.data; photoUserId = current.data.id; }
            photo.setValue(value);
        }));
    }

    public void loadPhoto() {
        ScreenState<Profile> current = profile.getValue();
        if (current.status == ScreenState.Status.SUCCESS) loadPhoto(current.data, false);
    }

    public void loadPhoto(Profile current, boolean force) {
        if (current == null || current.id == null || current.id <= 0 || photo.getValue().busy() || (!force && Objects.equals(photoUserId, current.id))) return;
        photoUserId = current.id;
        long expected = generation;
        int request = ++photoRequest;
        photo.setValue(ScreenState.loading());
        startPhotoTimeout(expected, request);
        photos.load(photoUserId, () -> request == photoRequest && auth.hasSession() && auth.sessionGeneration() == expected, value -> main.post(() -> {
            if (!valid(expected) || request != photoRequest) { if (value.data != null) value.data.delete(); return; }
            cancelPhotoTimeout();
            if (value.status == ScreenState.Status.SUCCESS) { clearPhotoFile(); confirmedPhoto = value.data; }
            if (value.status == ScreenState.Status.ERROR) photoUserId = null;
            photo.setValue(value);
        }));
    }

    public void discardPhoto() {
        photoRequest++;
        cancelPhotoTimeout();
        photo.setValue(confirmedPhoto == null ? ScreenState.idle() : ScreenState.success(confirmedPhoto));
    }

    private void startPhotoTimeout(long expected, int request) {
        cancelPhotoTimeout();
        photoTimeout = () -> {
            if (!valid(expected) || request != photoRequest || !photo.getValue().busy()) return;
            photoRequest++;
            photoTimeout = null;
            photoUserId = null;
            clearCapture();
            photo.setValue(ScreenState.error("A atualização da foto demorou demais. Tente novamente."));
        };
        main.postDelayed(photoTimeout, PHOTO_TIMEOUT_MS);
    }

    private void cancelPhotoTimeout() {
        if (photoTimeout != null) main.removeCallbacks(photoTimeout);
        photoTimeout = null;
    }

    private void clearPhotoFile() { if (confirmedPhoto != null) confirmedPhoto.delete(); confirmedPhoto = null; }
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
        cancelPhotoTimeout();
        postal.cancel();
        cepLoading = false;
        main.removeCallbacksAndMessages(null);
        clearPhotoFile();
        photoUserId = null;
        draft = null;
        personalDraft = null;
        propertyId = null;
        saved.remove("residential_draft");
        clearCapture();
        profile.setValue(ScreenState.idle());
        residence.setValue(ScreenState.idle());
        photo.setValue(ScreenState.idle());
        saving.setValue(ScreenState.idle());
        savingProfile.setValue(ScreenState.idle());
        cepMessage.setValue("");
    }

    @Override
    protected void onCleared() {
        cleared = true;
        photoRequest++;
        cancelPhotoTimeout();
        auth.session().removeObserver(sessionObserver);
        postal.cancel();
        photos.close();
        clearCapture();
        clearPhotoFile();
        main.removeCallbacksAndMessages(null);
    }
}
