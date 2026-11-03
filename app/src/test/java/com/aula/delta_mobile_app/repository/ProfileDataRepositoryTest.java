package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import java.lang.reflect.Proxy;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import static org.junit.Assert.*;

public class ProfileDataRepositoryTest {
    private ProfileDataRepository repository(Response<Profile> response) {
        ApiService api = (ApiService) Proxy.newProxyInstance(ApiService.class.getClassLoader(), new Class<?>[] { ApiService.class }, (proxy, method, args) -> {
            assertEquals("me", method.getName());
            return Proxy.newProxyInstance(Call.class.getClassLoader(), new Class<?>[] { Call.class }, (call, operation, input) -> {
                assertEquals("execute", operation.getName());
                return response;
            });
        });
        return new ProfileDataRepository(api);
    }

    private Profile profile() {
        return GsonProvider.getInstance().fromJson("{\"id\":7,\"name\":\"Maria Silva\",\"email\":\"maria@example.com\",\"phone\":\"11999999999\",\"birthDate\":\"1996-02-29\"}", Profile.class);
    }

    @Test
    public void loadsAllPublishedProfileFields() throws Exception {
        ScreenState<Profile> result = repository(Response.success(profile())).load();
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertEquals("Maria Silva", result.data.name);
        assertEquals("maria@example.com", result.data.email);
        assertEquals("11999999999", result.data.phone);
        assertEquals("1996-02-29", result.data.birthDate);
    }

    @Test
    public void emptyResponseNeverProducesASuccessfulBlankForm() throws Exception {
        assertEquals(ScreenState.Status.ERROR, repository(Response.success(null)).load().status);
        assertEquals(ScreenState.Status.ERROR, repository(Response.success(new Profile())).load().status);
    }

    @Test
    public void partialIdentityIsAnErrorInsteadOfMissingUserData() throws Exception {
        Profile profile = profile();
        profile.name = " ";
        assertEquals(ScreenState.Status.ERROR, repository(Response.success(profile)).load().status);
        profile = profile();
        profile.email = null;
        assertEquals(ScreenState.Status.ERROR, repository(Response.success(profile)).load().status);
    }

    @Test
    public void absentOptionalFieldsDoNotEraseConfirmedNameAndEmail() throws Exception {
        Profile profile = profile();
        profile.phone = null;
        profile.birthDate = null;
        ScreenState<Profile> result = repository(Response.success(profile)).load();
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertEquals("Maria Silva", result.data.name);
    }

    @Test
    public void httpErrorsKeepTheirStatusAndActionableMessage() throws Exception {
        for (int status : new int[] { 401, 403, 429, 503 }) {
            ScreenState<Profile> result = repository(Response.error(status, ResponseBody.create(MediaType.get("application/json"), "{}"))).load();
            assertEquals(ScreenState.Status.ERROR, result.status);
            assertEquals(status, result.code);
            assertFalse(result.message.isEmpty());
        }
    }
}
