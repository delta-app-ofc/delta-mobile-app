package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.*;

import com.aula.delta_mobile_app.model.LeakMonitoringData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.LeakMonitoringRepository;

public final class LeakMonitoringViewModel extends AndroidViewModel {
    private final MutableLiveData<ScreenState<LeakMonitoringData>> state = new MutableLiveData<>(ScreenState.idle());
    private final LeakMonitoringRepository repository;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Integer requestedProperty;
    private int generation;
    private boolean cleared;
    private String selectedEvent;

    public LeakMonitoringViewModel(Application app) {
        super(app);
        repository = new LeakMonitoringRepository(AuthRepository.getInstance(app));
    }

    public LiveData<ScreenState<LeakMonitoringData>> state() {
        return state;
    }

    public void load(Integer propertyId, boolean refresh) {
        ScreenState<LeakMonitoringData> current = state.getValue();
        boolean sameSelection = java.util.Objects.equals(propertyId, requestedProperty) || (current.status == ScreenState.Status.SUCCESS
            && java.util.Objects.equals(propertyId, current.data.propertyId));
        if (sameSelection && (current.busy() || (!refresh && current.status == ScreenState.Status.SUCCESS)))
            return;
        if (!java.util.Objects.equals(propertyId, requestedProperty)) selectedEvent = null;
        requestedProperty = propertyId;
        int request = ++generation;
        state.setValue(ScreenState.loading());
        repository.load(propertyId, value -> main.post(() -> {
            if (!cleared && request == generation) state.setValue(value);
        }));
    }

    public void selectEvent(String id) {
        selectedEvent = id;
    }

    public LeakMonitoringData.Event selected() {
        ScreenState<LeakMonitoringData> value = state.getValue();
        if (value.status != ScreenState.Status.SUCCESS || value.data.events.isEmpty()) return null;
        for (LeakMonitoringData.Event event : value.data.events)
            if (event.id.equals(selectedEvent)) return event;
        return value.data.events.get(0);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        generation++;
        main.removeCallbacksAndMessages(null);
    }
}
