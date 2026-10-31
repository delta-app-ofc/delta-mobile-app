package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import retrofit2.http.POST;

public class RegistrationContractTest {
    @Test
    public void endpointsMatchAccountCreationAndCompletionContract() throws Exception {
        assertEquals("delta/auth/create-account", ApiService.class.getMethod("create", CreateAccount.class).getAnnotation(POST.class).value());
        assertEquals("delta/auth/complete-registration", ApiService.class.getMethod("complete", Completion.class).getAnnotation(POST.class).value());
    }

    @Test
    public void accountRequestContainsRequiredCredentialAndPersonalKeys() {
        JsonObject body = new Gson().toJsonTree(new CreateAccount("Nome", "2000-02-29", "", "user@example.com", "password")).getAsJsonObject();
        for (String key : new String[] {"name", "birthDate", "phone", "email", "password"})
        assertTrue(body.has(key));
        assertEquals("2000-02-29", body.get("birthDate").getAsString());
        assertFalse(body.has("passwordConfirm"));
    }

    @Test
    public void completionIsOneNestedPayloadWithAnExplicitConfirmation() {
        Completion payload = new Completion();
        payload.address = new Address();
        payload.address.cep = "01001000";
        payload.property = new Property();
        payload.property.name = "Casa";
        JsonObject body = new Gson().toJsonTree(payload).getAsJsonObject();
        assertTrue(body.has("address"));
        assertTrue(body.has("property"));
        assertTrue(body.has("habits"));
        assertFalse(body.getAsJsonObject("address").has("regionId"));
        Completed result = new Gson().fromJson("{\"userId\":1,\"completed\":true}", Completed.class);
        assertTrue(result.completed);
        assertEquals(Integer.valueOf(1), result.userId);
    }

    @Test
    public void conflictOnlyCountsAsCompletedWhenUserHasAValidPropertyAssociation() {
        PropertyLink link = new PropertyLink();
        link.propertyId = 12;
        assertTrue(SignupCheck.confirmed(1, java.util.Arrays.asList(link)).completed);
        assertNull(SignupCheck.confirmed(1, java.util.Collections.emptyList()));
        assertNull(SignupCheck.confirmed(1, null));
        assertNull(SignupCheck.confirmed(null, java.util.Arrays.asList(link)));
        link.propertyId = null;
        assertNull(SignupCheck.confirmed(1, java.util.Arrays.asList(link)));
    }

    @Test
    public void conflictPreservesBusinessReasonFromServer() {
        retrofit2.Response<Object> response = retrofit2.Response.error(409, okhttp3.ResponseBody.create(okhttp3.MediaType.get("application/json"),
            "{\"message\":\"Este imóvel já está associado à conta.\"}"));
        assertEquals("Este imóvel já está associado à conta.", ApiErrors.from(response).message);
    }

    @Test
    public void genericConflictDoesNotClaimEmailWasDuplicated() {
        retrofit2.Response<Object> response = retrofit2.Response.error(409, okhttp3.ResponseBody.create(okhttp3.MediaType.get("application/json"), "{}"));
        assertFalse(ApiErrors.from(response).message.contains("e-mail"));
    }

    @Test
    public void sqlDetailsAreNotDisplayedAsBusinessMessages() {
        retrofit2.Response<Object> response = retrofit2.Response.error(409, okhttp3.ResponseBody.create(okhttp3.MediaType.get("application/json"),
            "{\"message\":\"duplicate key violates unique constraint" + " address_cep\"}"));
        assertFalse(ApiErrors.from(response).message.contains("address_cep"));
    }
}
