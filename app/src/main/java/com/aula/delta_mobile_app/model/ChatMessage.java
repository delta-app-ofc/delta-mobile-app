package com.aula.delta_mobile_app.model;

import java.util.Objects;

public final class ChatMessage {
    public enum Author {
        USER, ASSISTANT
    }

    public final long id;
    public final Author author;
    public final String content;
    public final ScreenState.Status status;
    public final String agentUsed;

    public ChatMessage(long id, Author author, String content, ScreenState.Status status, String agentUsed) {
        this.id = id;
        this.author = Objects.requireNonNull(author);
        this.content = Objects.requireNonNull(content);
        this.status = Objects.requireNonNull(status);
        this.agentUsed = agentUsed;
    }

    public ChatMessage withStatus(ScreenState.Status status) {
        return new ChatMessage(id, author, content, status, agentUsed);
    }
}
