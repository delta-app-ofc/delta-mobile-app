package com.aula.delta_mobile_app.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.aula.delta_mobile_app.services.network.RetrofitClient;
import com.aula.delta_mobile_app.services.session.TokenStore;

import retrofit2.Call;
import retrofit2.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AuthRepository implements AuthActions, SignupActions {
    private static AuthRepository instance;
    private final TokenStore store;
    private final ApiService api;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> session = new MutableLiveData<>(false);
    private Profile profile;
    private boolean authenticating;
    private final android.content.SharedPreferences loginPreferences;

    private AuthRepository(Context context) {
        loginPreferences = context.getApplicationContext().getSharedPreferences("delta_login_preferences", Context.MODE_PRIVATE);
        store = new TokenStore(context);
        api = RetrofitClient.createApiService(store);
    }

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) instance = new AuthRepository(context.getApplicationContext());
        return instance;
    }

    public LiveData<Boolean> session() {
        return session;
    }

    public ApiService api() {
        return api;
    }

    public <T> void resource(java.util.concurrent.Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback) {
        long expected = store.generation();
        worker.execute(() -> {
            ScreenState<T> result;
            try {
                if (!store.hasSession())
                    result = ScreenState.error("Entre novamente para continuar.", 401, 0);
                else result = operation.call();
                if (expected != store.generation())
                    result = ScreenState.error("Sessão alterada. Entre novamente.", 401, 0);
            } catch (Exception failure) {
                result = ApiErrors.network();
            }
            notifyInvalid();
            callback.accept(result);
        });
    }

    public Profile currentProfile() throws java.io.IOException {
        Response<Profile> response = api.me().execute();
        if (!response.isSuccessful()) {
            if (response.errorBody() != null) response.errorBody().close();
            throw new java.io.IOException("Perfil indisponível");
        }
        Profile value = response.body();
        if (value == null || value.id == null || value.id <= 0)
            throw new java.io.IOException("Perfil inválido");
        return value;
    }

    public synchronized boolean hasSession() {
        return store.hasSession();
    }

    public boolean rememberMe() {
        return loginPreferences.getBoolean("remember_me", false);
    }

    public void setRememberMe(boolean remember) {
        loginPreferences.edit().putBoolean("remember_me", remember).apply();
    }

    public void refreshSession(Consumer<ScreenState<Boolean>> callback) {
        long expected = store.generation();
        worker.execute(() -> {
            try {
                String token = store.refresh();
                if (!store.hasSession() || token == null) {
                    store.clear();
                    setRememberMe(false);
                    session.postValue(false);
                    callback.accept(ScreenState.error("Sua sessão expirou. Entre com e-mail e senha.", 401, 0));
                    return;
                }
                Response<Tokens> response = api.refresh(new Refresh(token)).execute();
                if (expected != store.generation()) {
                    callback.accept(ScreenState.error("Autenticação cancelada."));
                    return;
                }
                if (!response.isSuccessful()) {
                    ScreenState<Boolean> error = ApiErrors.from(response);
                    if (response.code() == 400 || response.code() == 401 || response.code() == 403) {
                        store.clear();
                        setRememberMe(false);
                        session.postValue(false);
                    }
                    callback.accept(error);
                    return;
                }
                Tokens tokens = response.body();
                boolean saved = tokens != null && tokens.refreshToken != null && tokens.refreshExpiresIn > 0 ? store.save(tokens, expected) : store.updateAccess(tokens, expected);
                if (!saved) {
                    callback.accept(ScreenState.error("Autenticação cancelada."));
                    return;
                }
                session.postValue(true);
                callback.accept(ScreenState.success(true));
            } catch (java.io.IOException unavailable) {
                callback.accept(ApiErrors.network());
            } catch (Exception invalid) {
                callback.accept(ScreenState.error("Não foi possível renovar sua sessão. Entre novamente."));
            }
        });
    }

    public void create(CreateAccount request, Consumer<ScreenState<Boolean>> callback) {
        authenticate(api.create(request), request, callback);
    }

    public void login(Credentials request, Consumer<ScreenState<Boolean>> callback) {
        authenticate(api.login(request), request, callback);
    }

    private synchronized void authenticate(Call<Tokens> call, Credentials request, Consumer<ScreenState<Boolean>> callback) {
        if (authenticating) {
            request.password = null;
            callback.accept(ScreenState.error("Aguarde a autenticação em andamento."));
            return;
        }
        authenticating = true;
        long expected = store.generation();
        worker.execute(() -> {
            ScreenState<Boolean> result;
            try {
                Response<Tokens> response = call.execute();
                if (!response.isSuccessful()) result = ApiErrors.from(response);
                else if (!store.save(response.body(), expected))
                    result = ScreenState.error("Autenticação cancelada.");
                else {
                    profile = null;
                    session.postValue(true);
                    result = ScreenState.success(true);
                }
            } catch (java.io.IOException failure) {
                result = ApiErrors.network();
            } catch (Exception failure) {
                result = ScreenState.error("Não foi possível guardar a sessão com segurança. Entre" + " novamente.");
            } finally {
                request.password = null;
                synchronized (this) {
                    authenticating = false;
                }
            }
            callback.accept(result);
        });
    }

    public void route(Consumer<ScreenState<Boolean>> callback) {
        long expected = store.generation();
        worker.execute(() -> {
            if (!store.hasSession()) {
                store.clear();
                session.postValue(false);
                callback.accept(ScreenState.error("Entre para continuar.", 401, 0));
                return;
            }
            try {
                Response<Profile> me = api.me().execute();
                if (expected != store.generation()) {
                    notifyInvalid();
                    callback.accept(ScreenState.error("Sessão alterada. Entre novamente.", 401, 0));
                    return;
                }
                if (!me.isSuccessful()) {
                    notifyInvalid();
                    callback.accept(ApiErrors.from(me));
                    return;
                }
                Profile value = me.body();
                if (value == null || value.id == null || value.id <= 0) {
                    callback.accept(ScreenState.error("Perfil indisponível. Tente novamente."));
                    return;
                }
                Response<java.util.List<PropertyLink>> links = api.properties(value.id).execute();
                if (expected != store.generation()) {
                    notifyInvalid();
                    callback.accept(ScreenState.error("Sessão alterada. Entre novamente.", 401, 0));
                    return;
                }
                if (!links.isSuccessful()) {
                    notifyInvalid();
                    callback.accept(ApiErrors.from(links));
                    return;
                }
                if (links.body() == null) {
                    callback.accept(ScreenState.error("Não foi possível verificar o cadastro."));
                    return;
                }
                store.saveProfile(value, expected);
                profile = value;
                session.postValue(true);
                callback.accept(ScreenState.success(!links.body().isEmpty()));
            } catch (Exception failure) {
                notifyInvalid();
                callback.accept(ApiErrors.network());
            }
        });
    }

    public void complete(Completion request, Consumer<ScreenState<Completed>> callback) {
        long expected = store.generation();
        worker.execute(() -> {
            if (!store.hasSession()) {
                notifyInvalid();
                callback.accept(ScreenState.error("Entre novamente para concluir o cadastro.", 401, 0));
                return;
            }
            try {
                Response<Completed> response = api.complete(request).execute();
                if (expected != store.generation()) {
                    callback.accept(ScreenState.error("Sessão alterada. Entre novamente.", 401, 0));
                    return;
                }
                if (!response.isSuccessful()) {
                    ScreenState<Completed> error = ApiErrors.from(response);
                    if (response.code() == 409) {
                        try {
                            Response<Profile> me = api.me().execute();
                            if (me.isSuccessful() && me.body() != null && me.body().id != null) {
                                Response<java.util.List<PropertyLink>> links = api.properties(me.body().id).execute();
                                if (links.isSuccessful() && expected == store.generation()) {
                                    Completed confirmed = SignupCheck.confirmed(me.body().id, links.body());
                                    if (confirmed != null) {
                                        callback.accept(ScreenState.success(confirmed));
                                        return;
                                    }
                                } else if (links.errorBody() != null)
                                links.errorBody().close();
                            } else if (me.errorBody() != null) me.errorBody().close();
                        } catch (Exception unavailable) {
                        }
                        if (expected != store.generation()) {
                            notifyInvalid();
                            callback.accept(ScreenState.error("Sessão alterada. Entre novamente.", 401, 0));
                            return;
                        }
                    }
                    notifyInvalid();
                    callback.accept(error);
                } else if (response.body() == null || !response.body().completed)
                callback.accept(ScreenState.error("O servidor não confirmou a conclusão."));
                else callback.accept(ScreenState.success(response.body()));
            } catch (Exception failure) {
                notifyInvalid();
                callback.accept(ApiErrors.network());
            }
        });
    }

    public void logout() {
        store.clear();
        setRememberMe(false);
        profile = null;
        session.setValue(false);
    }

    private void notifyInvalid() {
        if (!store.hasSession()) {
            store.clear();
            profile = null;
            session.postValue(false);
        }
    }
}
