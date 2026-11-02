package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.services.session.SessionStore;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class ChatbotAuthInterceptor implements Interceptor {
    private final SessionStore store;

    public ChatbotAuthInterceptor(SessionStore store) {
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String access = store.access();
        Request request = chain.request();
        if (access == null || access.isEmpty()) return chain.proceed(request);
        return chain.proceed(request.newBuilder().removeHeader("Authorization").header("Authorization", "Bearer " + access).build());
    }
}
