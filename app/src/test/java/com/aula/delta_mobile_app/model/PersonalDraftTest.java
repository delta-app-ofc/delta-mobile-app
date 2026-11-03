package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ApiModels.UpdateProfileRequest;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import org.junit.Test;
import retrofit2.http.PUT;
import static org.junit.Assert.*;

public class PersonalDraftTest {
    @Test
    public void phoneMaskDoesNotChangePayloadOrMarkDraftAsChanged() {
        Profile original = profile();
        original.phone = "11987065088";
        PersonalDraft draft = new PersonalDraft(original);
        draft.phone = "(11) 98706-5088";
        assertNull(draft.error("phone"));
        assertEquals(original.phone, draft.request().phone);
        assertFalse(draft.changed(original));
        original.phone = "(11) 98706-5088";
        assertFalse(draft.changed(original));
    }

    private Profile profile() {
        return GsonProvider.getInstance().fromJson("{\"id\":7,\"name\":\"Maria Silva\",\"email\":\"maria@example.com\",\"phone\":\"11999999999\",\"birthDate\":\"1996-02-29\"}", Profile.class);
    }

    @Test
    public void loadsAllPublishedFieldsWithoutChangingConfirmedProfile() {
        Profile profile = profile();
        PersonalDraft draft = new PersonalDraft(profile);
        assertEquals("11999999999", draft.phone);
        assertEquals("1996-02-29", draft.birthDate);
        assertNull(draft.validation());
        assertFalse(draft.changed(profile));
        draft.name = "Outro nome";
        assertTrue(draft.changed(profile));
        assertEquals("Maria Silva", profile.name);
    }

    @Test
    public void fullUpdatePreservesBirthDateAndCanRemoveOptionalPhone() throws Exception {
        PersonalDraft draft = new PersonalDraft(profile());
        draft.phone = " ";
        UpdateProfileRequest request = draft.request();
        assertNull(request.phone);
        assertEquals("1996-02-29", request.birthDate);
        assertEquals("Maria Silva", request.name);
        assertEquals("maria@example.com", request.email);
        assertFalse(GsonProvider.getInstance().toJsonTree(request).getAsJsonObject().has("password"));
        assertEquals("delta/auth/me", ApiService.class.getMethod("updateProfile", UpdateProfileRequest.class).getAnnotation(PUT.class).value());
    }

    @Test
    public void rejectsInvalidDatesAndFutureBirthDate() {
        PersonalDraft draft = new PersonalDraft(profile());
        for (String date : new String[] { "2023-02-29", "29/02/1996", "", java.time.LocalDate.now().plusDays(1).toString() }) {
            draft.birthDate = date;
            assertNotNull(draft.validation());
        }
    }

    @Test
    public void rejectsInvalidContactFields() {
        PersonalDraft draft = new PersonalDraft(profile());
        draft.email = "sem-email";
        assertNotNull(draft.validation());
        draft.email = "maria@example.com";
        draft.phone = "1".repeat(16);
        assertNotNull(draft.validation());
        draft.phone = "";
        draft.name = " ";
        assertNotNull(draft.validation());
    }
}
