package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.PostalCodeResponse;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.PostalCodeRepository;
import com.aula.delta_mobile_app.repository.SignupOptions;

import java.time.LocalDate;
import java.util.*;

public class SignupViewModel extends AndroidViewModel {
    public final Bundle draft = new Bundle();
    public boolean hostInitialized;
    public final MutableLiveData<ScreenState<Boolean>> creation = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<PostalCodeResponse>> postalCode = new MutableLiveData<>(ScreenState.idle());
    public final MutableLiveData<ScreenState<Completed>> completion = new MutableLiveData<>(ScreenState.idle());
    public final SignupOptions catalog;
    private final AuthRepository auth;
    private final PostalCodeRepository postalCodes = new PostalCodeRepository();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Habit> habits = new LinkedHashMap<>();
    private String password = "", confirmation = "", currentCep = "";
    private int epoch;
    private final SignupFlow requests;

    public SignupViewModel(Application application) {
        super(application);
        auth = AuthRepository.getInstance(application);
        catalog = SignupOptions.load(application);
        requests = new SignupFlow(auth, action -> main.post(action), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                password = "";
                confirmation = "";
            }
            creation.setValue(value);
        }, completion::setValue);
    }

    public String secret(String key) {
        return "password".equals(key) ? password : confirmation;
    }

    public void secret(String key, String value) {
        if ("password".equals(key)) password = value;
        else confirmation = value;
    }

    public String personalError() {
        return SignupValidation.personal(draft.getString("name", ""), draft.getString("phone", ""), draft.getString("birthDate", ""), LocalDate.now());
    }

    public static LocalDate latestAllowedBirthDate() {
        return LocalDate.now().minusYears(18);
    }

    private LocalDate birthDate() {
        return SignupValidation.birthDate(draft.getString("birthDate", ""));
    }

    public void create() {
        ScreenState<Boolean> state = creation.getValue();
        if (state != null && (state.busy() || state.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < state.retryAt)) return;
        if (auth.hasSession()) {
            creation.setValue(ScreenState.error("Já existe uma sessão. Retome o complemento ou saia antes de criar" + " outra conta."));
            return;
        }
        String error = personalError(), email = draft.getString("email", "").trim();
        if (error == null)
            error = SignupValidation.credentials(email, password, confirmation, value -> Patterns.EMAIL_ADDRESS.matcher(value).matches());
        if (error != null) {
            creation.setValue(ScreenState.error(error));
            return;
        }
        CreateAccount request = new CreateAccount(draft.getString("name").trim(), birthDate().toString(), draft.getString("phone", "").replaceAll("\\D", ""), email, password);
        requests.create(request);
    }

    public void getCep(String digits, boolean retry) {
        if (!retry && currentCep.equals(digits)) return;
        currentCep = digits;
        postalCodes.cancel();
        draft.remove("state");
        draft.remove("city");
        draft.remove("regionId");
        draft.remove("latitude");
        draft.remove("longitude");
        if (digits.length() != 8) {
            postalCode.setValue(ScreenState.idle());
            return;
        }
        postalCode.setValue(ScreenState.loading());
        int attempt = epoch;
        postalCodes.getCep(digits, result -> {
            if (attempt != epoch || !currentCep.equals(digits)) return;
            if (result.status == ScreenState.Status.SUCCESS) {
                PostalCodeResponse value = result.data;
                draft.putString("city", value.city);
                draft.putString("state", value.state);
                Integer region = catalog.region(value.state, value.city);
                if (region != null) draft.putInt("regionId", region);
                if (value.location != null && value.location.coordinates != null) {
                    Double lat = value.location.coordinates.latitude, lon = value.location.coordinates.longitude;
                    if (lat != null && lon != null && Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180) {
                        draft.putDouble("latitude", lat);
                        draft.putDouble("longitude", lon);
                    }
                }
            }
            postalCode.setValue(result);
        });
    }

    public Habit habit(String key) {
        return habits.get(key);
    }

    public void removeHabit(String key) {
        habits.remove(key);
        draft.putBoolean(key, false);
    }

    public String setHabit(String key, int frequency, List<Integer> days) {
        Integer id = catalog.habit(key);
        if (id == null || frequency <= 0 || days.isEmpty() || new HashSet<>(days).size() != days.size())
            return "Informe frequência positiva e dias válidos.";
        for (Integer day : days)
            if (day == null || day <= 0 || catalog.days.stream().noneMatch(entry -> entry.id == day))
                return "Dia não disponível no catálogo.";
        for (Map.Entry<String, Habit> entry : habits.entrySet())
            if (!entry.getKey().equals(key) && id.equals(entry.getValue().habitId))
                return "Este hábito já foi selecionado.";
        Habit value = new Habit();
        value.habitId = id;
        value.frequency = frequency;
        value.daysOfWeek = new ArrayList<>(days);
        habits.put(key, value);
        draft.putBoolean(key, true);
        return null;
    }

    public static final String[] TYPES = {"", "CASA", "PRÉDIO"};
    public static final String[] CLASSIFICATIONS = {
        "", "RESIDENCIAL_NORMAL", "COMERCIAL_ENTIDADE_ASSISTENCIA_SOCIAL", "COMERCIAL_NORMAL_INDUSTRIAL", "RESIDENCIAL_ESPECIAL", "COMERCIAL_ESPECIAL", "RESIDENCIAL_FAVELA",
        "PUBLICA_COM_CONTRATO", "RESIDENCIAL_SOCIAL"
    };

    public static boolean isResidentialClassification(int index) {
        return index > 0 && index < CLASSIFICATIONS.length && SignupValidation.isResidential(CLASSIFICATIONS[index]);
    }

    public String propertyError() {
        String error = SignupValidation.property(draft.getString("propertyName", ""), draft.getInt("propertyIndex"), draft.getInt("classificationIndex"), TYPES.length,
            CLASSIFICATIONS.length, draft.getString("builtArea", ""));
        if (error != null) return error;
        return isResidentialClassification(draft.getInt("classificationIndex")) ? null : "Selecione uma classificação residencial.";
    }

    public void finish() {
        ScreenState<Completed> state = completion.getValue();
        if (state != null && (state.busy() || state.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < state.retryAt)) return;
        String error = propertyError();
        if (error == null && (currentCep.length() != 8 || !draft.containsKey("regionId")))
            error = "Esta localidade ainda não tem uma região tarifária configurada. Não é possível" + " concluir o cadastro.";
        if (error == null && (draft.getString("city", "").isEmpty() || draft.getString("city", "").length() > 60 || draft.getString("state", "").isEmpty()
            || draft.getString("state", "").length() > 30))
        error = "Consulte um CEP válido antes de concluir.";
        if (error != null) {
            completion.setValue(ScreenState.error(error));
            return;
        }
        requests.finish(buildCompletion());
    }

    private Completion buildCompletion() {
        Completion request = new Completion();
        request.address = new Address();
        request.property = new Property();
        request.address.cep = currentCep;
        request.address.regionId = draft.getInt("regionId");
        request.address.city = draft.getString("city");
        request.address.state = draft.getString("state");
        if (draft.containsKey("latitude") && draft.containsKey("longitude")) {
            request.address.latitude = java.math.BigDecimal.valueOf(draft.getDouble("latitude"));
            request.address.longitude = java.math.BigDecimal.valueOf(draft.getDouble("longitude"));
        }
        request.property.name = draft.getString("propertyName").trim();
        request.property.type = TYPES[draft.getInt("propertyIndex")];
        request.property.classification = CLASSIFICATIONS[draft.getInt("classificationIndex")];
        String area = draft.getString("builtArea", "").trim();
        if (!area.isEmpty())
            request.property.builtAreaM2 = new java.math.BigDecimal(area.replace(',', '.'));
        request.habits.addAll(habits.values());
        return request;
    }

    public void consumeCompletion() {
        requests.consumeCompletion();
    }

    public void reset() {
        epoch++;
        postalCodes.cancel();
        draft.clear();
        habits.clear();
        password = "";
        confirmation = "";
        currentCep = "";
        requests.reset();
        postalCode.setValue(ScreenState.idle());
    }

    @Override
    protected void onCleared() {
        postalCodes.cancel();
        password = "";
        confirmation = "";
    }
}
