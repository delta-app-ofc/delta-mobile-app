package com.aula.delta_mobile_app.services.api;

import com.aula.delta_mobile_app.model.ChatModels.*;

import retrofit2.Call;
import retrofit2.http.*;

public interface ChatbotApiService {
    @POST("chat")
    Call<ChatResponse> chat(@Body ChatRequest request);

    @GET("sessions/{user_id}")
    Call<SessionsResponse> sessions(@Path("user_id") int userId);

    @GET("sessions/{session_id}/history")
    Call<HistoryResponse> history(@Path("session_id") String sessionId, @Query("user_id") int userId);

    @POST("sessions/{session_id}/close")
    Call<CloseResponse> close(@Path("session_id") String sessionId, @Query("user_id") int userId);
}
