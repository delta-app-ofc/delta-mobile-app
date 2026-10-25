package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ChatModels.ChatResponse;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.ChatActions;
import com.aula.delta_mobile_app.services.network.ChatbotErrors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

public final class ChatFlow {
    private final ChatActions actions;
    private final Consumer<Runnable> dispatch;
    private final Consumer<ChatState> observer;
    private final IntFunction<String> strings;
    private final LongSupplier now;
    private ChatState state;
    private long nextId, attempt, generation;
    private boolean active = true;

    public ChatFlow(ChatActions actions, int userId, Consumer<Runnable> dispatch, Consumer<ChatState> observer, IntFunction<String> strings, LongSupplier now) {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive");
        this.actions = Objects.requireNonNull(actions);
        this.dispatch = Objects.requireNonNull(dispatch);
        this.observer = Objects.requireNonNull(observer);
        this.strings = Objects.requireNonNull(strings);
        this.now = Objects.requireNonNull(now);
        state = ChatState.forAccount(ScreenState.success(userId));
    }

    public ChatState state() {
        return state;
    }

    public void setDraft(String value) {
        if (!active) return;
        publish(new ChatState(state.account, state.showIntroduction, value == null ? "" : value, state.sessionId, state.messages, state.sending));
    }

    public void send() {
        if (!active || state.sending.busy() || now.getAsLong() < state.sending.retryAt) return;
        String content = state.draft;
        if (content.codePoints().allMatch(value -> Character.isWhitespace(value) || Character.isSpaceChar(value))) {
            invalidInput(R.string.chat_error_empty_message);
            return;
        }
        if (content.codePointCount(0, content.length()) > 2000) {
            invalidInput(R.string.chat_error_message_length);
            return;
        }
        List<ChatMessage> messages = new ArrayList<>(state.messages);
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        long id;
        if (last != null && last.author == ChatMessage.Author.USER && last.status == ScreenState.Status.ERROR && last.content.equals(content)) {
            id = last.id;
            messages.set(messages.size() - 1, last.withStatus(ScreenState.Status.LOADING));
        } else {
            id = ++nextId;
            messages.add(new ChatMessage(id, ChatMessage.Author.USER, content, ScreenState.Status.LOADING, null));
        }
        String session = state.sessionId;
        long request = ++attempt;
        long identity = generation;
        publish(new ChatState(state.account, false, state.draft, session, messages, ScreenState.loading()));
        actions.send(content, session, result -> dispatch.accept(() -> finish(identity, request, id, content, session, result)));
    }

    public void invalidate() {
        active = false;
        generation++;
        publish(ChatState.empty());
    }

    private void invalidInput(int message) {
        publish(new ChatState(state.account, state.showIntroduction, state.draft, state.sessionId, state.messages, ScreenState.error(strings.apply(message))));
    }

    private void finish(long identity, long request, long id, String content, String session, ScreenState<ChatResponse> result) {
        if (!active || generation != identity || attempt != request || !state.sending.busy() || !Objects.equals(state.sessionId, session)) return;
        ChatResponse response = result == null ? null : result.data;
        boolean success = result != null && result.status == ScreenState.Status.SUCCESS;
        if (success && (response == null || response.response == null || response.response.trim().isEmpty() || response.sessionId == null || response.sessionId.isEmpty()
            || (session != null && !session.equals(response.sessionId)))) {
            result = ChatbotErrors.invalidBody(strings);
            success = false;
        }
        if (!success && (result == null || result.status != ScreenState.Status.ERROR)) result = ChatbotErrors.invalidBody(strings);
        List<ChatMessage> messages = new ArrayList<>(state.messages);
        for (int index = 0; index < messages.size(); index++)
            if (messages.get(index).id == id) messages.set(index, messages.get(index).withStatus(success ? ScreenState.Status.SUCCESS : ScreenState.Status.ERROR));
        if (success) {
            ChatMessage reply = new ChatMessage(++nextId, ChatMessage.Author.ASSISTANT, response.response, ScreenState.Status.SUCCESS, response.agentUsed);
            messages.add(reply);
            publish(new ChatState(state.account, false, state.draft.equals(content) ? "" : state.draft, response.sessionId, messages, ScreenState.success(reply)));
        } else {
            publish(new ChatState(state.account, false, state.draft, session, messages, ScreenState.error(result.message, result.code, result.retryAt)));
        }
    }

    private void publish(ChatState next) {
        state = next;
        observer.accept(next);
    }
}
