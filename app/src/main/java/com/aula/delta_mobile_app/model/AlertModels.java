package com.aula.delta_mobile_app.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
public final class AlertModels {
    private AlertModels() {}
    public static class Page<T> { public List<T> content; public Paging page; }
    public static class Paging { public int totalPages; public long totalElements; }
    public static class Alert {
        @SerializedName("_id") public String id;
        @SerializedName("device_id") public String deviceId;
        @SerializedName("property_id") public Integer propertyId;
        @SerializedName("alert_type") public String type;
        @SerializedName("triggered_at") public String triggeredAt;
        @SerializedName("resolved_at") public String resolvedAt;
        public String title, message, severity, source;
        public Map<String,Object> evidence;
    }
    public static class Summary {
        @SerializedName("liters_per_day") public double litersPerDay;
        public int points;
    }
    public static class Window {
        @SerializedName("window_started_at") public String start;
        @SerializedName("window_finished_at") public String end;
        @SerializedName("lpm_average") public Double lpm;
        @SerializedName("consumption_liters") public Double liters;
        @SerializedName("anomaly_evidence") public Map<String,Object> evidence;
    }
}
