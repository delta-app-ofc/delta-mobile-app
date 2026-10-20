package com.aula.delta_mobile_app.services.network;

public final class NetworkConfig {
    public static final String BASE_URL = com.aula.delta_mobile_app.BuildConfig.DELTA_API_URL;

    public static final long CONNECT_TIMEOUT_SECONDS = 30L;

    public static final long READ_TIMEOUT_SECONDS = 90L;
    public static final long WRITE_TIMEOUT_SECONDS = 30L;

    public static final boolean HTTP_LOGGING_ENABLED = false;

    private NetworkConfig() {
        throw new IllegalStateException("Classe utilitaria nao deve ser instanciada");
    }
}
