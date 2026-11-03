package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.MonthlyGoal;
import com.aula.delta_mobile_app.model.MonthlyGoalContext;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import static org.junit.Assert.*;

public class MonthlyGoalsRepositoryTest {
    private final Map<String, String> values = new HashMap<>();
    private final YearMonth month = YearMonth.of(2026, 11);
    private Profile profile;
    private List<UserPropertyResponse> links;

    private MonthlyGoalsRepository repository() {
        profile = new Profile();
        profile.id = 7;
        UserPropertyResponse owned = new UserPropertyResponse();
        owned.userId = 7;
        owned.propertyId = 10;
        links = Collections.singletonList(owned);
        ApiService api = (ApiService) Proxy.newProxyInstance(ApiService.class.getClassLoader(), new Class<?>[] { ApiService.class }, (proxy, method, args) -> {
            if (method.getName().equals("listUserProperties")) assertEquals(profile.id, args[0]);
            return Proxy.newProxyInstance(Call.class.getClassLoader(), new Class<?>[] { Call.class }, (call, operation, input) -> {
                if (!operation.getName().equals("execute")) throw new AssertionError(operation.getName());
                if (method.getName().equals("me")) return Response.success(profile);
                if (method.getName().equals("listUserProperties")) return Response.success(links);
                throw new AssertionError(method.getName());
            });
        });
        MonthlyGoalStorage storage = new MonthlyGoalStorage(new MonthlyGoalStorage.Store() {
            @Override public String read(String key) { return values.get(key); }
            @Override public boolean write(String key, String value) { values.put(key, value); return true; }
        });
        return new MonthlyGoalsRepository(api, storage, Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    public void loadsMissingGoalThenPersistsAndConfirmsEdit() throws Exception {
        MonthlyGoalsRepository repository = repository();
        ScreenState<MonthlyGoalContext> initial = repository.load(10, month);
        assertEquals(ScreenState.Status.SUCCESS, initial.status);
        assertNull(initial.data.liters);
        assertEquals(Integer.valueOf(10), initial.data.property.propertyId);
        ScreenState<MonthlyGoalContext> saved = repository.save(initial.data, "1234,56");
        assertEquals(ScreenState.Status.SUCCESS, saved.status);
        assertEquals(0, new BigDecimal("1234.56").compareTo(repository.load(10, month).data.liters));
        assertNull(repository.load(10, month.plusMonths(1)).data.liters);
        assertEquals(ScreenState.Status.SUCCESS, repository.save(saved.data, "2000").status);
        assertEquals(0, new BigDecimal("2000").compareTo(repository.load(10, month).data.liters));
    }

    @Test
    public void identityAndPropertyAssociationAreRequired() throws Exception {
        MonthlyGoalsRepository repository = repository();
        assertEquals(ScreenState.Status.ERROR, repository.load(null, month).status);
        assertEquals(ScreenState.Status.ERROR, repository.load(99, month).status);
        links.get(0).userId = 8;
        assertEquals(ScreenState.Status.ERROR, repository.load(10, month).status);
        profile.id = null;
        assertEquals(ScreenState.Status.ERROR, repository.load(null, month).status);
        assertTrue(values.isEmpty());
    }

    @Test
    public void revokedAssociationCannotBeSaved() throws Exception {
        MonthlyGoalsRepository repository = repository();
        MonthlyGoalContext original = repository.load(10, month).data;
        links = Collections.emptyList();
        assertEquals(ScreenState.Status.ERROR, repository.save(original, "100").status);
        assertTrue(values.isEmpty());
    }

    @Test
    public void updatedGoalIsNotSilentlyOverwritten() throws Exception {
        MonthlyGoalsRepository repository = repository();
        MonthlyGoalContext original = repository.load(10, month).data;
        values.put(MonthlyGoal.key(7, 10, month), "200");
        assertEquals(ScreenState.Status.ERROR, repository.save(original, "100").status);
        assertEquals("200", values.get(MonthlyGoal.key(7, 10, month)));
    }

    @Test
    public void accountChangeCannotWriteIntoAnotherAccountsScope() throws Exception {
        MonthlyGoalsRepository repository = repository();
        MonthlyGoalContext original = repository.load(10, month).data;
        profile.id = 8;
        links.get(0).userId = 8;
        assertEquals(ScreenState.Status.ERROR, repository.save(original, "100").status);
        assertTrue(values.isEmpty());
    }

    @Test
    public void corruptLocalValueAndMissingResponseAreExplicitErrors() throws Exception {
        MonthlyGoalsRepository repository = repository();
        values.put(MonthlyGoal.key(7, 10, month), "invalid");
        assertEquals(ScreenState.Status.ERROR, repository.load(10, month).status);
        links = null;
        assertEquals(ScreenState.Status.ERROR, repository.load(10, month).status);
    }

    @Test
    public void pastMonthsCanBeReadButCannotBeCreatedOrEdited() throws Exception {
        MonthlyGoalsRepository repository = repository();
        YearMonth past = month.minusMonths(1);
        MonthlyGoalContext missing = repository.load(10, past).data;
        assertNull(missing.liters);
        assertEquals(ScreenState.Status.ERROR, repository.save(missing, "100").status);
        assertTrue(values.isEmpty());
        values.put(MonthlyGoal.key(7, 10, past), "250");
        MonthlyGoalContext existing = repository.load(10, past).data;
        assertEquals(0, new BigDecimal("250").compareTo(existing.liters));
        assertEquals(ScreenState.Status.ERROR, repository.save(existing, "500").status);
        assertEquals("250", values.get(MonthlyGoal.key(7, 10, past)));
    }

    @Test
    public void futureMonthsKeepTheirOwnGoalAndReferenceTheCurrentMonth() throws Exception {
        MonthlyGoalsRepository repository = repository();
        values.put(MonthlyGoal.key(7, 10, month), "99200");
        values.put(MonthlyGoal.key(8, 10, month), "800");
        values.put(MonthlyGoal.key(7, 99, month), "900");
        for (YearMonth future : Arrays.asList(month.plusMonths(1), month.plusMonths(2))) {
            MonthlyGoalContext initial = repository.load(10, future).data;
            assertNull(initial.liters);
            assertEquals(0, new BigDecimal("99200").compareTo(initial.currentMonthLiters));
            ScreenState<MonthlyGoalContext> saved = repository.save(initial, "50000");
            assertEquals(ScreenState.Status.SUCCESS, saved.status);
            assertEquals(0, new BigDecimal("50000").compareTo(saved.data.liters));
            assertEquals(0, new BigDecimal("99200").compareTo(saved.data.currentMonthLiters));
            assertEquals(ScreenState.Status.SUCCESS, repository.save(saved.data, "60000").status);
        }
        assertEquals("99200", values.get(MonthlyGoal.key(7, 10, month)));
    }

    @Test
    public void futureReferenceIsMissingUntilTheCurrentMonthHasAGoal() throws Exception {
        MonthlyGoalsRepository repository = repository();
        YearMonth future = month.plusMonths(1);
        assertNull(repository.load(10, future).data.currentMonthLiters);
        repository.save(repository.load(10, month).data, "200");
        assertEquals(0, new BigDecimal("200").compareTo(repository.load(10, future).data.currentMonthLiters));
    }
}
