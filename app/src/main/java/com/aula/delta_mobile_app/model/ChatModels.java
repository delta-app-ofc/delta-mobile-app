package com.aula.delta_mobile_app.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;

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
        @SerializedName("agents_used")
        public List<String> agentsUsed;
        @SerializedName("session_id")
        public String sessionId;
        @SerializedName("chart_data")
        public JsonElement chartData;
    }

    public static class Session {
        @SerializedName("session_id")
        public String sessionId;
        @SerializedName("created_at")
        public String createdAt;
    }

    public static class SessionsResponse {
        public List<Session> sessions;
    }

    public static class HistoryMessage {
        public String role;
        public String content;
        public String timestamp;
    }

    public static class HistoryResponse {
        public List<HistoryMessage> messages;
    }

    public static class CloseResponse {
        public String status;
        @SerializedName("session_id")
        public String sessionId;
    }
}
