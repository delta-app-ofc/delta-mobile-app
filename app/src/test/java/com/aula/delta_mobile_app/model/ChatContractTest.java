package com.aula.delta_mobile_app.model;

import com.aula.delta_mobile_app.services.network.GsonProvider;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatContractTest {
    @Test
    public void requestUsesDocumentedFieldNamesAndOptionalSession() {
        JsonObject first = GsonProvider.getInstance().toJsonTree(new ChatModels.Request(42, null, "Olá")).getAsJsonObject();
        assertEquals(42, first.get("user_id").getAsInt());
        assertEquals("Olá", first.get("message").getAsString());
        assertFalse(first.has("session_id"));
        assertFalse(first.has("userId"));
        JsonObject next = GsonProvider.getInstance().toJsonTree(new ChatModels.Request(42, "session-1", "Continue")).getAsJsonObject();
        assertEquals("session-1", next.get("session_id").getAsString());
    }

    @Test
    public void decodesResponseAndOptionalChartWithoutInventingSchema() {
        ChatModels.Reply reply = GsonProvider.getInstance().fromJson("{\"response\":\"💧 Economize água\",\"agent_used\":\"routine\",\"session_id\":\"s1\",\"chart_data\":{\"series\":[1,2]}}", ChatModels.Reply.class);
        assertEquals("s1", reply.sessionId);
        assertEquals("routine", reply.agentUsed);
        assertEquals("💧 Economize água", reply.response);
        assertEquals(2, reply.chartData.getAsJsonArray("series").size());
    }

    @Test
    public void acceptsCurrentBackendAgentListAlongsideDeployedSingleAgent() {
        ChatModels.Reply reply = GsonProvider.getInstance().fromJson("{\"response\":\"Olá!\",\"agents_used\":[\"habits\"],\"session_id\":\"s2\"}", ChatModels.Reply.class);
        assertEquals("Olá!", reply.response);
        assertEquals("s2", reply.sessionId);
        assertEquals("habits", reply.agentsUsed.get(0));
        assertNull(reply.agentUsed);
    }
}
