package com.aula.delta_mobile_app.services.api;

import com.aula.delta_mobile_app.model.ChatModels;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatApiService {
    @POST("chat")
    Call<ChatModels.Reply> chat(@Body ChatModels.Request request);
}
