package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.model.ScreenState;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Locale;
import retrofit2.Response;

public final class ChatErrors {
    private ChatErrors() {}

    public static <T> ScreenState<T> from(Response<?> response) {
        int code = response.code();
        String detail = null;
        if (response.errorBody() != null) {
            try (okhttp3.ResponseBody body = response.errorBody()) {
                if (code >= 400 && code < 500 && body.contentLength() <= 8192) detail = detail(body.string());
            } catch (Exception ignored) {}
        }
        String message;
        switch (code) {
            case 400: message = "O Gluh não aceitou a mensagem."; break;
            case 401: case 403: message = "O serviço de IA não autorizou esta conta."; break;
            case 404: message = "A conversa ou a rota de chat não foi encontrada no serviço de IA."; break;
            case 422: message = "O serviço de IA não conseguiu validar os dados da mensagem."; break;
            case 429: message = "Muitas mensagens. Aguarde um pouco antes de tentar novamente."; break;
            case 502: case 503: case 504: message = "O serviço de IA está temporariamente indisponível. Tente novamente em instantes."; break;
            default: message = code >= 500 ? "O serviço de IA falhou ao gerar a resposta. Tente novamente mais tarde."
                : "O serviço de IA retornou uma resposta inesperada.";
        }
        if (detail != null) message += " " + detail;
        return ScreenState.error(message + " (HTTP " + code + ")", code, 0);
    }

    private static String detail(String body) {
        try {
            JsonElement json = JsonParser.parseString(body);
            if (!json.isJsonObject()) return null;
            for (String key : new String[] { "detail", "message" }) {
                JsonElement field = json.getAsJsonObject().get(key);
                if (field == null || !field.isJsonPrimitive() || !field.getAsJsonPrimitive().isString()) continue;
                String value = field.getAsString().trim();
                String lower = value.toLowerCase(Locale.ROOT);
                if (!value.isEmpty() && value.length() <= 240 && !lower.matches("(?s).*\\b(sql|jdbc|exception|traceback|stacktrace|token|password|secret|api_key)\\b.*")) return value;
            }
        } catch (RuntimeException ignored) {}
        return null;
    }
}
