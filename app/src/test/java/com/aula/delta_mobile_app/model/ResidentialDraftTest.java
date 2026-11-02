package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.model.ApiModels.AddressResponse;
import com.aula.delta_mobile_app.model.ApiModels.UpdateAddressRequest;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResidentialDraftTest {
    private AddressResponse address() {
        AddressResponse value = new AddressResponse();
        value.id = 10;
        value.regionId = 5;
        value.cep = "06110160";
        value.city = "Osasco";
        value.state = "SP";
        value.latitude = new BigDecimal("-23.5");
        value.longitude = new BigDecimal("-46.7");
        return value;
    }

    @Test
    public void draftChangesDoNotModifyConfirmedAddress() {
        AddressResponse confirmed = address();
        ResidentialDraft draft = new ResidentialDraft(confirmed);
        assertFalse(draft.changed(confirmed));
        draft.city = "São Paulo";
        assertTrue(draft.changed(confirmed));
        assertEquals("Osasco", confirmed.city);
    }

    @Test
    public void retainsCoordinatesOnlyForUnchangedPostalCode() {
        AddressResponse confirmed = address();
        ResidentialDraft draft = new ResidentialDraft(confirmed);
        assertEquals(confirmed.latitude, draft.request(confirmed).latitude);
        draft.cep = "01001000";
        UpdateAddressRequest request = draft.request(confirmed);
        assertNull(request.latitude);
        assertNull(request.longitude);
    }

    @Test
    public void rejectsInvalidPostalCodeAndEmptyLocation() {
        ResidentialDraft draft = new ResidentialDraft(address());
        assertNull(draft.validation());
        draft.cep = "06110-160";
        assertNotNull(draft.validation());
        draft.cep = "06110160";
        draft.city = " ";
        assertNotNull(draft.validation());
        draft.city = "a".repeat(61);
        assertNotNull(draft.validation());
        draft.city = "Osasco";
        draft.state = "";
        assertNotNull(draft.validation());
    }

    @Test
    public void requestContainsOnlyFieldsSupportedByApi() {
        AddressResponse confirmed = address();
        ResidentialDraft draft = new ResidentialDraft(confirmed);
        com.google.gson.JsonObject json = GsonProvider.getInstance().toJsonTree(draft.request(confirmed)).getAsJsonObject();
        assertEquals(5, json.size());
        assertTrue(json.has("cep"));
        assertFalse(json.has("regionId"));
        assertFalse(json.has("number"));
        assertFalse(json.has("street"));
        assertFalse(json.has("userId"));
    }
}
