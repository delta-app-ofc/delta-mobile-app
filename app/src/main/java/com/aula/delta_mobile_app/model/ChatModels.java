package com.aula.delta_mobile_app.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public final class ChatModels {
    private ChatModels() {}

    public static final class Request {
        @SerializedName("user_id") public final int userId;
        @SerializedName("session_id") public final String sessionId;
        public final String message;

        public Request(int userId, String sessionId, String message) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.message = message;
        }
    }

    public static final class Reply {
        public String response;
        @SerializedName("session_id") public String sessionId;
        @SerializedName("agent_used") public String agentUsed;
        @SerializedName("agents_used") public java.util.List<String> agentsUsed;
        @SerializedName("chart_data") public JsonObject chartData;
    }
}
