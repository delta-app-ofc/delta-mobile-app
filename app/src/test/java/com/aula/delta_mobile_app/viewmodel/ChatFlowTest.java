package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.ChatActions;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.junit.Assert.*;

public class ChatFlowTest {
    private static class Gateway implements ChatActions {
        final List<String> messages = new ArrayList<>(), sessions = new ArrayList<>();
        final List<Consumer<ScreenState<ChatResponse>>> replies = new ArrayList<>();

        @Override
        public void sessions(Consumer<ScreenState<List<Session>>> callback) {
            throw new AssertionError("Session listing belongs to a later stage");
        }

        @Override
        public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
            throw new AssertionError("History belongs to a later stage");
        }

        @Override
        public void send(String message, String sessionId, Consumer<ScreenState<ChatResponse>> callback) {
            messages.add(message);
            sessions.add(sessionId);
            replies.add(callback);
        }

        @Override
        public void close(String sessionId, Consumer<ScreenState<CloseResponse>> callback) {
            throw new AssertionError("Closing belongs to a later stage");
        }
    }

    private final Gateway gateway = new Gateway();
    private final List<Runnable> dispatched = new ArrayList<>();
    private final List<ChatState> states = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(1000);
    private final IntFunction<String> strings = id -> "resource:" + id;
    private final ChatFlow flow = new ChatFlow(gateway, 42, dispatched::add, states::add, strings, now::get);

    private ScreenState<ChatResponse> reply(String text, String session) {
        ChatResponse response = new ChatResponse();
        response.response = text;
        response.sessionId = session;
        response.agentUsed = "habits, rag";
        return ScreenState.success(response);
    }

    private void drain() {
        while (!dispatched.isEmpty()) dispatched.remove(0).run();
    }

    private void send(String text) {
        flow.setDraft(text);
        flow.send();
    }

    @Test
    public void startsWithAccountAndIntroductionWithoutRequestsOrFakeMessages() {
        ChatState state = flow.state();

        assertEquals(Integer.valueOf(42), state.account.data);
        assertEquals(ScreenState.Status.SUCCESS, state.account.status);
        assertTrue(state.showIntroduction);
        assertEquals("", state.draft);
        assertNull(state.sessionId);
        assertTrue(state.messages.isEmpty());
        assertEquals(ScreenState.Status.IDLE, state.sending.status);
        assertTrue(gateway.messages.isEmpty());
    }

    @Test
    public void editsDraftWithoutSendingOrChangingEarlierSnapshots() {
        ChatState initial = flow.state();
        flow.setDraft("Água\n💧");
        ChatState edited = flow.state();
        flow.setDraft("Outra pergunta");

        assertEquals("", initial.draft);
        assertEquals("Água\n💧", edited.draft);
        assertEquals("Outra pergunta", flow.state().draft);
        assertTrue(gateway.messages.isEmpty());
        assertTrue(flow.state().messages.isEmpty());
    }

    @Test
    public void rejectsEmptyWhitespaceAndOverlongMessagesWithoutRequests() {
        for (String text : new String[] {"", " \n\t", "\u00a0\u2003", "a".repeat(2001)}) {
            send(text);
            assertEquals(ScreenState.Status.ERROR, flow.state().sending.status);
            assertEquals(strings.apply(text.length() > 2000 ? R.string.chat_error_message_length : R.string.chat_error_empty_message), flow.state().sending.message);
            assertEquals(text, flow.state().draft);
            assertTrue(flow.state().messages.isEmpty());
        }
        assertTrue(gateway.messages.isEmpty());
    }

    @Test
    public void messageLimitCountsUnicodeCodePointsAndPreservesOriginalContent() {
        String text = "💧".repeat(2000);
        send(text);

        assertEquals(1, gateway.messages.size());
        assertEquals(text, gateway.messages.get(0));
        assertEquals(ScreenState.Status.LOADING, flow.state().sending.status);
    }

    @Test
    public void startsOnePendingMessageWithoutClearingDraftOrCreatingSession() {
        String text = "  Como economizar?\nÁgua 💧  ";
        send(text);

        assertEquals(1, gateway.messages.size());
        assertEquals(text, gateway.messages.get(0));
        assertNull(gateway.sessions.get(0));
        assertEquals(text, flow.state().draft);
        assertEquals(1, flow.state().messages.size());
        assertEquals(ChatMessage.Author.USER, flow.state().messages.get(0).author);
        assertEquals(ScreenState.Status.LOADING, flow.state().messages.get(0).status);
        assertTrue(flow.state().sending.busy());
        assertFalse(flow.state().showIntroduction);
    }

    @Test
    public void secondSendIsBlockedWhileWaitingEvenIfDraftChanges() {
        send("Primeira");
        long firstId = flow.state().messages.get(0).id;
        flow.send();
        flow.setDraft("Segunda");
        flow.send();

        assertEquals(1, gateway.messages.size());
        assertEquals(1, flow.state().messages.size());
        assertEquals(firstId, flow.state().messages.get(0).id);
        assertEquals("Segunda", flow.state().draft);
    }

    @Test
    public void replyOnlyUpdatesStateThroughInjectedDispatcher() {
        send("Água");
        ChatState pending = flow.state();
        gateway.replies.get(0).accept(reply("Confira suas torneiras.\n💧", "opaque-session"));

        assertSame(pending, flow.state());
        assertEquals(1, dispatched.size());
        drain();

        assertEquals(ScreenState.Status.SUCCESS, flow.state().sending.status);
        assertEquals("opaque-session", flow.state().sessionId);
        assertEquals("", flow.state().draft);
        assertEquals(2, flow.state().messages.size());
        ChatMessage human = flow.state().messages.get(0), assistant = flow.state().messages.get(1);
        assertEquals(ScreenState.Status.SUCCESS, human.status);
        assertEquals(ChatMessage.Author.ASSISTANT, assistant.author);
        assertEquals("Confira suas torneiras.\n💧", assistant.content);
        assertEquals("habits, rag", assistant.agentUsed);
        assertNotEquals(human.id, assistant.id);
        assertEquals(ScreenState.Status.LOADING, pending.messages.get(0).status);
        assertEquals(1, pending.messages.size());
    }

    @Test
    public void successfulReplyDoesNotEraseDraftEditedWhileWaiting() {
        send("Primeira");
        flow.setDraft("Próxima pergunta");
        gateway.replies.get(0).accept(reply("Resposta", "session-a"));
        drain();

        assertEquals("Próxima pergunta", flow.state().draft);
        flow.send();
        assertEquals("Próxima pergunta", gateway.messages.get(1));
        assertEquals("session-a", gateway.sessions.get(1));
    }

    @Test
    public void nextSendUsesReturnedSessionAndStableDistinctMessageIds() {
        send("Primeira");
        gateway.replies.get(0).accept(reply("Resposta", "session-a"));
        drain();
        ChatState firstConversation = flow.state();
        send("Segunda");
        gateway.replies.get(1).accept(reply("Outra resposta", "session-a"));
        drain();

        assertEquals("session-a", gateway.sessions.get(1));
        assertEquals(4, flow.state().messages.size());
        assertEquals(firstConversation.messages.get(0).id, flow.state().messages.get(0).id);
        assertEquals(firstConversation.messages.get(1).id, flow.state().messages.get(1).id);
        assertEquals(4, flow.state().messages.stream().map(message -> message.id).distinct().count());
    }

    @Test
    public void failurePreservesDraftAndErrorWithoutAutomaticRetry() {
        send("Água");
        gateway.replies.get(0).accept(ScreenState.error("resource:error", 500, 0));
        drain();

        assertEquals("Água", flow.state().draft);
        assertNull(flow.state().sessionId);
        assertEquals(ScreenState.Status.ERROR, flow.state().sending.status);
        assertEquals("resource:error", flow.state().sending.message);
        assertEquals(500, flow.state().sending.code);
        assertEquals(ScreenState.Status.ERROR, flow.state().messages.get(0).status);
        assertEquals(1, gateway.messages.size());
        assertEquals(1, flow.state().messages.size());
    }

    @Test
    public void explicitRetryReusesFailedMessageIdentityWithoutDuplicateBubble() {
        send("Água");
        long id = flow.state().messages.get(0).id;
        gateway.replies.get(0).accept(ScreenState.error("resource:error"));
        drain();
        flow.send();

        assertEquals(2, gateway.messages.size());
        assertEquals(1, flow.state().messages.size());
        assertEquals(id, flow.state().messages.get(0).id);
        assertEquals(ScreenState.Status.LOADING, flow.state().messages.get(0).status);
        gateway.replies.get(1).accept(reply("Resposta", "session-a"));
        drain();
        assertEquals(2, flow.state().messages.size());
        assertEquals(ScreenState.Status.SUCCESS, flow.state().messages.get(0).status);
    }

    @Test
    public void rateLimitBlocksManualRetryUntilServerWaitElapses() {
        send("Água");
        gateway.replies.get(0).accept(ScreenState.error("resource:rate-limit", 429, 2000));
        drain();
        flow.send();
        assertEquals(1, gateway.messages.size());
        now.set(2000);
        flow.send();
        assertEquals(2, gateway.messages.size());
    }

    @Test
    public void invalidSuccessfulResponseAndWrongSessionBecomeErrors() {
        send("Água");
        gateway.replies.get(0).accept(ScreenState.success(null));
        drain();
        assertEquals(strings.apply(R.string.chat_error_invalid_response), flow.state().sending.message);
        flow.send();
        gateway.replies.get(1).accept(reply("Resposta", "session-a"));
        drain();
        send("Continue");
        gateway.replies.get(2).accept(reply("Outra conversa", "session-b"));
        drain();

        assertEquals(ScreenState.Status.ERROR, flow.state().sending.status);
        assertEquals("session-a", flow.state().sessionId);
        assertEquals(3, flow.state().messages.size());
        assertEquals("Continue", flow.state().draft);
    }

    @Test
    public void immutableMessageListCannotBeChangedByObserver() {
        send("Água");
        try {
            flow.state().messages.clear();
            fail("Expected immutable snapshot");
        } catch (UnsupportedOperationException expected) {
            assertEquals(1, flow.state().messages.size());
        }
    }
}
