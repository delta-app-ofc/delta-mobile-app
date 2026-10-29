package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.BuildConfig;
import com.aula.delta_mobile_app.model.ChatModels;
import com.aula.delta_mobile_app.services.api.ChatbotApiService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Authenticator;
import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;

import static org.junit.Assert.*;

public class ChatbotNetworkTest {
    private static final String BASE = "https://chatbot.test/";
    private final List<Request> requests = new ArrayList<>();

    private ChatbotApiService service(String body) {
        OkHttpClient client = RetrofitClient.createChatbotHttpClient().newBuilder().addInterceptor(chain -> {
            Request request = chain.request();
            requests.add(request);
            return new okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(MediaType.get("application/json"), body)).build();
        }).build();
        return RetrofitClient.createChatbotApiService(BASE, client);
    }

    @Test
    public void configuredServiceTargetsChatbotHttpsOrigin() {
        Request request = RetrofitClient.getChatbotService().chat(new ChatModels.ChatRequest(42, "Água", null)).request();

        assertEquals("https://delta-artificial-intelligence.onrender.com/", BuildConfig.CHATBOT_API_URL);
        assertEquals(BuildConfig.CHATBOT_API_URL + "chat", request.url().toString());
        assertTrue(request.url().isHttps());
    }

    @Test
    public void clientPreservesTimeoutsWithoutDeltaCredentialsOrConnectionRetries() {
        OkHttpClient client = RetrofitClient.createChatbotHttpClient();

        assertEquals(NetworkConfig.CONNECT_TIMEOUT_SECONDS * 1000, client.connectTimeoutMillis());
        assertEquals(NetworkConfig.READ_TIMEOUT_SECONDS * 1000, client.readTimeoutMillis());
        assertEquals(NetworkConfig.WRITE_TIMEOUT_SECONDS * 1000, client.writeTimeoutMillis());
        assertFalse(client.followRedirects());
        assertFalse(client.followSslRedirects());
        assertFalse(client.retryOnConnectionFailure());
        assertSame(Authenticator.NONE, client.authenticator());
        assertSame(CookieJar.NO_COOKIES, client.cookieJar());
        assertTrue(client.networkInterceptors().isEmpty());
        assertFalse(client.interceptors().stream().anyMatch(interceptor -> interceptor instanceof SessionInterceptor));
    }

    @Test
    public void chatSendsJsonAndReadsTypedResponseWithoutCredentials() throws Exception {
        ChatbotApiService api = service("{\"response\":\"Confira suas torneiras.\",\"agent_used\":\"habits\",\"session_id\":\"session-a\",\"chart_data\":null}");

        ChatModels.ChatResponse response = api.chat(new ChatModels.ChatRequest(42, "Como economizar?\nÁgua", null)).execute().body();

        Request request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals(BASE + "chat", request.url().toString());
        assertTrue(request.body().contentType().toString().startsWith("application/json"));
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject json = JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
        assertEquals(2, json.size());
        assertEquals(42, json.get("user_id").getAsInt());
        assertEquals("Como economizar?\nÁgua", json.get("message").getAsString());
        assertFalse(json.has("session_id"));
        assertEquals("Confira suas torneiras.", response.response);
        assertEquals("habits", response.agentUsed);
        assertEquals("session-a", response.sessionId);
        assertTrue(response.chartData.isJsonNull());
        assertNoCredentials(request);
        assertEquals(1, requests.size());
    }

    @Test
    public void sessionsUsesUserInPathAndHasNoBody() throws Exception {
        ChatModels.SessionsResponse response = service("{\"sessions\":[]}").sessions(42).execute().body();

        Request request = requests.get(0);
        assertEquals("GET", request.method());
        assertEquals(BASE + "sessions/42", request.url().toString());
        assertNull(request.body());
        assertTrue(response.sessions.isEmpty());
        assertNoCredentials(request);
    }

    @Test
    public void historyUsesSessionInPathAndUserInQueryWithoutBody() throws Exception {
        ChatModels.HistoryResponse response = service("{\"messages\":[]}").history("session-a", 42).execute().body();

        Request request = requests.get(0);
        assertEquals("GET", request.method());
        assertEquals(BASE + "sessions/session-a/history?user_id=42", request.url().toString());
        assertNull(request.body());
        assertTrue(response.messages.isEmpty());
        assertNoCredentials(request);
    }

    @Test
    public void closePostsUserInQueryWithNoJsonBody() throws Exception {
        ChatModels.CloseResponse response = service("{\"status\":\"ok\",\"session_id\":\"session-a\"}").close("session-a", 42).execute().body();

        Request request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals(BASE + "sessions/session-a/close?user_id=42", request.url().toString());
        assertEquals(0, request.body().contentLength());
        assertNull(request.body().contentType());
        assertEquals("ok", response.status);
        assertEquals("session-a", response.sessionId);
        assertNoCredentials(request);
    }

    @Test
    public void sessionIdentifierIsEncodedAsOnePathSegment() {
        Request request = service("{}").history("session with spaces?x=1", 42).request();

        assertEquals("/sessions/session%20with%20spaces%3Fx=1/history", request.url().encodedPath());
        assertEquals("user_id=42", request.url().encodedQuery());
    }

    @Test
    public void malformedSuccessfulResponseFailsConversionInsteadOfBecomingSuccess() throws Exception {
        for (String body : new String[] {"{\"response\":[]}", "{\"response\":", "<html>unavailable</html>"}) {
            try {
                service(body).chat(new ChatModels.ChatRequest(42, "Água", null)).execute();
                fail("Expected invalid response conversion");
            } catch (JsonParseException | IOException invalid) {
                assertFalse(invalid.getMessage().isEmpty());
            }
        }
        assertEquals(3, requests.size());
    }

    private void assertNoCredentials(Request request) {
        assertNull(request.header("Authorization"));
        assertNull(request.header("Proxy-Authorization"));
        assertNull(request.header("Cookie"));
        assertEquals("chatbot.test", request.url().host());
        assertEquals("", request.url().username());
        assertEquals("", request.url().password());
    }
}
