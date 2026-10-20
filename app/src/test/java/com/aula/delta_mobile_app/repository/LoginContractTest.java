package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.Credentials;
import com.aula.delta_mobile_app.model.AuthModels.Tokens;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.Test;

import retrofit2.Response;
import retrofit2.http.POST;

public class LoginContractTest {
    @Test
    public void refreshSendsOriginalRefreshTokenToRefreshEndpoint() throws Exception {
        assertEquals("delta/auth/refresh", ApiService.class.getMethod("refresh", com.aula.delta_mobile_app.model.AuthModels.Refresh.class).getAnnotation(POST.class).value());
        JsonObject body = new Gson().toJsonTree(new com.aula.delta_mobile_app.model.AuthModels.Refresh("original-refresh")).getAsJsonObject();
        assertEquals(1, body.size());
        assertEquals("original-refresh", body.get("refreshToken").getAsString());
    }

    @Test
    public void loginUsesExpectedEndpointAndCredentialKeys() throws Exception {
        assertEquals("delta/auth/login", ApiService.class.getMethod("login", Credentials.class).getAnnotation(POST.class).value());
        JsonObject body = new Gson().toJsonTree(new Credentials("user@example.com", "secret")).getAsJsonObject();
        assertEquals(2, body.size());
        assertTrue(body.has("email"));
        assertTrue(body.has("password"));
    }

    @Test
    public void tokenResponseMatchesPersistedSessionFields() {
        Tokens tokens = new Gson().fromJson("{\"accessToken\":\"access\",\"refreshToken\":\"refresh\",\"tokenType\":\"Bearer\",\"expiresIn\":900,\"refreshExpiresIn\":86400}",
            Tokens.class);
        assertEquals("access", tokens.accessToken);
        assertEquals("refresh", tokens.refreshToken);
        assertEquals("Bearer", tokens.tokenType);
        assertEquals(900, tokens.expiresIn);
        assertEquals(86400, tokens.refreshExpiresIn);
    }

    @Test
    public void unauthorizedResponseIsAnActionableLoginError() {
        ScreenState<Object> error = ApiErrors.from(Response.error(401, ResponseBody.create(MediaType.get("application/json"), "{}")));
        assertEquals(ScreenState.Status.ERROR, error.status);
        assertEquals(401, error.code);
        assertTrue(error.message.contains("Credenciais inválidas"));
    }

    @Test
    public void serverFailureDoesNotMasqueradeAsInvalidCredentials() {
        ScreenState<Object> error = ApiErrors.from(Response.error(503, ResponseBody.create(MediaType.get("application/json"), "{}")));
        assertEquals(503, error.code);
        assertTrue(error.message.contains("Serviço indisponível"));
    }
}
