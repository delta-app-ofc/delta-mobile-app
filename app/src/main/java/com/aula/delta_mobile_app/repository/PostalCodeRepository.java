package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.PostalCodeResponse;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.ApiErrors;
import com.aula.delta_mobile_app.services.network.RetrofitClient;

import retrofit2.*;

import java.util.function.Consumer;

public final class PostalCodeRepository {
    private Call<PostalCodeResponse> current;
    private final ApiService api;

    public PostalCodeRepository() {
        this(RetrofitClient.getCepService());
    }

    public PostalCodeRepository(ApiService api) {
        this.api = api;
    }

    public void cancel() {
        if (current != null) current.cancel();
        current = null;
    }

    public void getCep(String postalCode, Consumer<ScreenState<PostalCodeResponse>> callback) {
        cancel();
        Call<PostalCodeResponse> request = api.getCep(postalCode);
        current = request;
        request.enqueue(new Callback<PostalCodeResponse>() {
            @Override
            public void onResponse(Call<PostalCodeResponse> call, Response<PostalCodeResponse> response) {
                if (current != call) return;
                current = null;
                PostalCodeResponse value = response.body();
                if (!response.isSuccessful() || value == null || value.city == null || value.state == null || value.city.trim().isEmpty() || value.state.trim().isEmpty()
                    || value.city.length() > 60 || value.state.length() > 30) {
                    if (response.errorBody() != null) response.errorBody().close();
                    callback.accept(ScreenState.error(response.code() == 404 ? "CEP não encontrado." : "Não foi possível consultar o CEP. Tente" + " novamente."));
                } else callback.accept(ScreenState.success(value));
            }

            @Override
            public void onFailure(Call<PostalCodeResponse> call, Throwable failure) {
                if (current != call || call.isCanceled()) return;
                current = null;
                callback.accept(ApiErrors.network());
            }
        });
    }
}
