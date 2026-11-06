package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.BuildConfig;
import com.aula.delta_mobile_app.model.ChatModels;
import com.aula.delta_mobile_app.model.AuthModels.Tokens;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.api.ChatbotApiService;
import com.aula.delta_mobile_app.services.session.SessionStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
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
        Request request = RetrofitClient.createChatbotApiService(BuildConfig.CHATBOT_API_URL, RetrofitClient.createChatbotHttpClient())
            .chat(new ChatModels.ChatRequest("Água", null)).request();

        assertEquals("https://delta-artificial-intelligence-mvo1.onrender.com/", BuildConfig.CHATBOT_API_URL);
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
    public void chatbotRequestUsesTheStoredDeltaAccessTokenAsBearer() throws Exception {
        SessionStore store = new SessionStore() {
            @Override public long generation() { return 1; }
            @Override public boolean hasSession() { return true; }
            @Override public String access() { return "access-token"; }
            @Override public String refresh() { return "refresh-token"; }
            @Override public long accessExpiresAt() { return Long.MAX_VALUE; }
            @Override public boolean updateAccess(Tokens tokens, long expected) { return false; }
            @Override public void clear() {}
        };
        ApiService deltaApi = (ApiService) Proxy.newProxyInstance(ApiService.class.getClassLoader(), new Class<?>[] {ApiService.class}, (proxy, method, args) -> null);
        OkHttpClient client = RetrofitClient.createChatbotHttpClient().newBuilder()
            .addInterceptor(new ChatbotAuthInterceptor(store, deltaApi))
            .addInterceptor(chain -> {
                requests.add(chain.request());
                return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(ResponseBody.create(MediaType.get("application/json"), "{\"response\":\"ok\",\"session_id\":\"s\"}")).build();
            }).build();

        RetrofitClient.createChatbotApiService(BASE, client).chat(new ChatModels.ChatRequest("Água", null)).execute();

        assertEquals("Bearer access-token", requests.get(0).header("Authorization"));
    }

    @Test
    public void chatSendsJsonAndReadsTypedResponseWithoutCredentials() throws Exception {
        ChatbotApiService api = service("{\"response\":\"Confira suas torneiras.\",\"agent_used\":\"habits\",\"session_id\":\"session-a\",\"chart_data\":null}");

        ChatModels.ChatResponse response = api.chat(new ChatModels.ChatRequest("Como economizar?\nÁgua", null)).execute().body();

        Request request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals(BASE + "chat", request.url().toString());
        assertTrue(request.body().contentType().toString().startsWith("application/json"));
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject json = JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
        assertEquals(1, json.size());
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
    public void sessionsUsesPaginationQueryAndHasNoBody() throws Exception {
        ChatModels.SessionsResponse response = service("{\"sessions\":[]}").sessions(20, 40).execute().body();

        Request request = requests.get(0);
        assertEquals("GET", request.method());
        assertEquals(BASE + "sessions?limit=20&offset=40", request.url().toString());
        assertNull(request.body());
        assertTrue(response.sessions.isEmpty());
        assertNoCredentials(request);
    }

    @Test
    public void historyUsesMessagesPathWithoutUserQueryOrBody() throws Exception {
        ChatModels.HistoryResponse response = service("{\"messages\":[]}").history("session-a").execute().body();

        Request request = requests.get(0);
        assertEquals("GET", request.method());
        assertEquals(BASE + "sessions/session-a/messages", request.url().toString());
        assertNull(request.body());
        assertTrue(response.messages.isEmpty());
        assertNoCredentials(request);
    }

    @Test
    public void closePostsWithoutUserQueryAndWithNoJsonBody() throws Exception {
        ChatModels.CloseResponse response = service("{\"status\":\"ok\",\"session_id\":\"session-a\"}").close("session-a").execute().body();

        Request request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals(BASE + "sessions/session-a/close", request.url().toString());
        assertEquals(0, request.body().contentLength());
        assertNull(request.body().contentType());
        assertEquals("ok", response.status);
        assertEquals("session-a", response.sessionId);
        assertNoCredentials(request);
    }

    @Test
    public void sessionIdentifierIsEncodedAsOnePathSegment() {
        Request request = service("{}").history("session with spaces?x=1").request();

        assertEquals("/sessions/session%20with%20spaces%3Fx=1/messages", request.url().encodedPath());
        assertNull(request.url().encodedQuery());
    }

    @Test
    public void malformedSuccessfulResponseFailsConversionInsteadOfBecomingSuccess() throws Exception {
        for (String body : new String[] {"{\"response\":[]}", "{\"response\":", "<html>unavailable</html>"}) {
            try {
                service(body).chat(new ChatModels.ChatRequest("Água", null)).execute();
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
