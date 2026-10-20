package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;

public class LoginViewModel extends AndroidViewModel {
    public final MutableLiveData<ScreenState<Boolean>> state = new MutableLiveData<>(ScreenState.idle());
    public String email = "", password = "";
    private final LoginFlow flow;
    private final AuthRepository auth;
    public boolean rememberMe;

    public LoginViewModel(Application application) {
        super(application);
        auth = AuthRepository.getInstance(application);
        rememberMe = auth.rememberMe();
        Handler main = new Handler(Looper.getMainLooper());
        flow = new LoginFlow(AuthRepository.getInstance(application), action -> main.post(action), value -> {
            if (value.status == ScreenState.Status.SUCCESS)
                auth.setRememberMe(rememberMe);
            state.setValue(value);
        }, () -> password = "", value -> Patterns.EMAIL_ADDRESS.matcher(value).matches());
    }

    public void restoreSession() {
        flow.restoreSession();
    }

    public void login() {
        flow.login(email, password);
    }

    public void submit() {
        if (!rememberMe) auth.setRememberMe(false);
        flow.submit(email, password);
    }

    public void route() {
        flow.route();
    }

    public void cancelPending() {
        flow.cancelPending();
    }

    public void consume() {
        flow.consume();
    }

    public void logout() {
        email = "";
        flow.logout();
    }

    @Override
    protected void onCleared() {
        password = "";
    }
}
