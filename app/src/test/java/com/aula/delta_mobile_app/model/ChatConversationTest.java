package com.aula.delta_mobile_app.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChatConversationTest {
    @Test
    public void rejectsBlankAndOverLimitMessages() {
        ChatConversation chat = new ChatConversation();
        assertEquals(0, chat.begin(" \n "));
        assertEquals(0, chat.begin("a".repeat(2001)));
        assertEquals(0, chat.messages().size());
        assertTrue(chat.begin("💧".repeat(2000)) > 0);
    }

    @Test
    public void onlyOneRequestCanBePending() {
        ChatConversation chat = new ChatConversation();
        assertTrue(chat.begin(" Economizar água ") > 0);
        assertEquals(0, chat.begin("Outra pergunta"));
        assertEquals(0, chat.retry());
        assertEquals("Economizar água", chat.pendingText());
        assertEquals(1, chat.messages().size());
    }

    @Test
    public void replyCompletesMessageAndRetainsRemoteSession() {
        ChatConversation chat = new ChatConversation();
        long request = chat.begin("Rotina");
        assertTrue(chat.complete(request, "Banhos de até 8 minutos.\n💧 Evite desperdícios.", "session-1"));
        assertFalse(chat.pending());
        assertEquals("session-1", chat.sessionId());
        assertEquals(ChatConversation.Delivery.SENT, chat.messages().get(0).delivery);
        assertFalse(chat.messages().get(1).user);
        assertFalse(chat.complete(request, "Resposta duplicada", "session-1"));
        assertEquals(2, chat.messages().size());
    }

    @Test
    public void retryRetainsMessageAndRejectsLateFirstResponse() {
        ChatConversation chat = new ChatConversation();
        long first = chat.begin("Rotina");
        long id = chat.messages().get(0).id;
        assertTrue(chat.fail(first));
        assertEquals(0, chat.begin("Outra pergunta"));
        long retry = chat.retry();
        assertTrue(retry > first);
        assertEquals(1, chat.messages().size());
        assertEquals(id, chat.messages().get(0).id);
        assertFalse(chat.complete(first, "Resposta atrasada", "old"));
        assertTrue(chat.complete(retry, "Resposta atual", "new"));
        assertEquals(2, chat.messages().size());
    }

    @Test
    public void clearingConversationInvalidatesInFlightCallbacks() {
        ChatConversation chat = new ChatConversation();
        long previous = chat.begin("Mensagem anterior");
        chat.clear();
        long current = chat.begin("Nova sessão");
        assertFalse(chat.complete(previous, "Resposta de outra conta", "old"));
        assertFalse(chat.fail(previous));
        assertTrue(chat.pending());
        assertNull(chat.sessionId());
        assertTrue(chat.complete(current, "Resposta atual", "new"));
        assertEquals("Nova sessão", chat.messages().get(0).text);
    }

    @Test
    public void incompleteReplyEndsLoadingAndAllowsRetry() {
        ChatConversation chat = new ChatConversation();
        long request = chat.begin("Olá");
        chat.complete(request, "", "session");
        assertFalse(chat.pending());
        assertTrue(chat.failed());
        assertNull(chat.sessionId());
        assertTrue(chat.retry() > request);
    }

    @Test
    public void snapshotsDoNotChangeWhenConversationChanges() {
        ChatConversation chat = new ChatConversation();
        long request = chat.begin("Olá");
        java.util.List<ChatConversation.Message> snapshot = chat.messages();
        chat.complete(request, "Olá!", "session");
        assertEquals(1, snapshot.size());
        assertEquals(ChatConversation.Delivery.PENDING, snapshot.get(0).delivery);
    }
}
