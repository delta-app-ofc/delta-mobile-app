package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ChatbotApiService;
import com.aula.delta_mobile_app.services.network.ChatbotErrors;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ChatRepository implements ChatActions {
    private final ChatbotApiService api;
    private final int userId;
    private final IntFunction<String> strings;

    public ChatRepository(ChatbotApiService api, int userId, IntFunction<String> strings) {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        this.api = Objects.requireNonNull(api);
        this.userId = userId;
        this.strings = Objects.requireNonNull(strings);
    }

    @Override
    public void sessions(Consumer<ScreenState<List<Session>>> callback) {
        request(api.sessions(userId), body -> {
            if (body.sessions == null) return null;
            for (Session session : body.sessions)
                if (session == null || !present(session.sessionId) || !text(session.createdAt)) return null;
            return body.sessions;
        }, callback);
    }

    @Override
    public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
        request(api.history(Objects.requireNonNull(sessionId), userId), body -> {
            if (body.messages == null) return null;
            for (HistoryMessage message : body.messages)
                if (message == null || !text(message.role) || !text(message.content) || !text(message.timestamp)) return null;
            return body.messages;
        }, callback);
    }

    private <S, T> void request(Call<S> call, Function<S, T> read, Consumer<ScreenState<T>> callback) {
        Objects.requireNonNull(callback);
        call.enqueue(new Callback<S>() {
            @Override
            public void onResponse(Call<S> request, Response<S> response) {
                if (!response.isSuccessful()) {
                    callback.accept(ChatbotErrors.from(response, strings));
                    return;
                }
                T value = response.body() == null ? null : read.apply(response.body());
                callback.accept(value == null ? ChatbotErrors.invalidBody(strings) : ScreenState.success(value));
            }

            @Override
            public void onFailure(Call<S> request, Throwable failure) {
                callback.accept(ChatbotErrors.network(failure, strings));
            }
        });
    }

    private static boolean present(String value) {
        return value != null && !value.isEmpty();
    }

    private static boolean text(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
