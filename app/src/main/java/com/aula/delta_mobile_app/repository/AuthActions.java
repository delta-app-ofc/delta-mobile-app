package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.Credentials;
import com.aula.delta_mobile_app.model.ScreenState;

import java.util.function.Consumer;

public interface AuthActions {
    boolean hasSession();

    boolean rememberMe();

    void refreshSession(Consumer<ScreenState<Boolean>> callback);

    void login(Credentials credentials, Consumer<ScreenState<Boolean>> callback);

    void route(Consumer<ScreenState<Boolean>> callback);

    void logout();
}
