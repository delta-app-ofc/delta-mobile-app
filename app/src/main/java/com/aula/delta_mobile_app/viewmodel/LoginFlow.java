package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.model.AuthModels.Credentials;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthActions;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class LoginFlow {
    private final AuthActions auth;
    private final Consumer<Runnable> dispatch;
    private final Consumer<ScreenState<Boolean>> observer;
    private final Runnable clearPassword;
    private final Predicate<String> validEmail;
    private ScreenState<Boolean> state = ScreenState.idle();
    private int epoch;
    private boolean checked, routing;

    public LoginFlow(AuthActions auth, Consumer<Runnable> dispatch, Consumer<ScreenState<Boolean>> observer, Runnable clearPassword, Predicate<String> validEmail) {
        this.auth = auth;
        this.dispatch = dispatch;
        this.observer = observer;
        this.clearPassword = clearPassword;
        this.validEmail = validEmail;
    }

    private void publish(ScreenState<Boolean> next) {
        state = next;
        observer.accept(next);
    }

    public void restoreSession() {
        if (checked) return;
        checked = true;
        if (auth.rememberMe() && auth.hasSession()) refreshAndRoute();
    }

    public void submit(String email, String password) {
        if (state.busy() || System.currentTimeMillis() < state.retryAt) return;
        if (auth.rememberMe() && auth.hasSession() && password.isEmpty()) {
            refreshAndRoute();
            return;
        }
        login(email, password);
    }

    public void login(String email, String password) {
        if (state.busy() || System.currentTimeMillis() < state.retryAt) return;
        if (!validEmail.test(email.trim()) || email.trim().length() > 255 || password.trim().isEmpty() || password.length() > 256) {
            publish(ScreenState.error("Informe e-mail válido e senha preenchida com até 256 caracteres."));
            return;
        }
        publish(ScreenState.loading());
        int attempt = epoch;
        auth.login(new Credentials(email.trim(), password), result -> dispatch.accept(() -> {
            if (attempt != epoch) return;
            if (result.status == ScreenState.Status.SUCCESS) {
                clearPassword.run();
                route();
            } else publish(result);
        }));
    }

    public void route() {
        if (routing || state.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < state.retryAt) return;
        routing = true;
        publish(ScreenState.loading());
        int attempt = epoch;
        auth.route(result -> dispatch.accept(() -> {
            if (attempt != epoch) return;
            routing = false;
            publish(result);
        }));
    }

    private void refreshAndRoute() {
        if (state.busy() || System.currentTimeMillis() < state.retryAt) return;
        publish(ScreenState.loading());
        int attempt = epoch;
        auth.refreshSession(result -> dispatch.accept(() -> {
            if (attempt != epoch) return;
            if (result.status == ScreenState.Status.SUCCESS) route();
            else publish(result);
        }));
    }

    public void cancelPending() {
        logout();
    }

    public void logout() {
        epoch++;
        routing = false;
        clearPassword.run();
        auth.logout();
        publish(ScreenState.idle());
    }

    public void consume() {
        publish(ScreenState.idle());
    }
}
