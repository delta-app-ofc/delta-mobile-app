package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.session.SessionStore;

import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public final class SessionInterceptor implements Interceptor {
    private final SessionStore store;
    private final ApiService publicApi;
    private final HttpUrl base;
    private final Object refreshLock = new Object();
    private IOException lastFailure;
    private long failureUntil;
    private long failureGeneration = -1;

    public SessionInterceptor(SessionStore store, ApiService publicApi) {
        this.store = store;
        this.publicApi = publicApi;
        base = HttpUrl.get(NetworkConfig.BASE_URL);
    }

    public SessionInterceptor(SessionStore store, ApiService publicApi, String baseUrl) {
        this.store = store;
        this.publicApi = publicApi;
        base = HttpUrl.get(baseUrl);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!protectedRequest(request)) return chain.proceed(request);
        long generation = store.generation();
        String access = store.access();
        boolean renewed = access != null && store.accessExpiresAt() <= System.currentTimeMillis();
        if (renewed) access = renew(access, generation);
        Response response = chain.proceed(withToken(request, access));
        if (response.code() != 401 || access == null) return response;
        if (renewed) {
            if (store.generation() == generation) store.clear();
            return response;
        }
        String replacement;
        try {
            replacement = renew(access, generation);
        } catch (IOException failure) {
            response.close();
            throw failure;
        }
        if (replacement == null) return response;
        response.close();

        Response retried = chain.proceed(withToken(request, replacement));
        if (retried.code() == 401 && store.generation() == generation) store.clear();
        return retried;
    }

    private String renew(String previous, long expected) throws IOException {
        synchronized (refreshLock) {
            if (store.generation() != expected) return null;
            if (failureGeneration != expected) {
                lastFailure = null;
                failureUntil = 0;
                failureGeneration = expected;
            }
            if (!store.hasSession()) {
                store.clear();
                return null;
            }
            String current = store.access();
            if (!Objects.equals(previous, current)) return current;
            if (lastFailure != null && System.currentTimeMillis() < failureUntil) throw lastFailure;
            try {
                String original = store.refresh();
                if (original == null || store.generation() != expected) return null;
                retrofit2.Response<Tokens> result = publicApi.refresh(new Refresh(original)).execute();
                if (result.code() == 400 || result.code() == 401 || result.code() == 403) {
                    if (result.errorBody() != null) result.errorBody().close();
                    if (store.generation() == expected) store.clear();
                    return null;
                }
                if (!result.isSuccessful()) {
                    if (result.code() == 429)
                        failureUntil = com.aula.delta_mobile_app.services.network.ApiErrors.from(result).retryAt;
                    if (result.errorBody() != null) result.errorBody().close();
                    throw new IOException("Refresh temporarily unavailable");
                }
                if (!store.updateAccess(result.body(), expected)) return null;
                lastFailure = null;
                return store.access();
            } catch (Exception failure) {
                lastFailure = failure instanceof IOException ? (IOException) failure : new IOException("Refresh failed", failure);
                failureUntil = Math.max(failureUntil, System.currentTimeMillis() + 3000);
                throw lastFailure;
            }
        }
    }

    private Request withToken(Request request, String token) {
        Request.Builder builder = request.newBuilder().removeHeader("Authorization");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder.build();
    }

    private boolean protectedRequest(Request request) {
        HttpUrl url = request.url();
        if (!url.scheme().equals(base.scheme()) || !url.host().equals(base.host()) || url.port() != base.port()) return false;
        String path = url.encodedPath();
        if (!path.startsWith(base.encodedPath() + "delta/")) return false;
        return !(path.endsWith("/auth/login") || path.endsWith("/auth/create-account") || path.endsWith("/auth/register") || path.endsWith("/auth/refresh"));
    }
}
