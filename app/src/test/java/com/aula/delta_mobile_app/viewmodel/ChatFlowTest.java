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
        final List<Consumer<ScreenState<List<Session>>>> listings = new ArrayList<>();
        final List<Consumer<ScreenState<List<HistoryMessage>>>> histories = new ArrayList<>();
        final List<String> opened = new ArrayList<>();

        @Override
        public void sessions(Consumer<ScreenState<List<Session>>> callback) {
            listings.add(callback);
        }

        @Override
        public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
            opened.add(sessionId);
            histories.add(callback);
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

    private Session session(String id, String date) {
        Session session = new Session();
        session.sessionId = id;
        session.createdAt = date;
        return session;
    }

    private HistoryMessage history(String role, String content) {
        HistoryMessage message = new HistoryMessage();
        message.role = role;
        message.content = content;
        message.timestamp = "2026-10-25T12:00:00Z";
        return message;
    }

    private void listSessions() {
        flow.loadSessions();
        gateway.listings.get(0).accept(ScreenState.success(List.of(session("a", "2026-10-25T12:00:00Z"), session("b", "2026-10-24T12:00:00Z"))));
        drain();
    }

    @Test
    public void doubleSendAndEditedDraftStillProduceOneRequestUntilReply() {
        send("Primeira pergunta");
        flow.setDraft("Próxima pergunta");
        flow.send();
        assertEquals(List.of("Primeira pergunta"), gateway.messages);
        gateway.replies.get(0).accept(reply("Primeira resposta", "created-session"));
        drain();
        assertEquals("Próxima pergunta", flow.state().draft);
        flow.send();
        assertEquals("created-session", gateway.sessions.get(1));
    }

    @Test
    public void firstSend422DoesNotInventASessionOrRetryAutomatically() {
        send("Pergunta");
        gateway.replies.get(0).accept(ScreenState.error(strings.apply(R.string.chat_error_validation), 422, 0));
        drain();
        assertNull(flow.state().sessionId);
        assertEquals("Pergunta", flow.state().draft);
        assertEquals(422, flow.state().sending.code);
        assertEquals(1, gateway.replies.size());
        assertEquals(ScreenState.Status.ERROR, flow.state().messages.get(0).status);
    }

    @Test
    public void firstSendTimeoutCannotConfirmWhetherServerCreatedASession() {
        send("Pergunta");
        gateway.replies.get(0).accept(ScreenState.error(strings.apply(R.string.chat_error_timeout)));
        drain();
        assertNull(flow.state().sessionId);
        assertEquals(strings.apply(R.string.chat_error_timeout), flow.state().sending.message);
        assertEquals(1, gateway.replies.size());
        flow.send();
        assertNull(gateway.sessions.get(1));
        assertEquals(1, flow.state().messages.size());
    }

    @Test
    public void establishedSessionSurvives500AndManualRetryUsesItsId() {
        send("Primeira");
        gateway.replies.get(0).accept(reply("Resposta", "existing"));
        drain();
        send("Segunda");
        gateway.replies.get(1).accept(ScreenState.error(strings.apply(R.string.chat_error_unavailable), 500, 0));
        drain();
        assertEquals("existing", flow.state().sessionId);
        assertEquals("Segunda", flow.state().draft);
        assertEquals(3, flow.state().messages.size());
        flow.send();
        assertEquals("existing", gateway.sessions.get(2));
        assertEquals(3, flow.state().messages.size());
    }

    @Test
    public void firstReplyRefreshesOnlyPreviouslyRequestedSessionListing() {
        listSessions();
        send("Água");
        gateway.replies.get(0).accept(reply("Resposta", "created"));
        drain();
        assertEquals(2, gateway.listings.size());
        assertEquals(1, gateway.replies.size());
        assertTrue(flow.historyState().sessions.busy());
        gateway.listings.get(1).accept(ScreenState.success(List.of(session("created", "2026-10-25T12:00:00Z"))));
        drain();
        assertEquals("created", flow.historyState().sessions.data.get(0).id);
    }

    @Test
    public void listingStartedBeforeSessionCreationCannotHideTheCreatedSession() {
        flow.loadSessions();
        send("Água");
        gateway.replies.get(0).accept(reply("Resposta", "created"));
        drain();
        gateway.listings.get(1).accept(ScreenState.success(List.of(session("created", "2026-10-25T12:00:00Z"))));
        drain();
        gateway.listings.get(0).accept(ScreenState.success(List.of()));
        drain();
        assertEquals("created", flow.historyState().sessions.data.get(0).id);
    }

    @Test
    public void listingIsDeduplicatedAndDoesNotFetchPreviews() {
        flow.loadSessions();
        flow.loadSessions();
        assertTrue(flow.historyState().sessions.busy());
        assertEquals(1, gateway.listings.size());
        gateway.listings.get(0).accept(ScreenState.success(List.of()));
        drain();
        flow.loadSessions();
        assertEquals(ScreenState.Status.SUCCESS, flow.historyState().sessions.status);
        assertTrue(flow.historyState().sessions.data.isEmpty());
        assertEquals(1, gateway.listings.size());
        assertTrue(gateway.histories.isEmpty());
    }

    @Test
    public void sessionsSortByInstantWithInvalidDatesLastAndIdsAsTieBreakers() {
        flow.loadSessions();
        Session mutable = session("z", "2026-10-25T09:00:00-03:00");
        gateway.listings.get(0).accept(ScreenState.success(List.of(session("invalid", "nonsense"), mutable,
            session("old", "2026-10-24T12:00:00Z"), session("a", "2026-10-25T12:00:00Z"))));
        drain();
        mutable.sessionId = "changed";
        assertEquals(List.of("a", "z", "old", "invalid"), flow.historyState().sessions.data.stream().map(entry -> entry.id).collect(java.util.stream.Collectors.toList()));
        assertNull(flow.historyState().sessions.data.get(3).created);
        assertThrows(UnsupportedOperationException.class, () -> flow.historyState().sessions.data.clear());
    }

    @Test
    public void naiveDateIsNotSilentlyAssignedATimeZone() {
        flow.loadSessions();
        gateway.listings.get(0).accept(ScreenState.success(List.of(session("a", "2026-10-25T12:00:00"))));
        drain();
        assertNull(flow.historyState().sessions.data.get(0).created);
    }

    @Test
    public void duplicateSessionIdentityIsAnInvalidResponse() {
        flow.loadSessions();
        gateway.listings.get(0).accept(ScreenState.success(List.of(session("a", "bad"), session("a", "bad"))));
        drain();
        assertEquals(ScreenState.Status.ERROR, flow.historyState().sessions.status);
        assertEquals(strings.apply(R.string.chat_error_invalid_response), flow.historyState().sessions.message);
    }

    @Test
    public void listingFailureRetriesOnlyAfterExplicitActionAndRetryAt() {
        flow.loadSessions();
        gateway.listings.get(0).accept(ScreenState.error("Aguarde", 429, 2000));
        drain();
        assertEquals(ScreenState.Status.ERROR, flow.historyState().sessions.status);
        flow.loadSessions();
        assertEquals(1, gateway.listings.size());
        now.set(2000);
        flow.loadSessions();
        assertEquals(2, gateway.listings.size());
        gateway.listings.get(1).accept(ScreenState.success(List.of()));
        drain();
        assertEquals(ScreenState.Status.SUCCESS, flow.historyState().sessions.status);
    }

    @Test
    public void reopeningMapsRolesInServerOrderWithoutInventingAgentOrCharts() {
        listSessions();
        flow.openSession("a");
        flow.openSession("a");
        assertTrue(flow.historyState().history.busy());
        assertTrue(flow.state().messages.isEmpty());
        gateway.histories.get(0).accept(ScreenState.success(List.of(history("human", "Pergunta\nÁgua 💧"), history("assistant", "Fonte: https://example.org"))));
        drain();
        flow.openSession("a");
        assertEquals(List.of("a"), gateway.opened);
        assertEquals("a", flow.state().sessionId);
        assertEquals(ChatMessage.Author.USER, flow.state().messages.get(0).author);
        assertEquals(ChatMessage.Author.ASSISTANT, flow.state().messages.get(1).author);
        assertEquals("Pergunta\nÁgua 💧", flow.state().messages.get(0).content);
        assertNull(flow.state().messages.get(1).agentUsed);
        assertEquals(ScreenState.Status.SUCCESS, flow.state().messages.get(0).status);
    }

    @Test
    public void openingAnotherSessionDiscardsLateHistoryAndDoesNotKeepPreviousRows() {
        listSessions();
        flow.openSession("a");
        flow.openSession("b");
        gateway.histories.get(1).accept(ScreenState.success(List.of(history("assistant", "Resposta B"))));
        drain();
        gateway.histories.get(0).accept(ScreenState.success(List.of(history("assistant", "Resposta A atrasada"))));
        drain();
        assertEquals("b", flow.state().sessionId);
        assertEquals("Resposta B", flow.state().messages.get(0).content);
        flow.openSession("a");
        assertTrue(flow.state().messages.isEmpty());
    }

    @Test
    public void emptyHistoryIsSuccessfulWithoutInventingItsCause() {
        listSessions();
        flow.openSession("a");
        gateway.histories.get(0).accept(ScreenState.success(List.of()));
        drain();
        assertEquals(ScreenState.Status.SUCCESS, flow.historyState().history.status);
        assertTrue(flow.state().messages.isEmpty());
        assertEquals("a", flow.state().sessionId);
    }

    @Test
    public void historyFailureKeepsSelectionAndHasExplicitRetry() {
        listSessions();
        flow.openSession("a");
        gateway.histories.get(0).accept(ScreenState.error("Indisponível", 503, 0));
        drain();
        assertEquals("a", flow.historyState().selectedId);
        assertEquals("Indisponível", flow.historyState().history.message);
        assertTrue(flow.state().messages.isEmpty());
        assertEquals(1, gateway.histories.size());
        flow.openSession("a");
        assertEquals(2, gateway.histories.size());
    }

    @Test
    public void unknownHistoryRoleIsNotMisattributedToAssistant() {
        listSessions();
        flow.openSession("a");
        gateway.histories.get(0).accept(ScreenState.success(List.of(history("system", "Conteúdo desconhecido"))));
        drain();
        assertEquals(ScreenState.Status.ERROR, flow.historyState().history.status);
        assertTrue(flow.state().messages.isEmpty());
    }

    @Test
    public void unknownSessionCannotBeQueried() {
        listSessions();
        flow.openSession("not-from-account-list");
        assertTrue(gateway.histories.isEmpty());
        assertNull(flow.historyState().selectedId);
    }

    @Test
    public void draftBlocksSelectionWithoutDiscardingText() {
        listSessions();
        flow.setDraft("Rascunho");
        flow.openSession("a");
        assertEquals("Rascunho", flow.state().draft);
        assertEquals(strings.apply(R.string.chat_history_draft), flow.historyState().notice);
        assertTrue(gateway.histories.isEmpty());
    }

    @Test
    public void sendingBlocksSelectionWithoutCancelingTheOriginalRequest() {
        listSessions();
        send("Água");
        flow.openSession("a");
        assertEquals(strings.apply(R.string.chat_history_busy), flow.historyState().notice);
        assertTrue(gateway.histories.isEmpty());
        gateway.replies.get(0).accept(reply("Resposta", "original"));
        drain();
        assertEquals("original", flow.state().sessionId);
    }

    @Test
    public void historyLoadingAndFailureCannotAcceptSend() {
        listSessions();
        flow.openSession("a");
        flow.setDraft("Água");
        flow.send();
        assertTrue(gateway.replies.isEmpty());
        gateway.histories.get(0).accept(ScreenState.error("Falha"));
        drain();
        flow.send();
        assertTrue(gateway.replies.isEmpty());
    }

    @Test
    public void logoutClearsListsAndDiscardsQueuedHistory() {
        listSessions();
        flow.openSession("a");
        gateway.histories.get(0).accept(ScreenState.success(List.of(history("assistant", "Privado"))));
        flow.invalidate();
        drain();
        assertEquals(ScreenState.Status.IDLE, flow.historyState().sessions.status);
        assertEquals(ScreenState.Status.IDLE, flow.historyState().history.status);
        assertNull(flow.historyState().selectedId);
        assertTrue(flow.state().messages.isEmpty());
    }

    @Test
    public void logoutDiscardsLateSessionListing() {
        flow.loadSessions();
        flow.invalidate();
        gateway.listings.get(0).accept(ScreenState.success(List.of(session("a", "bad"))));
        drain();
        assertEquals(ScreenState.Status.IDLE, flow.historyState().sessions.status);
        assertNull(flow.historyState().sessions.data);
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
    public void oldDuplicateCallbackCannotFinishANewerAttempt() {
        send("Água");
        gateway.replies.get(0).accept(ScreenState.error("resource:error"));
        drain();
        flow.send();
        gateway.replies.get(0).accept(reply("Resposta atrasada", "old-session"));
        drain();

        assertTrue(flow.state().sending.busy());
        assertNull(flow.state().sessionId);
        gateway.replies.get(1).accept(reply("Resposta atual", "new-session"));
        drain();
        assertEquals("new-session", flow.state().sessionId);
        assertEquals("Resposta atual", flow.state().messages.get(1).content);
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

    @Test
    public void replyStillUpdatesFlowAfterConceptualViewObserverIsDetached() {
        List<ChatState> visibleStates = new ArrayList<>();
        boolean[] attached = {true};
        ChatFlow activityFlow = new ChatFlow(gateway, 42, dispatched::add, value -> {
            if (attached[0]) visibleStates.add(value);
        }, strings, now::get);
        activityFlow.setDraft("Água");
        activityFlow.send();
        int visibleCount = visibleStates.size();
        attached[0] = false;
        gateway.replies.get(0).accept(reply("Resposta fora da tela", "session-a"));
        drain();

        assertEquals(visibleCount, visibleStates.size());
        assertEquals(ScreenState.Status.SUCCESS, activityFlow.state().sending.status);
        assertEquals("Resposta fora da tela", activityFlow.state().messages.get(1).content);
        assertEquals(1, gateway.messages.size());
    }

    @Test
    public void logoutErasesAccountDraftAndMessagesAndDiscardsLateReply() {
        send("Texto privado");
        flow.invalidate();
        int publications = states.size();
        gateway.replies.get(0).accept(reply("Resposta privada", "session-a"));
        drain();
        flow.setDraft("Outra mensagem");
        flow.send();

        assertEquals(publications, states.size());
        assertEquals(ScreenState.Status.IDLE, flow.state().account.status);
        assertEquals("", flow.state().draft);
        assertTrue(flow.state().messages.isEmpty());
        assertNull(flow.state().sessionId);
        assertEquals(1, gateway.messages.size());
    }

    @Test
    public void identityInvalidationDiscardsReplyAlreadyQueuedForMainThread() {
        send("Conta anterior");
        gateway.replies.get(0).accept(reply("Resposta anterior", "old-session"));
        assertEquals(1, dispatched.size());
        flow.invalidate();
        drain();

        assertTrue(flow.state().messages.isEmpty());
        assertNull(flow.state().sessionId);
        assertEquals(ScreenState.Status.IDLE, flow.state().account.status);
    }

    @Test
    public void oldAccountReplyCannotReplaceNewAccountConversation() {
        send("Conta anterior");
        flow.invalidate();
        ChatFlow nextAccount = new ChatFlow(gateway, 77, dispatched::add, states::add, strings, now::get);
        nextAccount.setDraft("Conta atual");
        nextAccount.send();
        gateway.replies.get(1).accept(reply("Resposta atual", "new-session"));
        drain();
        int publications = states.size();
        gateway.replies.get(0).accept(reply("Resposta antiga", "old-session"));
        drain();

        assertEquals(publications, states.size());
        assertEquals(Integer.valueOf(77), nextAccount.state().account.data);
        assertEquals("new-session", nextAccount.state().sessionId);
        assertEquals("Resposta atual", nextAccount.state().messages.get(1).content);
    }
}
