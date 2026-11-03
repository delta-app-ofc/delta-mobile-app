package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.*;

import androidx.lifecycle.*;

import com.aula.delta_mobile_app.model.AbnormalConsumptionData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AbnormalConsumptionRepository;
import com.aula.delta_mobile_app.repository.AuthRepository;

public final class AbnormalConsumptionViewModel extends AndroidViewModel {
    private final MutableLiveData<ScreenState<AbnormalConsumptionData>> state = new MutableLiveData<>(ScreenState.idle());
    private final AbnormalConsumptionRepository repository;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Integer property;
    private int generation;
    private boolean cleared;
    public AbnormalConsumptionData.Period period = AbnormalConsumptionData.Period.WEEK;

    public AbnormalConsumptionViewModel(Application app) {
        super(app);
        repository = new AbnormalConsumptionRepository(AuthRepository.getInstance(app));
    }

    public LiveData<ScreenState<AbnormalConsumptionData>> state() {
        return state;
    }

    public void load(Integer id, boolean refresh) {
        ScreenState<AbnormalConsumptionData> current = state.getValue();
        if (java.util.Objects.equals(id, property) && (current.busy() || (!refresh && current.status == ScreenState.Status.SUCCESS)))
            return;
        property = id;
        int request = ++generation;
        state.setValue(ScreenState.loading());
        repository.load(id, value -> main.post(() -> {
            if (!cleared && request == generation) state.setValue(value);
        }));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        generation++;
        main.removeCallbacksAndMessages(null);
    }
}
