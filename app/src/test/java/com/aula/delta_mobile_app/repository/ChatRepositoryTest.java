package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ChatbotApiService;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import com.google.gson.JsonSyntaxException;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

public class ChatRepositoryTest {
    private final FakeApi api = new FakeApi();
    private final IntFunction<String> strings = id -> "resource:" + id;
    private final ChatActions repository = new ChatRepository(api, 42, strings);

    private static class Result<T> implements Consumer<ScreenState<T>> {
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

    private static class Pending<T> implements Call<T> {
        Callback<T> callback;
        boolean canceled;
        int enqueues;

        @Override
        public void enqueue(Callback<T> callback) {
            enqueues++;
            this.callback = callback;
        }

        void respond(T body) {
            callback.onResponse(this, Response.success(body));
        }

        void reject(int code, String body, String retryAfter) {
            okhttp3.Response.Builder raw = new okhttp3.Response.Builder().request(request()).protocol(Protocol.HTTP_1_1).code(code).message("test");
            if (retryAfter != null) raw.header("Retry-After", retryAfter);
            callback.onResponse(this, Response.error(ResponseBody.create(MediaType.get("application/json"), body), raw.build()));
        }

        void fail(Throwable failure) {
            callback.onFailure(this, failure);
        }

        @Override
        public Response<T> execute() {
            throw new AssertionError("Repository must use enqueue");
        }

        @Override
        public boolean isExecuted() {
            return enqueues > 0;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<T> clone() {
            throw new AssertionError("Repository must not repeat requests");
        }

        @Override
        public Request request() {
            return new Request.Builder().url("https://chatbot.test/").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }

    private static class FakeApi implements ChatbotApiService {
        final List<Pending<SessionsResponse>> sessions = new ArrayList<>();
        final List<Pending<HistoryResponse>> histories = new ArrayList<>();
        final List<Pending<ChatResponse>> sends = new ArrayList<>();
        final List<Pending<CloseResponse>> closes = new ArrayList<>();
        final List<Integer> sessionUsers = new ArrayList<>(), historyUsers = new ArrayList<>(), closeUsers = new ArrayList<>();
        final List<String> historyIds = new ArrayList<>(), closeIds = new ArrayList<>();
        final List<ChatRequest> payloads = new ArrayList<>();

        @Override
        public Call<SessionsResponse> sessions(int userId) {
            sessionUsers.add(userId);
            Pending<SessionsResponse> call = new Pending<>();
            sessions.add(call);
            return call;
        }

        @Override
        public Call<HistoryResponse> history(String sessionId, int userId) {
            historyIds.add(sessionId);
            historyUsers.add(userId);
            Pending<HistoryResponse> call = new Pending<>();
            histories.add(call);
            return call;
        }

        @Override
        public Call<ChatResponse> chat(ChatRequest request) {
            payloads.add(request);
            Pending<ChatResponse> call = new Pending<>();
            sends.add(call);
            return call;
        }

        @Override
        public Call<CloseResponse> close(String sessionId, int userId) {
            closeIds.add(sessionId);
            closeUsers.add(userId);
            Pending<CloseResponse> call = new Pending<>();
            closes.add(call);
            return call;
        }
    }

    private <T> T json(String json, Class<T> type) {
        return GsonProvider.getInstance().fromJson(json, type);
    }

    private void assertInvalid(ScreenState<?> state) {
        assertEquals(ScreenState.Status.ERROR, state.status);
        assertEquals(strings.apply(R.string.chat_error_invalid_response), state.message);
        assertNull(state.data);
    }

    @Test
    public void constructionDoesNotStartRequestsAndRejectsInvalidAccount() {
        assertTrue(api.sessions.isEmpty());
        assertTrue(api.histories.isEmpty());
        assertTrue(api.sends.isEmpty());
        assertTrue(api.closes.isEmpty());
        for (int user : new int[] {0, -1}) {
            try {
                new ChatRepository(api, user, strings);
                fail("Expected positive account identity");
            } catch (IllegalArgumentException expected) {
                assertFalse(expected.getMessage().isEmpty());
            }
        }
    }

    @Test
    public void sessionsUsesBoundAccountAndPreservesServerOrderAndDates() {
        Result<List<Session>> result = new Result<>();
        repository.sessions(result);

        assertEquals(Integer.valueOf(42), api.sessionUsers.get(0));
        assertTrue(result.values.isEmpty());
        assertEquals(1, api.sessions.get(0).enqueues);
        api.sessions.get(0).respond(json("{\"sessions\":[{\"session_id\":\"opaque-b\",\"created_at\":\"2026-10-25T19:57:00+00:00\"},"
            + "{\"session_id\":\"opaque-a\",\"created_at\":\"unformatted-date\"}]}", SessionsResponse.class));

        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals("opaque-b", result.only().data.get(0).sessionId);
        assertEquals("2026-10-25T19:57:00+00:00", result.only().data.get(0).createdAt);
        assertEquals("opaque-a", result.only().data.get(1).sessionId);
        assertEquals("unformatted-date", result.only().data.get(1).createdAt);
    }

    @Test
    public void historyUsesBoundAccountAndPreservesMessageOrderAndText() {
        Result<List<HistoryMessage>> result = new Result<>();
        repository.history("opaque-session", result);

        assertEquals(Integer.valueOf(42), api.historyUsers.get(0));
        assertEquals("opaque-session", api.historyIds.get(0));
        assertTrue(result.values.isEmpty());
        api.histories.get(0).respond(json("{\"messages\":[{\"role\":\"human\",\"content\":\"Água 💧\",\"timestamp\":\"2026-10-25T19:57:00Z\"},"
            + "{\"role\":\"assistant\",\"content\":\"Confira:\\n- Torneiras\",\"timestamp\":\"2026-10-25T19:57:01Z\"}]}", HistoryResponse.class));

        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals("human", result.only().data.get(0).role);
        assertEquals("Água 💧", result.only().data.get(0).content);
        assertEquals("Confira:\n- Torneiras", result.only().data.get(1).content);
        assertEquals("2026-10-25T19:57:01Z", result.only().data.get(1).timestamp);
    }

    @Test
    public void emptySessionsAndHistoryAreSuccessfulWithoutInventingACause() {
        Result<List<Session>> sessions = new Result<>();
        Result<List<HistoryMessage>> history = new Result<>();
        repository.sessions(sessions);
        repository.history("opaque-session", history);
        api.sessions.get(0).respond(json("{\"sessions\":[]}", SessionsResponse.class));
        api.histories.get(0).respond(json("{\"messages\":[]}", HistoryResponse.class));

        assertEquals(ScreenState.Status.SUCCESS, sessions.only().status);
        assertTrue(sessions.only().data.isEmpty());
        assertEquals(ScreenState.Status.SUCCESS, history.only().status);
        assertTrue(history.only().data.isEmpty());
        assertEquals("", history.only().message);
    }

    @Test
    public void missingOrIncompleteSessionsAreInvalidRatherThanEmptySuccess() {
        for (String body : new String[] {"null", "{}", "{\"sessions\":null}", "{\"sessions\":[null]}", "{\"sessions\":[{}]}",
                "{\"sessions\":[{\"session_id\":\"a\"}]}", "{\"sessions\":[{\"session_id\":\"\",\"created_at\":\"date\"}]}"}) {
            Result<List<Session>> result = new Result<>();
            repository.sessions(result);
            api.sessions.get(api.sessions.size() - 1).respond(json(body, SessionsResponse.class));
            assertInvalid(result.only());
        }
    }

    @Test
    public void missingOrIncompleteHistoryIsInvalidRatherThanEmptySuccess() {
        for (String body : new String[] {"null", "{}", "{\"messages\":null}", "{\"messages\":[null]}", "{\"messages\":[{}]}",
                "{\"messages\":[{\"role\":\"assistant\",\"content\":\"Answer\"}]}",
                "{\"messages\":[{\"role\":\"assistant\",\"content\":\" \",\"timestamp\":\"date\"}]}"}) {
            Result<List<HistoryMessage>> result = new Result<>();
            repository.history("opaque-session", result);
            api.histories.get(api.histories.size() - 1).respond(json(body, HistoryResponse.class));
            assertInvalid(result.only());
        }
    }

    @Test
    public void oldAccountCompletionOnlyReachesItsOriginalCallback() {
        ChatActions newAccount = new ChatRepository(api, 77, strings);
        Result<List<Session>> oldResult = new Result<>(), newResult = new Result<>();
        repository.sessions(oldResult);
        newAccount.sessions(newResult);

        api.sessions.get(1).respond(json("{\"sessions\":[{\"session_id\":\"new-account\",\"created_at\":\"date\"}]}", SessionsResponse.class));
        assertTrue(oldResult.values.isEmpty());
        api.sessions.get(0).respond(json("{\"sessions\":[{\"session_id\":\"old-account\",\"created_at\":\"date\"}]}", SessionsResponse.class));

        assertEquals(Integer.valueOf(42), api.sessionUsers.get(0));
        assertEquals(Integer.valueOf(77), api.sessionUsers.get(1));
        assertEquals("old-account", oldResult.only().data.get(0).sessionId);
        assertEquals("new-account", newResult.only().data.get(0).sessionId);
    }

    @Test
    public void sessionHttpFailurePreservesStatusAndRateLimitWaitWithoutRetry() {
        Result<List<Session>> result = new Result<>();
        repository.sessions(result);
        long before = System.currentTimeMillis();
        api.sessions.get(0).reject(429, "{\"detail\":\"private server text\"}", "30");

        assertEquals(ScreenState.Status.ERROR, result.only().status);
        assertEquals(429, result.only().code);
        assertEquals(strings.apply(R.string.chat_error_rate_limit), result.only().message);
        assertTrue(result.only().retryAt >= before + 30000);
        assertEquals(1, api.sessions.size());
        assertEquals(1, api.sessions.get(0).enqueues);
    }

    @Test
    public void historyConnectionAndTimeoutFailuresAreMappedWithoutRetry() {
        Result<List<HistoryMessage>> connection = new Result<>(), timeout = new Result<>();
        repository.history("opaque-session", connection);
        repository.history("opaque-session", timeout);
        api.histories.get(0).fail(new IOException("private host"));
        api.histories.get(1).fail(new SocketTimeoutException("private url"));

        assertEquals(strings.apply(R.string.chat_error_network), connection.only().message);
        assertEquals(strings.apply(R.string.chat_error_timeout), timeout.only().message);
        assertEquals(2, api.histories.size());
        assertEquals(1, api.histories.get(1).enqueues);
        assertFalse(api.histories.get(0).isCanceled());
    }

    @Test
    public void firstSendUsesBoundAccountAndPreservesReturnedSessionAndResponse() {
        Result<ChatResponse> result = new Result<>();
        String message = "  Como economizar água?\n💧  ";
        repository.send(message, null, result);

        assertEquals(42, api.payloads.get(0).userId);
        assertEquals(message, api.payloads.get(0).message);
        assertNull(api.payloads.get(0).sessionId);
        assertTrue(result.values.isEmpty());
        api.sends.get(0).respond(json("{\"response\":\"Confira suas torneiras.\",\"agent_used\":\"habits, rag\",\"session_id\":\"new-session\",\"chart_data\":null}", ChatResponse.class));

        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals("new-session", result.only().data.sessionId);
        assertEquals("habits, rag", result.only().data.agentUsed);
        assertEquals("Confira suas torneiras.", result.only().data.response);
    }

    @Test
    public void continuingSendKeepsRequestedSessionAndAcceptsChartObject() {
        Result<ChatResponse> result = new Result<>();
        repository.send("Continue", "opaque-session", result);
        api.sends.get(0).respond(json("{\"response\":\"Resposta\",\"agent_used\":\"future_agent\",\"session_id\":\"opaque-session\",\"chart_data\":{\"data\":[]}}", ChatResponse.class));

        assertEquals("opaque-session", api.payloads.get(0).sessionId);
        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertTrue(result.only().data.chartData.isJsonObject());
    }

    @Test
    public void incompleteSendResponseOrWrongSessionNeverBecomesSuccess() {
        for (String body : new String[] {"null", "{}", "{\"response\":\"Resposta\",\"session_id\":\"opaque-session\"}",
                "{\"response\":\" \",\"agent_used\":\"rag\",\"session_id\":\"opaque-session\"}",
                "{\"response\":\"Resposta\",\"agent_used\":\"rag\",\"session_id\":\"\"}",
                "{\"response\":\"Resposta\",\"agent_used\":\"rag\",\"session_id\":\"different-session\"}",
                "{\"response\":\"Resposta\",\"agent_used\":\"rag\",\"session_id\":\"opaque-session\",\"chart_data\":[]}"}) {
            Result<ChatResponse> result = new Result<>();
            repository.send("Continue", "opaque-session", result);
            api.sends.get(api.sends.size() - 1).respond(json(body, ChatResponse.class));
            assertInvalid(result.only());
        }
    }

    @Test
    public void unavailableTextInSuccessfulResponseIsPreservedAsServerContent() {
        Result<ChatResponse> result = new Result<>();
        String message = "Não foi possível obter uma resposta neste momento. Tente novamente.";
        repository.send("Água", null, result);
        ChatResponse body = new ChatResponse();
        body.response = message;
        body.agentUsed = "forecast";
        body.sessionId = "new-session";
        api.sends.get(0).respond(body);

        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals(message, result.only().data.response);
    }

    @Test
    public void closeUsesBoundAccountAndOnlyConfirmsMatchingSession() {
        Result<CloseResponse> result = new Result<>();
        repository.close("opaque-session", result);

        assertEquals(Integer.valueOf(42), api.closeUsers.get(0));
        assertEquals("opaque-session", api.closeIds.get(0));
        assertTrue(result.values.isEmpty());
        api.closes.get(0).respond(json("{\"status\":\"ok\",\"session_id\":\"opaque-session\"}", CloseResponse.class));

        assertEquals(ScreenState.Status.SUCCESS, result.only().status);
        assertEquals("opaque-session", result.only().data.sessionId);
        assertEquals("ok", result.only().data.status);
        assertTrue(api.sends.isEmpty());
    }

    @Test
    public void missingCloseFieldsUnknownStatusAndWrongSessionAreInvalid() {
        for (String body : new String[] {"null", "{}", "{\"status\":\"ok\"}", "{\"session_id\":\"opaque-session\"}",
                "{\"status\":\"failed\",\"session_id\":\"opaque-session\"}", "{\"status\":\"ok\",\"session_id\":\"different-session\"}"}) {
            Result<CloseResponse> result = new Result<>();
            repository.close("opaque-session", result);
            api.closes.get(api.closes.size() - 1).respond(json(body, CloseResponse.class));
            assertInvalid(result.only());
        }
    }

    @Test
    public void sendAndCloseHttpFailuresReuseSafeErrorMapping() {
        Result<ChatResponse> send = new Result<>();
        Result<CloseResponse> close = new Result<>();
        repository.send(" ", null, send);
        repository.close("opaque-session", close);
        api.sends.get(0).reject(422, "{\"detail\":\"Mensagem vazia.\"}", null);
        api.closes.get(0).reject(500, "{\"detail\":\"private exception\"}", null);

        assertEquals(422, send.only().code);
        assertEquals(strings.apply(R.string.chat_error_empty_message), send.only().message);
        assertEquals(500, close.only().code);
        assertEquals(strings.apply(R.string.chat_error_unavailable), close.only().message);
    }

    @Test
    public void sendConversionFailureAndCloseTimeoutDoNotRepeatOrCancelRequests() {
        Result<ChatResponse> send = new Result<>();
        Result<CloseResponse> close = new Result<>();
        repository.send("Água", null, send);
        repository.close("opaque-session", close);
        api.sends.get(0).fail(new JsonSyntaxException("private invalid payload"));
        api.closes.get(0).fail(new SocketTimeoutException("private url"));

        assertInvalid(send.only());
        assertEquals(strings.apply(R.string.chat_error_timeout), close.only().message);
        assertEquals(1, api.sends.size());
        assertEquals(1, api.closes.size());
        assertFalse(api.sends.get(0).isCanceled());
        assertFalse(api.closes.get(0).isCanceled());
    }

    @Test
    public void lateSendFromOldAccountCannotReachNewAccountCallback() {
        ChatActions newAccount = new ChatRepository(api, 77, strings);
        Result<ChatResponse> oldResult = new Result<>(), newResult = new Result<>();
        repository.send("Old account", null, oldResult);
        newAccount.send("New account", null, newResult);
        api.sends.get(1).respond(json("{\"response\":\"New reply\",\"agent_used\":\"rag\",\"session_id\":\"new-account\"}", ChatResponse.class));
        assertTrue(oldResult.values.isEmpty());
        api.sends.get(0).respond(json("{\"response\":\"Old reply\",\"agent_used\":\"rag\",\"session_id\":\"old-account\"}", ChatResponse.class));

        assertEquals(42, api.payloads.get(0).userId);
        assertEquals(77, api.payloads.get(1).userId);
        assertEquals("old-account", oldResult.only().data.sessionId);
        assertEquals("new-account", newResult.only().data.sessionId);
    }
}
