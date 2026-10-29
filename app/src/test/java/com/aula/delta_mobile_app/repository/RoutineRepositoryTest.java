package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.aula.delta_mobile_app.services.network.GsonProvider;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RoutineRepositoryTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T12:00:00Z"), ZoneOffset.UTC);
    private static final String HABIT = "{\"id\":105,\"userId\":73,\"habitId\":1,\"habitName\":\"Banho longo\","
        + "\"habitCategory\":\"Higiene\",\"frequency\":5,\"daysOfWeek\":[1,3],"
        + "\"schedule\":[{\"dayOfWeek\":1,\"startTimes\":[\"07:00\",\"20:00\"]},"
        + "{\"dayOfWeek\":3,\"startTimes\":[\"08:00\",\"14:00\",\"21:00\"]}]}";
    private final Access access = new Access();
    private final RoutineActions repository = new RoutineRepository(access, CLOCK);

    private static final class Result<T> implements Consumer<ScreenState<T>> {
        final List<ScreenState<T>> values = new ArrayList<>();

        @Override
        public void accept(ScreenState<T> value) {
            values.add(value);
        }

        ScreenState<T> only() {
            assertEquals(1, values.size());
            return values.get(0);
        }
    }

    private static final class Access implements RoutineRepository.Access {
        final List<Request> requests = new ArrayList<>();
        final List<Runnable> work = new ArrayList<>();
        final ApiService service;
        Integer userId = 73;
        int code = 200, profileReads, resources;
        String body = "[]", retryAfter;
        IOException networkFailure, profileFailure;
        boolean missingProfile, blocked, running;

        Access() {
            OkHttpClient client = new OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor(chain -> {
                assertTrue(running);
                requests.add(chain.request());
                if (networkFailure != null) throw networkFailure;
                okhttp3.Response.Builder response = new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
                    .body(ResponseBody.create(MediaType.get("application/json"), body));
                if (retryAfter != null) response.header("Retry-After", retryAfter);
                return response.build();
            }).build();
            service = new Retrofit.Builder().baseUrl("https://delta.test/").client(client).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance()))
                .build().create(ApiService.class);
        }

        @Override
        public ApiService api() {
            assertTrue(running);
            return service;
        }

        @Override
        public Profile currentProfile() throws IOException {
            assertTrue(running);
            profileReads++;
            if (profileFailure != null) throw profileFailure;
            if (missingProfile) return null;
            Profile profile = new Profile();
            profile.id = userId;
            return profile;
        }

        @Override
        public <T> void resource(Callable<ScreenState<T>> operation, Consumer<ScreenState<T>> callback) {
            resources++;
            work.add(() -> {
                running = true;
                try {
                    callback.accept(blocked ? ScreenState.error("Sessão indisponível.", 401, 123) : operation.call());
                } catch (Exception unexpected) {
                    throw new AssertionError(unexpected);
                } finally {
                    running = false;
                }
            });
        }

        void run() {
            assertFalse(work.isEmpty());
            work.remove(0).run();
        }
    }

    @Test
    public void collectionIsDeferredToAuthenticatedWorkAndUsesCurrentAccountPath() {
        access.body = "[" + HABIT + "]";
        Result<RoutineData> result = new Result<>();
        repository.load(result);
        assertEquals(1, access.resources);
        assertEquals(0, access.profileReads);
        assertTrue(access.requests.isEmpty());
        assertTrue(result.values.isEmpty());
        access.run();
        ScreenState<RoutineData> state = result.only();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertEquals(1, access.profileReads);
        assertRequest(0, "/delta/habits/73");
        assertEquals(1, access.requests.size());
        assertEquals(LocalDate.of(2026, 10, 29), state.data.reference);
        assertEquals(105, state.data.habits.get(0).id);
        assertEquals(73, state.data.habits.get(0).userId);
        assertTrue(state.data.habits.get(0).scheduleComplete);
        assertEquals("Higiene", state.data.habits.get(0).category);
    }

    @Test
    public void successfulEmptyCollectionIsNotAnErrorOrMissingBody() {
        ScreenState<RoutineData> state = load();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertTrue(state.data.habits.isEmpty());
        assertTrue(state.data.day(LocalDate.of(2026, 10, 29)).confirmedEmpty());
    }

    @Test
    public void nullOrAbsentCollectionBodyNeverBecomesAnEmptyAgenda() {
        access.body = "null";
        assertError(load(), 0);
        access.code = 204;
        access.body = "";
        assertError(load(), 0);
    }

    @Test
    public void collectionRejectsOtherOwnersWithoutPublishingAPartialList() {
        access.body = "[" + HABIT + "," + HABIT.replace("\"id\":105", "\"id\":106").replace("\"userId\":73", "\"userId\":74") + "]";
        assertError(load(), 0);
    }

    @Test
    public void collectionRejectsUnidentifiableOrRepeatedRecords() {
        for (String item : new String[] {"null", "{}", HABIT.replace("\"id\":105,", ""), HABIT.replace("\"userId\":73,", ""), HABIT.replace("\"habitId\":1,", "")}) {
            access.body = "[" + item + "]";
            assertError(load(), 0);
        }
        access.body = "[" + HABIT + "," + HABIT + "]";
        assertError(load(), 0);
    }

    @Test
    public void legacyAndIncompleteSchedulesRemainReadableWithoutInventedExecutions() {
        for (String item : new String[] {"{\"id\":105,\"userId\":73,\"habitId\":1}", HABIT.replace("\"frequency\":5", "\"frequency\":6")}) {
            access.body = "[" + item + "]";
            ScreenState<RoutineData> state = load();
            assertEquals(ScreenState.Status.SUCCESS, state.status);
            RoutineData.Day day = state.data.day(LocalDate.of(2026, 10, 26));
            assertFalse(day.agendaComplete);
            assertFalse(day.confirmedEmpty());
            assertNull(day.totalLiters);
        }
    }

    @Test
    public void unknownTypeDoesNotBecomeAKnownEstimateThroughItsName() {
        access.body = "[" + HABIT.replace("\"habitId\":1", "\"habitId\":99") + "]";
        ScreenState<RoutineData> state = load();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertNull(state.data.habits.get(0).litersPerExecution);
    }

    @Test
    public void accountIsResolvedForEachOperationInsteadOfBeingCached() {
        assertEquals(ScreenState.Status.SUCCESS, load().status);
        access.userId = 94;
        assertEquals(ScreenState.Status.SUCCESS, load().status);
        assertRequest(0, "/delta/habits/73");
        assertRequest(1, "/delta/habits/94");
        assertEquals(2, access.profileReads);
    }

    @Test
    public void accountIsResolvedWhenAuthenticatedWorkRuns() {
        Result<RoutineData> result = new Result<>();
        repository.load(result);
        access.userId = 94;
        access.run();
        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertRequest(0, "/delta/habits/94");
    }

    @Test
    public void missingIdentityPreventsCollectionRequest() {
        access.missingProfile = true;
        assertError(load(), 0);
        access.missingProfile = false;
        for (Integer user : new Integer[] {null, 0, -1}) {
            access.userId = user;
            assertError(load(), 0);
        }
        assertTrue(access.requests.isEmpty());
    }

    @Test
    public void profileFailureIsTranslatedWithoutStartingHabitRequests() {
        access.profileFailure = new IOException("internal profile details");
        assertNetwork(load());
        assertTrue(access.requests.isEmpty());
    }

    @Test
    public void authenticatedGateCanRejectCollectionWithoutResolvingIdentity() {
        access.blocked = true;
        ScreenState<?> collection = load();
        for (ScreenState<?> state : new ScreenState<?>[] {collection}) {
            assertError(state, 401);
            assertEquals("Sessão indisponível.", state.message);
            assertEquals(123, state.retryAt);
        }
        assertEquals(1, access.resources);
        assertEquals(0, access.profileReads);
        assertTrue(access.requests.isEmpty());
    }

    @Test
    public void collectionHttpErrorsPreserveStatusAndNeverPublishData() {
        for (int code : new int[] {400, 401, 403, 404, 409, 429, 500, 503}) {
            access.code = code;
            access.body = "{\"message\":\"technical exception SELECT private_data\"}";
            ScreenState<RoutineData> state = load();
            assertError(state, code);
            assertFalse(state.message.contains("private_data"));
            if (code == 429) assertTrue(state.retryAt > 0);
            else assertEquals(0, state.retryAt);
        }
    }

    @Test
    public void collectionConnectionAndTimeoutFailuresUseSharedNetworkTranslation() {
        for (IOException failure : new IOException[] {new IOException("internal host details"), new SocketTimeoutException("internal timeout details")}) {
            access.networkFailure = failure;
            assertNetwork(load());
        }
    }

    @Test
    public void collectionIncompatibleJsonDoesNotEscapeAsAnException() {
        for (String body : new String[] {"{}", "[42]", "[" + HABIT.replace("\"frequency\":5", "\"frequency\":\"invalid\"") + "]"}) {
            access.body = body;
            assertError(load(), 0);
        }
    }

    @Test
    public void retryAfterSecondsIsPreservedForCollection() {
        access.code = 429;
        access.body = "{}";
        access.retryAfter = "120";
        long before = System.currentTimeMillis();
        ScreenState<?> collection = load();
        long after = System.currentTimeMillis();
        for (ScreenState<?> state : new ScreenState<?>[] {collection}) {
            assertError(state, 429);
            assertTrue(state.retryAt >= before + 120000);
            assertTrue(state.retryAt <= after + 120000);
        }
    }

    @Test
    public void retryAfterHttpDateIsPreservedForCollection() {
        access.code = 429;
        access.body = "{}";
        ZonedDateTime retry = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).truncatedTo(ChronoUnit.SECONDS);
        access.retryAfter = DateTimeFormatter.RFC_1123_DATE_TIME.format(retry);
        assertEquals(retry.toInstant().toEpochMilli(), load().retryAt);
    }

    @Test
    public void businessMessagesAreReadOnlyByTheSharedStatusSpecificParser() {
        access.body = "{\"message\":\"Confira o cadastro informado.\"}";
        access.code = 400;
        assertEquals("Confira o cadastro informado.", load().message);
        access.code = 409;
        assertEquals("Confira o cadastro informado.", load().message);
        access.code = 500;
        assertNotEquals("Confira o cadastro informado.", load().message);
    }

    private ScreenState<RoutineData> load() {
        Result<RoutineData> result = new Result<>();
        repository.load(result);
        access.run();
        return result.only();
    }

    private void assertRequest(int index, String path) {
        Request request = access.requests.get(index);
        assertEquals("GET", request.method());
        assertEquals("delta.test", request.url().host());
        assertEquals(path, request.url().encodedPath());
        assertNull(request.url().query());
        assertNull(request.body());
    }

    private void assertError(ScreenState<?> state, int code) {
        assertEquals(ScreenState.Status.ERROR, state.status);
        assertEquals(code, state.code);
        assertNull(state.data);
        assertFalse(state.message.isEmpty());
    }

    private void assertNetwork(ScreenState<?> state) {
        assertError(state, 0);
        assertEquals(ApiErrors.network().message, state.message);
        assertEquals(0, state.retryAt);
    }
}
