package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ChatModels.ChatResponse;
import com.aula.delta_mobile_app.model.ChatModels.HistoryMessage;
import com.aula.delta_mobile_app.model.ChatModels.Session;
import com.aula.delta_mobile_app.model.ChatModels.CloseResponse;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.ChatActions;
import com.aula.delta_mobile_app.services.network.ChatbotErrors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

public final class ChatFlow {
    public static final class SessionEntry {
        public final String id;
        public final String createdAt;
        public final Instant created;

        public SessionEntry(String id, String createdAt) {
            this.id = Objects.requireNonNull(id);
            this.createdAt = createdAt;
            Instant parsed = null;
            try {
                if (createdAt != null) parsed = OffsetDateTime.parse(createdAt).toInstant();
            } catch (DateTimeParseException ignored) {}
            created = parsed;
        }
    }

    public static final class HistoryState {
        public final ScreenState<List<SessionEntry>> sessions;
        public final ScreenState<List<ChatMessage>> history;
        public final String selectedId;
        public final String notice;
        public final ScreenState<String> closing;

        private HistoryState(ScreenState<List<SessionEntry>> sessions, ScreenState<List<ChatMessage>> history, String selectedId, String notice, ScreenState<String> closing) {
            this.sessions = sessions;
            this.history = history;
            this.selectedId = selectedId;
            this.notice = notice;
            this.closing = closing;
        }
    }

    private final ChatActions actions;
    private final Consumer<Runnable> dispatch;
    private final Consumer<ChatState> observer;
    private final IntFunction<String> strings;
    private final LongSupplier now;
    private ChatState state;
    private long nextId, attempt, generation;
    private boolean active = true;
    private ScreenState<List<SessionEntry>> sessions = ScreenState.idle();
    private ScreenState<List<ChatMessage>> history = ScreenState.idle();
    private String selectedId, historyNotice = "";
    private long listAttempt, historyAttempt;
    private long closeAttempt;
    private ScreenState<String> closing = ScreenState.idle();
    private Consumer<HistoryState> historyObserver = value -> {};

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

    public HistoryState historyState() {
        return new HistoryState(sessions, history, selectedId, historyNotice, closing);
    }

    public void showIntroduction() {
        if (active) publish(new ChatState(state.account, true, state.draft, state.sessionId, state.messages, state.sending));
    }

    public void resumeConversation() {
        if (active) publish(new ChatState(state.account, false, state.draft, state.sessionId, state.messages, state.sending));
    }

    public void newChat(boolean discardDraft) {
        if (!active || closing.busy() || state.sending.busy() || history.busy() || now.getAsLong() < closing.retryAt) return;
        if (!state.draft.isEmpty() && !discardDraft) {
            historyNotice = strings.apply(R.string.chat_new_confirm_message);
            publishHistory();
            return;
        }
        String session = state.sessionId;
        if (session == null) {
            closing = ScreenState.idle();
            resetConversation();
            return;
        }
        long identity = generation, request = ++closeAttempt;
        historyNotice = "";
        closing = ScreenState.loading();
        publishHistory();
        actions.close(session, result -> dispatch.accept(() -> {
            if (!active || generation != identity || request != closeAttempt || !closing.busy() || !Objects.equals(state.sessionId, session)) return;
            CloseResponse response = result == null ? null : result.data;
            if (result != null && result.status == ScreenState.Status.SUCCESS && response != null && "ok".equals(response.status) && session.equals(response.sessionId)) {
                closing = ScreenState.success(session);
                resetConversation();
            } else {
                closing = failure(result != null && result.status == ScreenState.Status.ERROR ? result : ChatbotErrors.invalidBody(strings));
                publishHistory();
            }
        }));
    }

    private void resetConversation() {
        attempt++;
        historyAttempt++;
        selectedId = null;
        history = ScreenState.idle();
        historyNotice = "";
        publishHistory();
        publish(new ChatState(state.account, true, "", null, Collections.emptyList(), ScreenState.idle()));
    }

    public void observeHistory(Consumer<HistoryState> observer) {
        historyObserver = Objects.requireNonNull(observer);
        publishHistory();
    }

    public void loadSessions() {
        if (!active || sessions.busy() || sessions.status == ScreenState.Status.SUCCESS || now.getAsLong() < sessions.retryAt) return;
        long identity = generation, request = ++listAttempt;
        sessions = ScreenState.loading();
        publishHistory();
        actions.sessions(result -> dispatch.accept(() -> {
            if (!active || identity != generation || request != listAttempt || !sessions.busy()) return;
            if (result != null && result.status == ScreenState.Status.SUCCESS && result.data != null) {
                List<SessionEntry> entries = new ArrayList<>();
                for (Session session : result.data) {
                    if (session == null || session.sessionId == null || session.sessionId.isEmpty() || entries.stream().anyMatch(entry -> entry.id.equals(session.sessionId))) {
                        sessions = ChatbotErrors.invalidBody(strings);
                        publishHistory();
                        return;
                    }
                    entries.add(new SessionEntry(session.sessionId, session.createdAt));
                }
                entries.sort(Comparator.comparing((SessionEntry entry) -> entry.created, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(entry -> entry.id));
                sessions = ScreenState.success(Collections.unmodifiableList(entries));
            } else sessions = failure(result);
            publishHistory();
        }));
    }

    public void openSession(String sessionId) {
        if (!active || closing.busy() || sessions.data == null || sessions.data.stream().noneMatch(entry -> entry.id.equals(sessionId))) return;
        if (state.sending.busy() || !state.draft.isEmpty()) {
            historyNotice = strings.apply(state.sending.busy() ? R.string.chat_history_busy : R.string.chat_history_draft);
            publishHistory();
            return;
        }
        if (Objects.equals(selectedId, sessionId) && (history.busy() || history.status == ScreenState.Status.SUCCESS || now.getAsLong() < history.retryAt)) return;
        selectedId = sessionId;
        closing = ScreenState.idle();
        closeAttempt++;
        historyNotice = "";
        history = ScreenState.loading();
        long identity = generation, request = ++historyAttempt;
        publishHistory();
        publish(new ChatState(state.account, false, "", sessionId, Collections.emptyList(), ScreenState.idle()));
        actions.history(sessionId, result -> dispatch.accept(() -> {
            if (!active || identity != generation || request != historyAttempt || !Objects.equals(selectedId, sessionId) || !history.busy()) return;
            if (result != null && result.status == ScreenState.Status.SUCCESS && result.data != null) {
                List<ChatMessage> messages = new ArrayList<>();
                for (HistoryMessage message : result.data) {
                    if (message == null || message.content == null || !("human".equals(message.role) || "assistant".equals(message.role))) {
                        history = ChatbotErrors.invalidBody(strings);
                        publishHistory();
                        return;
                    }
                    messages.add(new ChatMessage(++nextId, "human".equals(message.role) ? ChatMessage.Author.USER : ChatMessage.Author.ASSISTANT,
                        message.content, ScreenState.Status.SUCCESS, null));
                }
                history = ScreenState.success(Collections.unmodifiableList(messages));
                publish(new ChatState(state.account, state.showIntroduction, "", sessionId, messages, ScreenState.idle()));
            } else history = failure(result);
            publishHistory();
        }));
    }

    private <T> ScreenState<T> failure(ScreenState<?> result) {
        return result != null && result.status == ScreenState.Status.ERROR ? ScreenState.error(result.message, result.code, result.retryAt) : ChatbotErrors.invalidBody(strings);
    }

    private void publishHistory() {
        historyObserver.accept(historyState());
    }

    public void setDraft(String value) {
        if (!active || closing.busy()) return;
        publish(new ChatState(state.account, state.showIntroduction, value == null ? "" : value, state.sessionId, state.messages, state.sending));
    }

    public void send() {
        if (!active || closing.busy() || state.sending.busy() || history.busy() || history.status == ScreenState.Status.ERROR || now.getAsLong() < state.sending.retryAt
            || state.showIntroduction && (state.sessionId != null || !state.messages.isEmpty())) return;
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
        sessions = ScreenState.idle();
        history = ScreenState.idle();
        selectedId = null;
        historyNotice = "";
        closing = ScreenState.idle();
        publishHistory();
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
            publish(new ChatState(state.account, state.showIntroduction, state.draft.equals(content) ? "" : state.draft, response.sessionId, messages, ScreenState.success(reply)));
            if (session == null && (sessions.busy() || sessions.status == ScreenState.Status.SUCCESS)) {
                sessions = ScreenState.idle();
                loadSessions();
            }
        } else {
            publish(new ChatState(state.account, state.showIntroduction, state.draft, session, messages, ScreenState.error(result.message, result.code, result.retryAt)));
        }
    }

    private void publish(ChatState next) {
        state = next;
        observer.accept(next);
        if (!state.sending.busy() && state.draft.isEmpty() && !historyNotice.isEmpty()) {
            historyNotice = "";
            publishHistory();
        }
    }
}
