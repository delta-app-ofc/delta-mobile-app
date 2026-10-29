package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.ScreenState;

import java.util.function.Consumer;

public interface RoutineActions {
    void load(Consumer<ScreenState<RoutineData>> callback);
}
