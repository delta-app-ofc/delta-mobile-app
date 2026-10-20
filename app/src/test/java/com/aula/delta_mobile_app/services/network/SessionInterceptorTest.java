package com.aula.delta_mobile_app.services.network;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.session.SessionStore;

import okhttp3.*;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionInterceptorTest {
    private static final String BASE = "https://delta.test/";

    private static class MemoryStore implements SessionStore {
        volatile String access = "expired", refresh = "original-refresh";
        volatile long generation, expires = Long.MAX_VALUE;

        @Override
        public long generation() {
            return generation;
        }

        @Override
        public boolean hasSession() {
            return refresh != null;
        }

        @Override
        public String access() {
            return access;
        }

        @Override
        public String refresh() {
            return refresh;
        }

        @Override
        public long accessExpiresAt() {
            return expires;
        }

        @Override
        public synchronized boolean updateAccess(Tokens tokens, long expected) {
            if (generation != expected) return false;
            access = tokens.accessToken;
            expires = Long.MAX_VALUE;
            return true;
        }

        @Override
        public synchronized void clear() {
            generation++;
            access = null;
            refresh = null;
        }
    }

    private interface Reply {
        retrofit2.Response<Tokens> execute(Refresh body) throws Exception;
    }

    private ApiService api(Reply reply) {
        return (ApiService)
        Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ApiService.class}, (proxy, method, args) -> {
            if (!method.getName().equals("refresh"))
                throw new AssertionError("Unexpected endpoint");
            Refresh body = (Refresh) args[0];
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {retrofit2.Call.class}, (p, m, a) -> {
                if (m.getName().equals("execute"))
                    return reply.execute(body);
                throw new AssertionError("Unexpected Call method");
            });
        });
    }

    private Tokens token() {
        Tokens value = new Tokens();
        value.accessToken = "renewed";
        value.tokenType = "Bearer";
        value.expiresIn = 900;
        return value;
    }

    private Interceptor.Chain chain(String path, AtomicInteger requests, boolean alwaysReject, CyclicBarrier barrier) {
        Request request = new Request.Builder().url(path).build();
        return (Interceptor.Chain)
        Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Interceptor.Chain.class}, (proxy, method, args) -> {
            if (method.getName().equals("request")) return request;
            if (method.getName().equals("proceed")) {
                Request sent = (Request) args[0];
                requests.incrementAndGet();
                boolean accepted = "Bearer renewed".equals(sent.header("Authorization"));
                if (!accepted && barrier != null)
                    barrier.await(5, TimeUnit.SECONDS);
                return new okhttp3.Response.Builder().request(sent).protocol(Protocol.HTTP_1_1).code(accepted && !alwaysReject ? 200 : 401).message("test").body(
                    ResponseBody.create(MediaType.get("application/json"), "{}")).build();
            }
            throw new AssertionError("Unexpected chain method");
        });
    }

    @Test
    public void concurrent401SharesOneRefreshAndSendsOriginal() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger refreshes = new AtomicInteger(), requests = new AtomicInteger();
        SessionInterceptor interceptor = new SessionInterceptor(store, api(body -> {
            assertEquals("original-refresh", body.refreshToken);
            refreshes.incrementAndGet();
            return retrofit2.Response.success(token());
        }), BASE);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CyclicBarrier barrier = new CyclicBarrier(3);
        try {
            java.util.List<Future<Integer>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++)
                futures.add(pool.submit(() -> {
                try (okhttp3.Response response = interceptor.intercept(chain(BASE + "delta/auth/me", requests, false, barrier))) {
                    return response.code();
                }
            }));
            for (Future<Integer> future : futures)
                assertEquals(200, (int) future.get(10, TimeUnit.SECONDS));
            assertEquals(1, refreshes.get());
            assertEquals(6, requests.get());
            assertEquals("original-refresh", store.refresh);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void rejectionRetriesOnceAndClearsSession() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger requests = new AtomicInteger();
        SessionInterceptor interceptor = new SessionInterceptor(store, api(body -> retrofit2.Response.success(token())), BASE);
        try (okhttp3.Response response = interceptor.intercept(chain(BASE + "delta/auth/me", requests, true, null))) {
            assertEquals(401, response.code());
        }
        assertEquals(2, requests.get());
        assertFalse(store.hasSession());
    }

    @Test
    public void networkFailurePreservesSession() throws Exception {
        MemoryStore store = new MemoryStore();
        SessionInterceptor interceptor = new SessionInterceptor(store, api(body -> {
            throw new IOException("offline");
        }), BASE);
        try {
            interceptor.intercept(chain(BASE + "delta/auth/me", new AtomicInteger(), false, null));
            fail();
        } catch (IOException expected) {
            assertTrue(store.hasSession());
            assertEquals("original-refresh", store.refresh);
        }
    }

    @Test
    public void logoutDuringRefreshCannotRestoreSession() throws Exception {
        MemoryStore store = new MemoryStore();
        SessionInterceptor interceptor = new SessionInterceptor(store, api(body -> {
            store.clear();
            return retrofit2.Response.success(token());
        }), BASE);
        try (okhttp3.Response ignored = interceptor.intercept(chain(BASE + "delta/auth/me", new AtomicInteger(), false, null))) {}
        assertFalse(store.hasSession());
        assertNull(store.access);
    }

    @Test
    public void publicAndForeignRequestsNeverGetBearerOrRefresh() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger refreshes = new AtomicInteger();
        SessionInterceptor interceptor = new SessionInterceptor(store, api(body -> {
            refreshes.incrementAndGet();
            return retrofit2.Response.success(token());
        }), BASE);
        for (String url : new String[] {
            "https://brasilapi.com.br/api/cep/v2/01001000", BASE + "delta/auth/login", BASE + "delta/auth/create-account", BASE + "delta/auth/refresh"
        }) {
            try (okhttp3.Response response = interceptor.intercept(chain(url, new AtomicInteger(), false, null))) {
                assertNull(response.request().header("Authorization"));
            }
        }
        assertEquals(0, refreshes.get());
    }
}
