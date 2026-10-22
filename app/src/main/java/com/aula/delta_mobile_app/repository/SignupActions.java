package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.ScreenState;

import java.util.function.Consumer;

public interface SignupActions {
    boolean hasSession();

    void create(CreateAccount request, Consumer<ScreenState<Boolean>> callback);

    void complete(Completion request, Consumer<ScreenState<Completed>> callback);
}
