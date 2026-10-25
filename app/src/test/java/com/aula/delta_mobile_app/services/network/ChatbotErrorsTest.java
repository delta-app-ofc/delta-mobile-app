package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.function.IntFunction;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.junit.Assert.*;

public class ChatbotErrorsTest {
    private final IntFunction<String> strings = id -> "resource:" + id;
    private static final long NOW = Instant.parse("2026-10-25T19:57:00Z").toEpochMilli();

    private Response<Object> error(int code, String body, String retryAfter) {
        okhttp3.Response.Builder raw = new okhttp3.Response.Builder().request(new Request.Builder().url("https://chatbot.test/chat").build())
            .protocol(Protocol.HTTP_1_1).code(code).message("test");
        if (retryAfter != null) raw.header("Retry-After", retryAfter);
        return Response.error(ResponseBody.create(MediaType.get("application/json"), body), raw.build());
    }

    private ScreenState<Object> parse(int code, String body) {
        return ChatbotErrors.from(error(code, body, null), strings, NOW);
    }

    @Test
    public void textual422MapsKnownGuardrailToLocalResource() {
        ScreenState<Object> state = parse(422, "{\"detail\":\"Mensagem vazia.\"}");

        assertEquals(ScreenState.Status.ERROR, state.status);
        assertEquals(strings.apply(R.string.chat_error_empty_message), state.message);
        assertEquals(422, state.code);
        assertEquals(0, state.retryAt);
        assertNull(state.data);
        assertEquals("", state.displayMessage());
    }

    @Test
    public void list422MapsMessageLengthUsingLocationAndType() {
        String prefix = "{\"detail\":[{\"loc\":[\"body\",\"message\"],\"msg\":\"technical details\",\"type\":\"";

        assertEquals(strings.apply(R.string.chat_error_empty_message), parse(422, prefix + "string_too_short\"}]}").message);
        assertEquals(strings.apply(R.string.chat_error_message_length), parse(422, prefix + "string_too_long\"}]}").message);
    }

    @Test
    public void unknownAndMultipleValidationErrorsUseGenericResourceWithoutRawDetail() {
        for (String body : new String[] {"{\"detail\":\"SQL exception and user content\"}", "{\"detail\":[]}",
                "{\"detail\":[{\"loc\":[\"body\",\"user_id\"],\"type\":\"string_too_short\"}]}",
                "{\"detail\":[{\"loc\":[\"body\",\"message\"],\"type\":\"future_type\"}]}",
                "{\"detail\":[{},{}]}"}) {
            assertEquals(strings.apply(R.string.chat_error_validation), parse(422, body).message);
        }
    }

    @Test
    public void invalidErrorBodiesNeverExposeHtmlOrThrow() {
        for (String body : new String[] {"<html>upstream failure</html>", "{broken", "", "null", "[]", "{}", "{\"detail\":null}",
                "{\"detail\":42}", "{\"detail\":[null]}", "{\"detail\":[{\"loc\":null}]}"}) {
            assertEquals(strings.apply(R.string.chat_error_validation), parse(422, body).message);
        }
    }

    @Test
    public void httpFailuresUseSafeResourcesAndPreserveStatusCode() {
        assertEquals(strings.apply(R.string.chat_error_validation), parse(400, "{}").message);
        assertEquals(strings.apply(R.string.chat_error_access), parse(401, "{}").message);
        assertEquals(strings.apply(R.string.chat_error_access), parse(403, "{}").message);
        assertEquals(strings.apply(R.string.chat_error_request), parse(404, "{}").message);
        ScreenState<Object> state = parse(500, "{\"detail\":\"internal exception with secrets\"}");
        assertEquals(strings.apply(R.string.chat_error_unavailable), state.message);
        assertEquals(500, state.code);
        assertEquals(strings.apply(R.string.chat_error_unavailable), parse(503, "<html>unavailable</html>").message);
    }

    @Test
    public void rateLimitPreservesWaitInSecondsOrHttpDate() {
        ScreenState<Object> seconds = ChatbotErrors.from(error(429, "{}", "30"), strings, NOW);
        ScreenState<Object> date = ChatbotErrors.from(error(429, "{}", "Sun, 25 Oct 2026 19:58:00 GMT"), strings, NOW);

        assertEquals(strings.apply(R.string.chat_error_rate_limit), seconds.message);
        assertEquals(429, seconds.code);
        assertEquals(NOW + 30000, seconds.retryAt);
        assertEquals(NOW + 60000, date.retryAt);
        assertEquals(NOW, ChatbotErrors.from(error(429, "{}", "0"), strings, NOW).retryAt);
        assertEquals(NOW, ChatbotErrors.from(error(429, "{}", "Sun, 25 Oct 2026 19:56:00 GMT"), strings, NOW).retryAt);
    }

    @Test
    public void absentInvalidOrOverflowingRetryAfterDoesNotInventWait() {
        for (String header : new String[] {null, "invalid", "-1", "9223372036854775807", "9999999999999999999999"}) {
            assertEquals(0, ChatbotErrors.from(error(429, "{}", header), strings, NOW).retryAt);
        }
        assertEquals(0, ChatbotErrors.from(error(500, "{}", "30"), strings, NOW).retryAt);
    }

    @Test
    public void networkAndTimeoutHaveDistinctMessagesWithoutTechnicalDetails() {
        ScreenState<Object> offline = ChatbotErrors.network(new IOException("private hostname"), strings);
        ScreenState<Object> timeout = ChatbotErrors.network(new SocketTimeoutException("private url"), strings);

        assertEquals(strings.apply(R.string.chat_error_network), offline.message);
        assertEquals(strings.apply(R.string.chat_error_timeout), timeout.message);
        assertEquals(ScreenState.Status.ERROR, offline.status);
        assertEquals(0, timeout.code);
        assertEquals(0, timeout.retryAt);
    }

    @Test
    public void invalidSuccessfulBodyAndConversionFailureUseInvalidResponseResource() {
        ScreenState<Object> empty = ChatbotErrors.invalidBody(strings);
        ScreenState<Object> malformed = ChatbotErrors.network(new JsonSyntaxException("invalid private payload"), strings);

        assertEquals(strings.apply(R.string.chat_error_invalid_response), empty.message);
        assertEquals(empty.message, malformed.message);
        assertEquals(empty.message, ChatbotErrors.network(new MalformedJsonException("invalid JSON"), strings).message);
        assertEquals(empty.message, ChatbotErrors.network(new EOFException("truncated JSON"), strings).message);
        assertEquals(ScreenState.Status.ERROR, empty.status);
        assertNull(empty.data);
    }
}
