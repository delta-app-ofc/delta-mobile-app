package com.aula.delta_mobile_app.services.network;

import android.content.Context;

import com.aula.delta_mobile_app.BuildConfig;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.api.ChatbotApiService;
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

    public static com.aula.delta_mobile_app.services.api.MongoApiService createMongoService(AuthRepository auth) {
        OkHttpClient client = createHttpClient().newBuilder().followRedirects(false).followSslRedirects(false)
            .addInterceptor(new SessionInterceptor(auth.tokenStore(), auth.api(), BuildConfig.MONGO_API_URL, "api/")).build();
        return new Retrofit.Builder().baseUrl(BuildConfig.MONGO_API_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build()
            .create(com.aula.delta_mobile_app.services.api.MongoApiService.class);
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

    private static final class OpenMeteoHolder {
        private static final com.aula.delta_mobile_app.services.api.OpenMeteoApiService SERVICE = new Retrofit.Builder().baseUrl("https://api.open-meteo.com/")
            .client(createHttpClient()).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build()
            .create(com.aula.delta_mobile_app.services.api.OpenMeteoApiService.class);
    }

    public static com.aula.delta_mobile_app.services.api.OpenMeteoApiService getOpenMeteoService() { return OpenMeteoHolder.SERVICE; }

    private static final class CloudinaryHolder {
        private static final com.aula.delta_mobile_app.services.api.CloudinaryApiService SERVICE = new Retrofit.Builder().baseUrl("https://api.cloudinary.com/v1_1/")
            .client(createPublicImageClient()).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build()
            .create(com.aula.delta_mobile_app.services.api.CloudinaryApiService.class);
    }

    public static com.aula.delta_mobile_app.services.api.CloudinaryApiService getCloudinaryService() { return CloudinaryHolder.SERVICE; }

    public static OkHttpClient createPublicImageClient() {
        return createHttpClient().newBuilder().callTimeout(90, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();
    }

    private static final class ChatbotHolder {
        private static final ChatbotApiService SERVICE = createChatbotApiService(BuildConfig.CHATBOT_API_URL, createChatbotHttpClient());
    }

    public static ChatbotApiService getChatbotService() {
        return ChatbotHolder.SERVICE;
    }

    public static ChatbotApiService getChatbotService(Context context) {
        AuthRepository auth = AuthRepository.getInstance(context);
        OkHttpClient client = createChatbotHttpClient().newBuilder().addInterceptor(new ChatbotAuthInterceptor(auth.tokenStore())).build();
        return createChatbotApiService(BuildConfig.CHATBOT_API_URL, client);
    }

    static OkHttpClient createChatbotHttpClient() {
        return createHttpClient().newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
    }

    static ChatbotApiService createChatbotApiService(String baseUrl, OkHttpClient client) {
        return new Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(GsonConverterFactory.create(GsonProvider.getInstance())).build()
            .create(ChatbotApiService.class);
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
