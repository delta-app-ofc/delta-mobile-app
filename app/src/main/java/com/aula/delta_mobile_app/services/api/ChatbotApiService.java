package com.aula.delta_mobile_app.services.api;

import com.aula.delta_mobile_app.model.ChatModels.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatbotApiService {
    @POST("chat")
    Call<ChatResponse> chat(@Body ChatRequest request);

    @GET("sessions")
    Call<SessionsResponse> sessions(@Query("limit") Integer limit, @Query("offset") Integer offset);

    @GET("sessions/{session_id}/messages")
    Call<HistoryResponse> history(@Path("session_id") String sessionId);

    @POST("sessions/{session_id}/close")
    Call<CloseResponse> close(@Path("session_id") String sessionId);
}
