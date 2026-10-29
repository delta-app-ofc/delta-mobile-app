package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.services.network.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChatModelsTest {
    private final Gson gson = GsonProvider.getInstance();

    @Test
    public void newChatRequestUsesContractNamesAndOmitsNullSession() {
        ChatModels.ChatRequest request = new ChatModels.ChatRequest(42, "Como economizar água?", null);

        JsonObject json = gson.toJsonTree(request).getAsJsonObject();

        assertEquals(2, json.size());
        assertEquals(42, json.get("user_id").getAsInt());
        assertEquals("Como economizar água?", json.get("message").getAsString());
        assertFalse(json.has("session_id"));
        assertFalse(json.has("userId"));
    }

    @Test
    public void continuingChatRequestPreservesSessionAndMessage() {
        String message = "  Água e consumo\nO que posso mudar? 💧  ";
        ChatModels.ChatRequest request = new ChatModels.ChatRequest(42, message, "opaque-session-id");

        JsonObject json = gson.toJsonTree(request).getAsJsonObject();

        assertEquals(3, json.size());
        assertEquals("opaque-session-id", json.get("session_id").getAsString());
        assertEquals(message, json.get("message").getAsString());
        assertFalse(json.has("sessionId"));
    }

    @Test
    public void chatResponsePreservesCombinedAgentsAndPlotlyObject() {
        String json = "{\"response\":\"Consumo de água:\\nConfira o período.\","
                + "\"agent_used\":\"forecast, habits\",\"session_id\":\"session-a\","
                + "\"chart_data\":{\"data\":[{\"type\":\"bar\",\"x\":[\"2026-10\"],\"y\":[12.5]}],"
                + "\"layout\":{\"title\":{\"text\":\"Consumo\"}},\"custom\":{\"enabled\":true}}}";

        ChatModels.ChatResponse response = gson.fromJson(json, ChatModels.ChatResponse.class);

        assertEquals("Consumo de água:\nConfira o período.", response.response);
        assertEquals("forecast, habits", response.agentUsed);
        assertEquals("session-a", response.sessionId);
        JsonObject chart = response.chartData.getAsJsonObject();
        JsonObject trace = chart.getAsJsonArray("data").get(0).getAsJsonObject();
        assertEquals("bar", trace.get("type").getAsString());
        assertEquals(12.5, trace.getAsJsonArray("y").get(0).getAsDouble(), 0.0);
        assertEquals("Consumo", chart.getAsJsonObject("layout").getAsJsonObject("title").get("text").getAsString());
        assertTrue(chart.getAsJsonObject("custom").get("enabled").getAsBoolean());
    }

    @Test
    public void chatResponseAcceptsNullOrAbsentChartAndUnknownAgent() {
        String base = "\"response\":\"Resposta\",\"agent_used\":\"future_agent\",\"session_id\":\"session-a\"";

        ChatModels.ChatResponse explicitNull = gson.fromJson("{" + base + ",\"chart_data\":null}", ChatModels.ChatResponse.class);
        ChatModels.ChatResponse absent = gson.fromJson("{" + base + "}", ChatModels.ChatResponse.class);

        assertTrue(explicitNull.chartData.isJsonNull());
        assertNull(absent.chartData);
        assertEquals("future_agent", absent.agentUsed);
    }

    @Test
    public void missingChatFieldsDoNotBecomeSuccessDefaults() {
        ChatModels.ChatResponse response = gson.fromJson("{}", ChatModels.ChatResponse.class);

        assertNull(response.response);
        assertNull(response.agentUsed);
        assertNull(response.sessionId);
        assertNull(response.chartData);
    }

    @Test
    public void sessionsResponsePreservesIdentifiersAndTransportDates() {
        String json = "{\"sessions\":[{\"session_id\":\"opaque-session-id\",\"created_at\":\"2026-10-25T19:57:00.123456+00:00\"},"
                + "{\"session_id\":\"session-b\",\"created_at\":\"2026-10-24T16:57:00-03:00\"}]}";

        ChatModels.SessionsResponse response = gson.fromJson(json, ChatModels.SessionsResponse.class);

        assertEquals(2, response.sessions.size());
        assertEquals("opaque-session-id", response.sessions.get(0).sessionId);
        assertEquals("2026-10-25T19:57:00.123456+00:00", response.sessions.get(0).createdAt);
        assertEquals("session-b", response.sessions.get(1).sessionId);
        assertEquals("2026-10-24T16:57:00-03:00", response.sessions.get(1).createdAt);
    }

    @Test
    public void emptySessionsAreDistinctFromNullOrMissingSessions() {
        ChatModels.SessionsResponse empty = gson.fromJson("{\"sessions\":[]}", ChatModels.SessionsResponse.class);
        ChatModels.SessionsResponse explicitNull = gson.fromJson("{\"sessions\":null}", ChatModels.SessionsResponse.class);
        ChatModels.SessionsResponse absent = gson.fromJson("{}", ChatModels.SessionsResponse.class);

        assertNotNull(empty.sessions);
        assertTrue(empty.sessions.isEmpty());
        assertNull(explicitNull.sessions);
        assertNull(absent.sessions);
    }

    @Test
    public void historyPreservesMessageOrderRolesContentAndDates() {
        String json = "{\"messages\":[{\"role\":\"human\",\"content\":\"Como economizar água? 💧\","
                + "\"timestamp\":\"2026-10-25T19:57:00+00:00\"},"
                + "{\"role\":\"assistant\",\"content\":\"Confira:\\n- Torneiras\\n- Chuveiro\","
                + "\"timestamp\":\"2026-10-25T19:57:01.123456+00:00\"}]}";

        ChatModels.HistoryResponse response = gson.fromJson(json, ChatModels.HistoryResponse.class);

        assertEquals(2, response.messages.size());
        assertEquals("human", response.messages.get(0).role);
        assertEquals("Como economizar água? 💧", response.messages.get(0).content);
        assertEquals("2026-10-25T19:57:00+00:00", response.messages.get(0).timestamp);
        assertEquals("assistant", response.messages.get(1).role);
        assertEquals("Confira:\n- Torneiras\n- Chuveiro", response.messages.get(1).content);
        assertEquals("2026-10-25T19:57:01.123456+00:00", response.messages.get(1).timestamp);
    }

    @Test
    public void emptyHistoryIsDistinctFromNullOrMissingHistory() {
        ChatModels.HistoryResponse empty = gson.fromJson("{\"messages\":[]}", ChatModels.HistoryResponse.class);
        ChatModels.HistoryResponse explicitNull = gson.fromJson("{\"messages\":null}", ChatModels.HistoryResponse.class);
        ChatModels.HistoryResponse absent = gson.fromJson("{}", ChatModels.HistoryResponse.class);

        assertNotNull(empty.messages);
        assertTrue(empty.messages.isEmpty());
        assertNull(explicitNull.messages);
        assertNull(absent.messages);
    }

    @Test
    public void missingSessionAndHistoryFieldsRemainNull() {
        ChatModels.Session session = gson.fromJson("{}", ChatModels.Session.class);
        ChatModels.HistoryMessage message = gson.fromJson("{}", ChatModels.HistoryMessage.class);

        assertNull(session.sessionId);
        assertNull(session.createdAt);
        assertNull(message.role);
        assertNull(message.content);
        assertNull(message.timestamp);
    }

    @Test
    public void closeResponsePreservesStatusAndSessionWithoutDefaultSuccess() {
        ChatModels.CloseResponse response = gson.fromJson("{\"status\":\"ok\",\"session_id\":\"opaque-session-id\"}", ChatModels.CloseResponse.class);
        ChatModels.CloseResponse missing = gson.fromJson("{}", ChatModels.CloseResponse.class);

        assertEquals("ok", response.status);
        assertEquals("opaque-session-id", response.sessionId);
        assertNull(missing.status);
        assertNull(missing.sessionId);
    }
}
