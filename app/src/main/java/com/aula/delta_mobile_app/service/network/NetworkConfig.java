package com.aula.delta_mobile_app.service.network;

/** Valores compartilhados pela configuracao da camada de rede. */
public final class NetworkConfig {

    /**
     * URL temporaria e valida para a configuracao inicial do Retrofit.
     * Substitua pelo endereco real da API antes de implementar os endpoints.
     */
    public static final String BASE_URL = "https://example.com/";

    public static final long CONNECT_TIMEOUT_SECONDS = 30L;
    public static final long READ_TIMEOUT_SECONDS = 30L;
    public static final long WRITE_TIMEOUT_SECONDS = 30L;

    /** Ative somente durante o desenvolvimento para registrar requisicoes HTTP. */
    public static final boolean HTTP_LOGGING_ENABLED = false;

    private NetworkConfig() {
        throw new IllegalStateException("Classe utilitaria nao deve ser instanciada");
    }
}
