package com.aula.delta_mobile_app.services.network;

import android.content.Context;

import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.session.TokenStore;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class RetrofitClient {
    private RetrofitClient() {
        throw new IllegalStateException("Classe utilitaria nao deve ser instanciada");
    }

    public static ApiService getApiService(Context context) {
        return AuthRepository.getInstance(context).api();
    }

    public static ApiService createApiService(TokenStore store) {
        return createSessionRetrofit(store).create(ApiService.class);
    }

    private static Retrofit createSessionRetrofit(TokenStore store) {
        ApiService publicApi = new Retrofit.Builder().baseUrl(NetworkConfig.BASE_URL).client(createHttpClient().newBuilder().followRedirects(false).followSslRedirects(false)
            .build()).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build().create(ApiService.class);
        OkHttpClient protectedClient = createHttpClient().newBuilder().followRedirects(false).followSslRedirects(false).addInterceptor(new SessionInterceptor(store, publicApi))
            .build();
        return new Retrofit.Builder().baseUrl(NetworkConfig.BASE_URL).client(protectedClient).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build();
    }

    private static final class BrasilApiHolder {
        private static final ApiService SERVICE = new Retrofit.Builder().baseUrl("https://brasilapi.com.br/api/").client(createHttpClient()).addConverterFactory(
            GsonConverterFactory.create(GsonProvider.getInstance())).build().create(ApiService.class);
    }

    public static ApiService getCepService() {
        return BrasilApiHolder.SERVICE;
    }

    private static OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (NetworkConfig.HTTP_LOGGING_ENABLED) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(loggingInterceptor);
        }

        return builder.build();
    }
}
