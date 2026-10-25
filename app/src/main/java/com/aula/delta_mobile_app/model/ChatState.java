package com.aula.delta_mobile_app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatState {
    public final ScreenState<Integer> account;
    public final boolean showIntroduction;
    public final String draft;
    public final String sessionId;
    public final List<ChatMessage> messages;
    public final ScreenState<ChatMessage> sending;

    public ChatState(ScreenState<Integer> account, boolean showIntroduction, String draft, String sessionId, List<ChatMessage> messages, ScreenState<ChatMessage> sending) {
        this.account = account;
        this.showIntroduction = showIntroduction;
        this.draft = draft;
        this.sessionId = sessionId;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.sending = sending;
    }

    public static ChatState empty() {
        return forAccount(ScreenState.idle());
    }

    public static ChatState forAccount(ScreenState<Integer> account) {
        return new ChatState(account, true, "", null, Collections.emptyList(), ScreenState.idle());
    }
}
