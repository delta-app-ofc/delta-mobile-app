package com.aula.delta_mobile_app.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public final class ChatModels {
    private ChatModels() {}

    public static class ChatRequest {
        @SerializedName("user_id")
        public final int userId;
        @SerializedName("session_id")
        public final String sessionId;
        public final String message;

        public ChatRequest(int userId, String message, String sessionId) {
            this.userId = userId;
            this.message = message;
            this.sessionId = sessionId;
        }
    }

    public static class ChatResponse {
        public String response;
        @SerializedName("agent_used")
        public String agentUsed;
        @SerializedName("session_id")
        public String sessionId;
        @SerializedName("chart_data")
        public JsonElement chartData;
    }
}
