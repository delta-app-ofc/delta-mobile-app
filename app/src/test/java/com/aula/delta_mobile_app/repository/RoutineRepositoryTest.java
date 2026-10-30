package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Test;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;
import static org.junit.Assert.*;

public class RoutineRepositoryTest {
    private RoutineFormState createForm() {
        return RoutineFormState.create(1).withHabit(1).withFrequency("5")
            .selectDay(DayOfWeek.MONDAY, true).withQuantity(DayOfWeek.MONDAY, "2")
            .withTime(DayOfWeek.MONDAY, 0, "07:00").withTime(DayOfWeek.MONDAY, 1, "20:00")
            .selectDay(DayOfWeek.WEDNESDAY, true).withQuantity(DayOfWeek.WEDNESDAY, "3")
            .withTime(DayOfWeek.WEDNESDAY, 0, "08:00").withTime(DayOfWeek.WEDNESDAY, 1, "14:00")
            .withTime(DayOfWeek.WEDNESDAY, 2, "21:00");
    }

    @Test
    public void createUsesAuthenticatedRouteAndExactPayloadAndRequires201() throws Exception {
        access.code = 201;
        access.body = HABIT;
        Result<RoutineData.Habit> result = new Result<>();
        repository.create(createForm(), result);
        assertTrue(access.requests.isEmpty());
        access.run();
        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals(105, result.only().data.id);
        Request request = access.requests.get(0);
        assertEquals("POST", request.method());
        assertEquals("/delta/habits/73", request.url().encodedPath());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject body = JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
        assertEquals(4, body.size());
        assertEquals(5, body.get("frequency").getAsInt());
        assertEquals(2, body.getAsJsonArray("schedule").size());
        assertEquals("20:00", body.getAsJsonArray("schedule").get(0).getAsJsonObject().getAsJsonArray("startTimes").get(1).getAsString());
        assertFalse(body.has("userId"));
    }

    @Test
    public void createRejectsInvalidDraftWithoutRequestOrResource() {
        Result<RoutineData.Habit> result = new Result<>();
        repository.create(createForm().withFrequency(""), result);
        assertEquals(400, result.only().code);
        assertEquals(0, access.resources);
    }

    @Test
    public void createPreservesConfirmedHttpErrorsAndRetryAfter() {
        for (int code : new int[]{400, 409, 401, 403, 429}) {
            access.code = code;
            access.body = "{}";
            access.retryAfter = "60";
            Result<RoutineData.Habit> result = new Result<>();
            repository.create(createForm(), result);
            access.run();
            assertEquals(code, result.only().code);
            if (code == 429) assertTrue(result.only().retryAt > 0);
        }
    }

    @Test
    public void createTreatsMismatchedMissingOrMalformedSuccessAsUncertain() {
        for (String body : new String[]{"", "{}", "null", "broken", HABIT.replace("\"userId\":73", "\"userId\":74"),
            HABIT.replace("\"habitId\":1", "\"habitId\":2"), HABIT.replace("07:00", "07:30"), HABIT.replace("\"frequency\":5", "\"frequency\":6")}) {
            access.code = 201;
            access.body = body;
            Result<RoutineData.Habit> result = new Result<>();
            repository.create(createForm(), result);
            access.run();
            assertEquals(body, RoutineRepository.UNCERTAIN, result.only().code);
        }
    }

    @Test
    public void createTreatsUnexpectedSuccessServerFailureAndTimeoutAsUncertain() {
        for (int code : new int[]{200, 202, 500, 503}) {
            access.code = code;
            access.body = HABIT;
            Result<RoutineData.Habit> result = new Result<>();
            repository.create(createForm(), result);
            access.run();
            assertEquals(RoutineRepository.UNCERTAIN, result.only().code);
        }
        access.networkFailure = new SocketTimeoutException();
        Result<RoutineData.Habit> timeout = new Result<>();
        repository.create(createForm(), timeout);
        access.run();
        assertEquals(RoutineRepository.UNCERTAIN, timeout.only().code);
    }

    @Test
    public void profileFailureBeforePostDoesNotBecomeAnUncertainMutation() {
        access.profileFailure = new IOException();
        Result<RoutineData.Habit> result = new Result<>();
        repository.create(createForm(), result);
        access.run();
        assertNotEquals(RoutineRepository.UNCERTAIN, result.only().code);
        assertTrue(access.requests.isEmpty());
    }

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
    public void missingIdentityPreventsBothHabitRequests() {
        access.missingProfile = true;
        assertError(load(), 0);
        assertError(detail(), 0);
        access.missingProfile = false;
        for (Integer user : new Integer[] {null, 0, -1}) {
            access.userId = user;
            assertError(load(), 0);
            assertError(detail(), 0);
        }
        assertTrue(access.requests.isEmpty());
    }

    @Test
    public void profileFailureIsTranslatedWithoutStartingHabitRequests() {
        access.profileFailure = new IOException("internal profile details");
        assertNetwork(load());
        assertNetwork(detail());
        assertTrue(access.requests.isEmpty());
    }

    @Test
    public void authenticatedGateCanRejectBothOperationsWithoutResolvingIdentity() {
        access.blocked = true;
        ScreenState<?> collection = load();
        ScreenState<?> detail = detail();
        for (ScreenState<?> state : new ScreenState<?>[] {collection, detail}) {
            assertError(state, 401);
            assertEquals("Sessão indisponível.", state.message);
            assertEquals(123, state.retryAt);
        }
        assertEquals(2, access.resources);
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
    public void detailUsesRecordIdAndAuthenticatedOwnerWithoutFetchingTheCollection() {
        access.body = HABIT;
        Result<RoutineData.Habit> result = new Result<>();
        repository.detail(105, result);
        assertTrue(access.requests.isEmpty());
        assertTrue(result.values.isEmpty());
        access.run();
        ScreenState<RoutineData.Habit> state = result.only();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertEquals(105, state.data.id);
        assertEquals(1, state.data.habitId);
        assertEquals(73, state.data.userId);
        assertEquals(5, state.data.frequency.intValue());
        assertTrue(state.data.scheduleComplete);
        assertEquals(1, access.requests.size());
        assertRequest(0, "/delta/habits/73/105");
    }

    @Test
    public void detailRejectsAnotherOwnerOrAnotherRecordEvenWithMatchingHabitType() {
        for (String body : new String[] {HABIT.replace("\"userId\":73", "\"userId\":74"), HABIT.replace("\"id\":105", "\"id\":106")}) {
            access.body = body;
            assertError(detail(), 0);
        }
    }

    @Test
    public void detailRejectsNullMissingIdentityOrIncompatibleBodies() {
        for (String body : new String[] {"null", "{}", "[]", "42", HABIT.replace("\"habitId\":1,", ""), HABIT.replace("\"frequency\":5", "\"frequency\":\"invalid\"")}) {
            access.body = body;
            assertError(detail(), 0);
        }
        access.code = 204;
        access.body = "";
        assertError(detail(), 0);
    }

    @Test
    public void detailWithoutOptionalMetadataOrSchedulePreservesTheLegacyRecord() {
        access.body = "{\"id\":105,\"userId\":73,\"habitId\":1}";
        ScreenState<RoutineData.Habit> state = detail();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertEquals(105, state.data.id);
        assertNull(state.data.name);
        assertNull(state.data.category);
        assertNull(state.data.description);
        assertFalse(state.data.scheduleComplete);
        assertTrue(state.data.schedule.isEmpty());
    }

    @Test
    public void detailHttpErrorsIncludeNotFoundAndPreserveStatus() {
        for (int code : new int[] {400, 401, 403, 404, 409, 429, 500, 503}) {
            access.code = code;
            access.body = "{}";
            assertError(detail(), code);
        }
    }

    @Test
    public void detailConnectionAndTimeoutFailuresUseSharedNetworkTranslation() {
        for (IOException failure : new IOException[] {new IOException("internal host details"), new SocketTimeoutException("internal timeout details")}) {
            access.networkFailure = failure;
            assertNetwork(detail());
        }
    }

    @Test
    public void retryAfterSecondsIsPreservedForBothReads() {
        access.code = 429;
        access.body = "{}";
        access.retryAfter = "120";
        long before = System.currentTimeMillis();
        ScreenState<?> collection = load();
        ScreenState<?> detail = detail();
        long after = System.currentTimeMillis();
        for (ScreenState<?> state : new ScreenState<?>[] {collection, detail}) {
            assertError(state, 429);
            assertTrue(state.retryAt >= before + 120000);
            assertTrue(state.retryAt <= after + 120000);
        }
    }

    @Test
    public void retryAfterHttpDateIsPreservedForBothReads() {
        access.code = 429;
        access.body = "{}";
        ZonedDateTime retry = ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).truncatedTo(ChronoUnit.SECONDS);
        access.retryAfter = DateTimeFormatter.RFC_1123_DATE_TIME.format(retry);
        assertEquals(retry.toInstant().toEpochMilli(), load().retryAt);
        assertEquals(retry.toInstant().toEpochMilli(), detail().retryAt);
    }

    @Test
    public void businessMessagesAreReadOnlyByTheSharedStatusSpecificParser() {
        access.body = "{\"message\":\"Confira o cadastro informado.\"}";
        access.code = 400;
        assertEquals("Confira o cadastro informado.", load().message);
        access.code = 409;
        assertEquals("Confira o cadastro informado.", detail().message);
        access.code = 500;
        assertNotEquals("Confira o cadastro informado.", load().message);
    }

    @Test
    public void detailRequestsKeepTheirOwnTargetWhenAuthenticatedWorkRunsOutOfOrder() {
        Result<RoutineData.Habit> first = new Result<>();
        Result<RoutineData.Habit> second = new Result<>();
        repository.detail(105, first);
        repository.detail(106, second);
        access.body = HABIT.replace("\"id\":105", "\"id\":106");
        access.work.remove(1).run();
        assertEquals(106, second.only().data.id);
        assertTrue(first.values.isEmpty());
        access.body = HABIT;
        access.run();
        assertEquals(105, first.only().data.id);
        assertRequest(0, "/delta/habits/73/106");
        assertRequest(1, "/delta/habits/73/105");
    }

    @Test
    public void detailResolvesTheAccountAtExecutionAndRejectsThePreviousOwnersBody() {
        access.body = HABIT;
        Result<RoutineData.Habit> result = new Result<>();
        repository.detail(105, result);
        access.userId = 94;
        access.run();
        assertError(result.only(), 0);
        assertRequest(0, "/delta/habits/94/105");
        access.body = HABIT.replace("\"userId\":73", "\"userId\":94");
        ScreenState<RoutineData.Habit> current = detail();
        assertEquals(ScreenState.Status.SUCCESS, current.status);
        assertEquals(94, current.data.userId);
    }

    @Test
    public void detailInconsistentScheduleRemainsIncompleteWithoutDiscardingItsIdentity() {
        access.body = HABIT.replace("\"frequency\":5", "\"frequency\":6");
        ScreenState<RoutineData.Habit> state = detail();
        assertEquals(ScreenState.Status.SUCCESS, state.status);
        assertEquals(105, state.data.id);
        assertFalse(state.data.scheduleComplete);
        assertTrue(state.data.schedule.isEmpty());
    }

    private ScreenState<RoutineData> load() {
        Result<RoutineData> result = new Result<>();
        repository.load(result);
        access.run();
        return result.only();
    }

    private ScreenState<RoutineData.Habit> detail() {
        Result<RoutineData.Habit> result = new Result<>();
        repository.detail(105, result);
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
