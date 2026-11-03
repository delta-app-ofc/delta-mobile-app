package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.services.api.ApiService;
import java.io.IOException;
import java.math.BigDecimal;
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
    private boolean wrongProperty, wrongAddress, missingAddress;
    private List<ConsumptionDailyResponse> readings;
    private boolean rejectAddress;

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
                    CreateAddressRequest addressRequest = (CreateAddressRequest) args[0];
                    assertEquals("01001000", addressRequest.cep);
                    assertEquals("São Paulo", addressRequest.city);
                    assertEquals("SP", addressRequest.state);
                    if (rejectAddress) return Response.error(404, ResponseBody.create(MediaType.get("application/json"), "{}"));
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
                case "getProperty":
                    assertEquals(42, args[0]);
                    PropertyResponse residence = new PropertyResponse(); residence.id = wrongProperty ? 99 : 42; residence.addressId = 25;
                    return Response.success(residence);
                case "getAddress":
                    assertFalse(wrongProperty);
                    assertEquals(25, args[0]);
                    if (missingAddress) return Response.error(500, ResponseBody.create(MediaType.get("application/json"), "{}"));
                    AddressResponse location = new AddressResponse(); location.id = wrongAddress ? 99 : 25; location.city = "São Paulo"; location.state = "SP";
                    return Response.success(location);
                case "deleteProperty": deletes++; return Response.success(204, null);
                case "consumptionDaily": return readings == null ? Response.error(500, ResponseBody.create(MediaType.get("application/json"), "{}")) : Response.success(readings);
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

    @Test public void addressFailureIdentifiesStageWithoutCreatingProperty() throws Exception {
        rejectAddress = true;
        ScreenState<PropertyResponse> result = repository().create(draft(), new UnitsRepository.Creation());
        assertEquals(ScreenState.Status.ERROR, result.status);
        assertEquals(404, result.code);
        assertTrue(result.message.startsWith("Não foi possível cadastrar o endereço."));
        assertEquals(0, properties);
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

    @Test public void residenceShowsItsConfirmedAddress() throws Exception {
        UnitSummary residence = repository().load(LocalDate.of(2026, 11, 2)).data.get(0);
        assertEquals("São Paulo", residence.address.city);
        assertEquals("SP", residence.address.state);
    }

    @Test public void consumptionDistinguishesMissingReadingsFromConfirmedZero() throws Exception {
        readings = Collections.emptyList();
        assertNull(repository().load(LocalDate.of(2026, 11, 2)).data.get(0).monthlyLiters);
        ConsumptionDailyResponse own = new ConsumptionDailyResponse(); own.propertyId = 42; own.fullDate = "2026-11-02"; own.totalLiters = BigDecimal.ZERO;
        ConsumptionDailyResponse foreign = new ConsumptionDailyResponse(); foreign.propertyId = 99; foreign.fullDate = "2026-11-02"; foreign.totalLiters = new BigDecimal("200");
        ConsumptionDailyResponse previous = new ConsumptionDailyResponse(); previous.propertyId = 42; previous.fullDate = "2026-10-31"; previous.totalLiters = new BigDecimal("300");
        readings = Arrays.asList(own, foreign, previous);
        assertEquals(0, BigDecimal.ZERO.compareTo(repository().load(LocalDate.of(2026, 11, 2)).data.get(0).monthlyLiters));
        own.totalLiters = new BigDecimal("125.50");
        assertEquals(0, new BigDecimal("125.50").compareTo(repository().load(LocalDate.of(2026, 11, 2)).data.get(0).monthlyLiters));
    }

    @Test public void addressFromAnotherPropertyIsNotDisplayed() throws Exception {
        wrongProperty = true;
        assertNull(repository().load(LocalDate.of(2026, 11, 2)).data.get(0).address);
        wrongProperty = false;
        wrongAddress = true;
        assertNull(repository().load(LocalDate.of(2026, 11, 2)).data.get(0).address);
    }

    @Test public void unavailableAddressDoesNotHideResidence() throws Exception {
        missingAddress = true;
        ScreenState<List<UnitSummary>> result = repository().load(LocalDate.of(2026, 11, 2));
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertEquals(Integer.valueOf(42), result.data.get(0).property.propertyId);
        assertNull(result.data.get(0).address);
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
