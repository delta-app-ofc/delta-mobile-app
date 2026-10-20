package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.model.ScreenState;

import retrofit2.Response;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class ApiErrors {
    private ApiErrors() {}

    public static <T> ScreenState<T> from(Response<?> response) {
        int code = response.code();
        String serverMessage = businessMessage(response, code);
        String message;
        switch (code) {
        case 400: message = "Confira os dados informados.";
            break;
        case 401: message = "Credenciais inválidas ou sessão expirada. Entre novamente.";
            break;
        case 403: message = "Acesso negado para esta conta.";
            break;
        case 409: message = "Há dados em conflito com um cadastro existente. Confira os dados" + " informados.";
            break;
        case 429: message = "Muitas tentativas. Aguarde antes de tentar novamente.";
            break;
        default: message = code >= 500 ? "Serviço indisponível. Tente novamente mais tarde." : "Não foi possível concluir a solicitação.";
        }
        if (serverMessage != null) message = serverMessage;
        long retryAt = 0;
        if (code == 429) {
            retryAt = System.currentTimeMillis() + 60000;
            String header = response.headers().get("Retry-After");
            if (header != null) {
                try {
                    retryAt = System.currentTimeMillis() + Math.max(0, Long.parseLong(header)) * 1000;
                } catch (NumberFormatException ignored) {
                    try {
                        retryAt = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
                    } catch (RuntimeException ignoredDate) {
                    }
                }
            }
        }
        return ScreenState.error(message, code, retryAt);
    }

    public static <T> ScreenState<T> network() {
        return ScreenState.error("Não foi possível conectar. Confira sua conexão e tente novamente.");
    }

    private static String businessMessage(Response<?> response, int code) {
        if (response.errorBody() == null) return null;
        try (okhttp3.ResponseBody body = response.errorBody()) {
            if (code != 400 && code != 409) return null;
            com.google.gson.JsonElement json = com.google.gson.JsonParser.parseString(body.string());
            if (!json.isJsonObject()) return null;
            com.google.gson.JsonElement field = json.getAsJsonObject().get("message");
            if (field == null || !field.isJsonPrimitive() || !field.getAsJsonPrimitive().isString())
                return null;
            String value = field.getAsString().trim();
            if (value.isEmpty() || value.length() > 300 || value.matches("(?is).*\\b(sql|jdbc|constraint|exception|insert|select|stacktrace)\\b.*")
                || value.toLowerCase(java.util.Locale.ROOT).contains("duplicate key"))
            return null;
            return value;
        } catch (Exception invalid) {
            return null;
        }
    }
}
