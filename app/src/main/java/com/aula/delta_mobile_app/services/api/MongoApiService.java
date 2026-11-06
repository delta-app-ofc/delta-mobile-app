package com.aula.delta_mobile_app.services.api;
import com.aula.delta_mobile_app.model.AlertModels.*;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.Map;
public interface MongoApiService {
    @GET("api/alerts") Call<Page<Alert>> alerts(@Query("severity") String severity, @Query("page") int page, @Query("size") int size);
    @PATCH("api/alerts/{id}/dismiss") Call<Void> dismiss(@Path("id") String id);
    @GET("api/alerts/leak-summary") Call<Summary> summary();
    @GET("api/detection/devices/{id}/evidence") Call<Page<Window>> evidence(@Path("id") String id, @Query("start") String start, @Query("end") String end, @Query("page") int page, @Query("size") int size);
    @PUT("api/detection/devices/{id}/visit") Call<Map<String,Object>> visit(@Path("id") String id, @Body Map<String,Object> body);
}
