package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.ScreenState;
import java.util.function.Consumer;

public interface RoutineActions {
    default void create(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
        callback.accept(ScreenState.error("Criação indisponível nesta fonte.", 405, 0));
    }

    default void update(RoutineFormState form, Consumer<ScreenState<RoutineData.Habit>> callback) {
        callback.accept(ScreenState.error("Edição indisponível nesta fonte.", 405, 0));
    }

    default void delete(int id, int userId, Consumer<ScreenState<Void>> callback) {
        callback.accept(ScreenState.error("Exclusão indisponível nesta fonte.", 405, 0));
    }

    void load(Consumer<ScreenState<RoutineData>> callback);

    void detail(int id, Consumer<ScreenState<RoutineData.Habit>> callback);
}
