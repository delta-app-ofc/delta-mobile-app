package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.services.api.ApiService;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.*;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import static org.junit.Assert.*;

public class UnitsRepositoryTest {
    private int addresses, properties, deletes;
    private boolean owned = true, failProperty, uncertainProperty;

    private UnitDraft draft() {
        UnitDraft draft = new UnitDraft();
        draft.name = "Nova unidade";
        draft.cep = draft.confirmedCep = "01001000";
        draft.city = "São Paulo";
        draft.state = "SP";
        draft.type = "PRÉDIO";
        draft.classification = "RESIDENCIAL_SOCIAL";
        return draft;
    }

    private UnitsRepository repository() {
        ApiService api = (ApiService) Proxy.newProxyInstance(ApiService.class.getClassLoader(), new Class<?>[] { ApiService.class }, (proxy, method, args) ->
            Proxy.newProxyInstance(Call.class.getClassLoader(), new Class<?>[] { Call.class }, (call, action, inputs) -> {
                if (!action.getName().equals("execute")) throw new AssertionError(action.getName());
                switch (method.getName()) {
                case "me":
                    Profile profile = new Profile(); profile.id = 7;
                    return Response.success(profile);
                case "createAddress":
                    addresses++;
                    AddressResponse address = new AddressResponse(); address.id = 25;
                    return Response.success(address);
                case "createProperty":
                    properties++;
                    if (uncertainProperty) throw new IOException("network");
                    if (failProperty) { failProperty = false; return Response.error(400, ResponseBody.create(MediaType.get("application/json"), "{}")); }
                    CreatePropertyRequest request = (CreatePropertyRequest) args[0];
                    assertEquals(Integer.valueOf(7), request.userId);
                    assertEquals(Integer.valueOf(25), request.addressId);
                    assertEquals("PRÉDIO", request.type);
                    assertEquals("RESIDENCIAL_SOCIAL", request.classification);
                    PropertyResponse property = new PropertyResponse(); property.id = 42; property.addressId = request.addressId;
                    property.name = request.name; property.type = request.type; property.classification = request.classification;
                    return Response.success(property);
                case "listUserProperties":
                    assertEquals(7, args[0]);
                    UserPropertyResponse link = new UserPropertyResponse(); link.id = 100; link.userId = 7; link.propertyId = 42;
                    return Response.success(owned ? Collections.singletonList(link) : Collections.emptyList());
                case "deleteProperty": deletes++; return Response.success(204, null);
                case "consumptionDaily": return Response.error(500, ResponseBody.create(MediaType.get("application/json"), "{}"));
                default: throw new AssertionError("Unexpected request: " + method.getName());
                }
            }));
        return new UnitsRepository(api);
    }

    @Test public void createsPropertyWithSelectedTypeCategoryAndAutomaticAssociation() throws Exception {
        ScreenState<PropertyResponse> result = repository().create(draft(), new UnitsRepository.Creation());
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertEquals(Integer.valueOf(42), result.data.id);
        assertEquals(1, addresses);
        assertEquals(1, properties);
    }

    @Test public void retryAfterRejectedPropertyReusesConfirmedAddress() throws Exception {
        failProperty = true;
        UnitsRepository repository = repository();
        UnitsRepository.Creation progress = new UnitsRepository.Creation();
        assertEquals(ScreenState.Status.ERROR, repository.create(draft(), progress).status);
        assertEquals(ScreenState.Status.SUCCESS, repository.create(draft(), progress).status);
        assertEquals(1, addresses);
        assertEquals(2, properties);
    }

    @Test public void uncertainPostIsNeverAutomaticallyRepeated() throws Exception {
        uncertainProperty = true;
        UnitsRepository repository = repository();
        UnitsRepository.Creation progress = new UnitsRepository.Creation();
        try { repository.create(draft(), progress); fail(); } catch (IOException expected) { }
        assertEquals(ScreenState.Status.ERROR, repository.create(draft(), progress).status);
        assertEquals(1, properties);
    }

    @Test public void deletionRequiresCurrentAccountAssociation() throws Exception {
        owned = false;
        UnitsRepository repository = repository();
        assertEquals(ScreenState.Status.ERROR, repository.delete(42).status);
        assertEquals(0, deletes);
        owned = true;
        assertEquals(ScreenState.Status.SUCCESS, repository.delete(42).status);
        assertEquals(1, deletes);
    }

    @Test public void missingConsumptionDoesNotBecomeZero() throws Exception {
        ScreenState<List<UnitSummary>> result = repository().load(LocalDate.of(2026, 11, 2));
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertNull(result.data.get(0).monthlyLiters);
    }

    @Test public void formRequiresConfirmedCepAndExplicitChoices() {
        UnitDraft draft = draft();
        assertNull(draft.validation());
        draft.confirmedCep = "";
        assertNotNull(draft.validation());
        draft.confirmedCep = draft.cep;
        draft.type = "";
        assertNotNull(draft.validation());
        draft.type = "CASA";
        draft.classification = "";
        assertNotNull(draft.validation());
    }

    @Test public void draftRestorationKeepsUserChoices() {
        UnitDraft original = draft();
        UnitDraft restored = UnitDraft.restore(original.values());
        assertEquals(original.type, restored.type);
        assertEquals(original.classification, restored.classification);
        assertEquals(original.cep, restored.confirmedCep);
        assertNull(restored.validation());
    }
}
