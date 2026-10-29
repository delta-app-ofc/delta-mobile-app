package com.aula.delta_mobile_app.services.network;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.IntFunction;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ChatbotErrors {
    private ChatbotErrors() {}

    public static <T> ScreenState<T> from(Response<?> response, IntFunction<String> strings) {
        return from(response, strings, System.currentTimeMillis());
    }

    static <T> ScreenState<T> from(Response<?> response, IntFunction<String> strings, long now) {
        int code = response.code();
        int message = R.string.chat_error_request;
        try (ResponseBody body = response.errorBody()) {
            if (code == 400 || code == 422) message = validationMessage(body);
            else if (code == 401 || code == 403) message = R.string.chat_error_access;
            else if (code == 429) message = R.string.chat_error_rate_limit;
            else if (code >= 500) message = R.string.chat_error_unavailable;
        }
        long retryAt = code == 429 ? retryAt(response.headers().get("Retry-After"), now) : 0;
        return ScreenState.error(strings.apply(message), code, retryAt);
    }

    public static <T> ScreenState<T> network(Throwable failure, IntFunction<String> strings) {
        if (failure instanceof JsonParseException || failure instanceof MalformedJsonException || failure instanceof EOFException) return invalidBody(strings);
        int message = failure instanceof InterruptedIOException ? R.string.chat_error_timeout : R.string.chat_error_network;
        return ScreenState.error(strings.apply(message));
    }

    public static <T> ScreenState<T> invalidBody(IntFunction<String> strings) {
        return ScreenState.error(strings.apply(R.string.chat_error_invalid_response));
    }

    private static int validationMessage(ResponseBody body) {
        if (body == null) return R.string.chat_error_validation;
        try {
            JsonElement json = JsonParser.parseString(body.string());
            if (!json.isJsonObject()) return R.string.chat_error_validation;
            JsonElement detail = json.getAsJsonObject().get("detail");
            if (detail == null) return R.string.chat_error_validation;
            if (detail.isJsonPrimitive() && detail.getAsJsonPrimitive().isString())
                return "Mensagem vazia.".equals(detail.getAsString()) ? R.string.chat_error_empty_message : R.string.chat_error_validation;
            if (!detail.isJsonArray() || detail.getAsJsonArray().size() != 1) return R.string.chat_error_validation;
            JsonElement item = detail.getAsJsonArray().get(0);
            if (!item.isJsonObject()) return R.string.chat_error_validation;
            JsonObject error = item.getAsJsonObject();
            JsonElement location = error.get("loc");
            if (location == null || !location.isJsonArray() || !"[\"body\",\"message\"]".equals(location.toString())) return R.string.chat_error_validation;
            JsonElement type = error.get("type");
            if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) return R.string.chat_error_validation;
            if ("string_too_short".equals(type.getAsString())) return R.string.chat_error_empty_message;
            if ("string_too_long".equals(type.getAsString())) return R.string.chat_error_message_length;
        } catch (IOException | RuntimeException invalid) {
            return R.string.chat_error_validation;
        }
        return R.string.chat_error_validation;
    }

    private static long retryAt(String header, long now) {
        if (header == null) return 0;
        try {
            long seconds = Long.parseLong(header.trim());
            return seconds < 0 ? 0 : Math.addExact(now, Math.multiplyExact(seconds, 1000));
        } catch (NumberFormatException invalidNumber) {
            try {
                return Math.max(now, ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli());
            } catch (RuntimeException invalidDate) {
                return 0;
            }
        } catch (ArithmeticException overflow) {
            return 0;
        }
    }
}
