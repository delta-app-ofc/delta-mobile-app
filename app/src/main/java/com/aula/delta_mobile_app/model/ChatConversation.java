package com.aula.delta_mobile_app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatConversation {
    public enum Delivery { PENDING, SENT, FAILED }

    public static final class Message {
        public final long id;
        public final boolean user;
        public final String text;
        public final Delivery delivery;

        public Message(long id, boolean user, String text, Delivery delivery) {
            this.id = id;
            this.user = user;
            this.text = text;
            this.delivery = delivery;
        }
    }

    private final List<Message> messages = new ArrayList<>();
    private long sequence, revision;
    private boolean pending;
    private String sessionId;

    public List<Message> messages() { return Collections.unmodifiableList(new ArrayList<>(messages)); }
    public boolean pending() { return pending; }
    public String sessionId() { return sessionId; }
    public boolean failed() { return !messages.isEmpty() && messages.get(messages.size() - 1).delivery == Delivery.FAILED; }

    public long begin(String draft) {
        String text = draft == null ? "" : draft.trim();
        if (pending || failed() || text.isEmpty() || text.codePointCount(0, text.length()) > 2000) return 0;
        messages.add(new Message(++sequence, true, text, Delivery.PENDING));
        pending = true;
        return ++revision;
    }

    public long retry() {
        if (pending || !failed()) return 0;
        replaceLast(Delivery.PENDING);
        pending = true;
        return ++revision;
    }

    public String pendingText() { return pending ? messages.get(messages.size() - 1).text : ""; }

    public boolean complete(long request, String text, String session) {
        if (!pending || request != revision) return false;
        if (text == null || text.trim().isEmpty() || session == null || session.trim().isEmpty()) return fail(request);
        replaceLast(Delivery.SENT);
        messages.add(new Message(++sequence, false, text, Delivery.SENT));
        sessionId = session;
        pending = false;
        return true;
    }

    public boolean fail(long request) {
        if (!pending || request != revision) return false;
        replaceLast(Delivery.FAILED);
        pending = false;
        return true;
    }

    public void clear() {
        revision++;
        pending = false;
        sessionId = null;
        messages.clear();
    }

    private void replaceLast(Delivery delivery) {
        Message last = messages.get(messages.size() - 1);
        messages.set(messages.size() - 1, new Message(last.id, true, last.text, delivery));
    }
}
