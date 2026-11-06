package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.model.AuthModels.Refresh;
import com.aula.delta_mobile_app.model.AuthModels.Tokens;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.session.SessionStore;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class ChatbotAuthInterceptor implements Interceptor {
    private final SessionStore store;
    private final ApiService api;
    private final Object refreshLock = new Object();

    public ChatbotAuthInterceptor(SessionStore store, ApiService api) {
        this.store = Objects.requireNonNull(store);
        this.api = Objects.requireNonNull(api);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long generation = store.generation();
        String access = store.access();
        boolean renewed = access != null && store.accessExpiresAt() <= System.currentTimeMillis();
        if (renewed) access = renew(access, generation);
        Response response = chain.proceed(withToken(request, access));
        if (response.code() != 401 || access == null || renewed) return response;
        String replacement;
        try {
            replacement = renew(access, generation);
        } catch (IOException failure) {
            response.close();
            throw failure;
        }
        if (replacement == null) return response;
        response.close();
        return chain.proceed(withToken(request, replacement));
    }

    private String renew(String previous, long expected) throws IOException {
        synchronized (refreshLock) {
            if (store.generation() != expected || !store.hasSession()) return null;
            String current = store.access();
            if (!Objects.equals(previous, current)) return current;
            String refresh = store.refresh();
            if (refresh == null || store.generation() != expected) return null;
            try {
                retrofit2.Response<Tokens> result = api.refresh(new Refresh(refresh)).execute();
                if (result.code() == 400 || result.code() == 401 || result.code() == 403) {
                    if (result.errorBody() != null) result.errorBody().close();
                    if (store.generation() == expected) store.clear();
                    return null;
                }
                if (!result.isSuccessful()) {
                    if (result.errorBody() != null) result.errorBody().close();
                    throw new IOException("Session refresh temporarily unavailable");
                }
                if (!store.updateAccess(result.body(), expected)) return null;
                return store.access();
            } catch (IOException failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IOException("Session refresh failed", failure);
            }
        }
    }

    private Request withToken(Request request, String token) {
        Request.Builder builder = request.newBuilder().removeHeader("Authorization");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder.build();
    }
}
