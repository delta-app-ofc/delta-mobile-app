package com.aula.delta_mobile_app.service.network;

import com.aula.delta_mobile_app.service.api.ApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Centraliza a criacao e o acesso as instancias de Retrofit e ApiService. */
public final class RetrofitClient {

    private RetrofitClient() {
        throw new IllegalStateException("Classe utilitaria nao deve ser instanciada");
    }

    private static final class Holder {
        private static final Retrofit RETROFIT = createRetrofit();
        private static final ApiService API_SERVICE = RETROFIT.create(ApiService.class);
    }

    public static Retrofit getInstance() {
        return Holder.RETROFIT;
    }

    public static ApiService getApiService() {
        return Holder.API_SERVICE;
    }

    private static Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(NetworkConfig.BASE_URL)
                .client(createHttpClient())
                .addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance()))
                .build();
    }

    private static OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (NetworkConfig.HTTP_LOGGING_ENABLED) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(loggingInterceptor);
        }

        return builder.build();
    }
}
