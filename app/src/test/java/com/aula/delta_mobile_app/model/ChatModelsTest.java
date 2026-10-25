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
}
