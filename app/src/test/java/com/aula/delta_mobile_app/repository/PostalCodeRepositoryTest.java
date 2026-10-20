package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.PostalCodeResponse;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;

import org.junit.Test;

import retrofit2.*;

import java.lang.reflect.Proxy;
import java.util.*;

public class PostalCodeRepositoryTest {
    private static class Pending {
        Call<PostalCodeResponse> call;
        Callback<PostalCodeResponse> callback;
        boolean cancelled;
    }

    private ApiService api(List<Pending> calls) {
        return (ApiService)
        Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ApiService.class}, (proxy, method, args) -> {
            Pending pending = new Pending();
            calls.add(pending);
            @SuppressWarnings("unchecked")
            Call<PostalCodeResponse> call = (Call<PostalCodeResponse>)
            Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Call.class}, (p, m, a) -> {
                if (m.getName().equals("enqueue")) {
                    pending.callback = (Callback<PostalCodeResponse>)
                    a[0];
                    return null;
                }
                if (m.getName().equals("cancel")) {
                    pending.cancelled = true;
                    return null;
                }
                if (m.getName().equals("isCanceled"))
                    return pending.cancelled;
                throw new AssertionError("Unexpected call");
            });
            pending.call = call;
            return call;
        });
    }

    @Test
    public void changingCepCancelsAndIgnoresLateResponse() {
        List<Pending> calls = new ArrayList<>();
        List<ScreenState<PostalCodeResponse>> results = new ArrayList<>();
        PostalCodeRepository repository = new PostalCodeRepository(api(calls));
        repository.getCep("01001000", results::add);
        repository.getCep("16400000", results::add);
        assertTrue(calls.get(0).cancelled);
        PostalCodeResponse old = new PostalCodeResponse();
        old.state = "SP";
        old.city = "São Paulo";
        calls.get(0).callback.onResponse(calls.get(0).call, Response.success(old));
        assertTrue(results.isEmpty());
        PostalCodeResponse current = new PostalCodeResponse();
        current.state = "SP";
        current.city = "Lins";
        calls.get(1).callback.onResponse(calls.get(1).call, Response.success(current));
        assertEquals(1, results.size());
        assertEquals("Lins", results.get(0).data.city);
    }

    @Test
    public void incompleteResponseIsRecoverableError() {
        List<Pending> calls = new ArrayList<>();
        List<ScreenState<PostalCodeResponse>> results = new ArrayList<>();
        PostalCodeRepository repository = new PostalCodeRepository(api(calls));
        repository.getCep("01001000", results::add);
        calls.get(0).callback.onResponse(calls.get(0).call, Response.success(new PostalCodeResponse()));
        assertEquals(ScreenState.Status.ERROR, results.get(0).status);
        repository.getCep("01001000", results::add);
        assertEquals(2, calls.size());
    }
}
