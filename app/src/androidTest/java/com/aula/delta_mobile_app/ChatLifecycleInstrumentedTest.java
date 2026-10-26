package com.aula.delta_mobile_app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.ChatActions;
import com.aula.delta_mobile_app.view.ChatMessageAdapter;
import com.aula.delta_mobile_app.view.ChatSessionAdapter;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.view.fragments.ChatFragment;
import com.aula.delta_mobile_app.viewmodel.ChatViewModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatLifecycleInstrumentedTest {
    private static final class Gateway implements ChatActions {
        final List<Consumer<ScreenState<ChatResponse>>> replies = new ArrayList<>();
        final List<String> messages = new ArrayList<>(), sessionIds = new ArrayList<>();
        final List<String> closed = new ArrayList<>();
        final List<Consumer<ScreenState<CloseResponse>>> closings = new ArrayList<>();
        final List<Session> saved = new ArrayList<>();

        @Override
        public void sessions(Consumer<ScreenState<List<Session>>> callback) {
            callback.accept(ScreenState.success(new ArrayList<>(saved)));
        }

        @Override
        public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
            HistoryMessage message = new HistoryMessage();
            message.role = "assistant";
            message.content = "Conversa anterior preservada";
            message.timestamp = "2026-10-25T12:00:00Z";
            callback.accept(ScreenState.success(List.of(message)));
        }

        @Override
        public void send(String message, String sessionId, Consumer<ScreenState<ChatResponse>> callback) {
            messages.add(message);
            sessionIds.add(sessionId);
            replies.add(callback);
        }

        @Override
        public void close(String sessionId, Consumer<ScreenState<CloseResponse>> callback) {
            closed.add(sessionId);
            closings.add(callback);
        }
    }

    private static final class Account implements ChatViewModel.Account {
        final MutableLiveData<Boolean> valid = new MutableLiveData<>(true);
        final Gateway gateway = new Gateway();
        final List<Integer> identities = new ArrayList<>();
        final List<Consumer<ScreenState<Integer>>> loads = new ArrayList<>();
        int userId = 42;
        boolean delayed;

        @Override
        public LiveData<Boolean> session() {
            return valid;
        }

        @Override
        public boolean hasSession() {
            return Boolean.TRUE.equals(valid.getValue());
        }

        @Override
        public void load(Consumer<ScreenState<Integer>> callback) {
            loads.add(callback);
            if (!delayed) callback.accept(ScreenState.success(userId));
        }

        @Override
        public ChatActions chat(int userId) {
            identities.add(userId);
            return gateway;
        }
    }

    private ActivityScenario<LoginActivity> scenario;
    private final Account account = new Account();
    private ChatViewModel model;

    @Before
    public void openWithActivityScopedModelAndControlledAccount() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LoginActivity.class)
            .putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true);
        scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            model = new ViewModelProvider(activity, new ViewModelProvider.Factory() {
                @NonNull
                @Override
                public <T extends ViewModel> T create(@NonNull Class<T> type) {
                    return type.cast(new ChatViewModel(activity.getApplication(), account, true));
                }
            }).get(ChatViewModel.class);
            FragmentNavigator.Destination destination = (FragmentNavigator.Destination) nav(activity).getGraph().findNode(R.id.loginFragment);
            destination.setClassName(ChatFragment.class.getName());
            nav(activity).navigate(R.id.loginFragment, null, new NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build());
        });
        await(() -> model.state().getValue().account.status == ScreenState.Status.SUCCESS && chat() != null && chat().getView() != null);
    }

    @After
    public void closeHost() {
        if (scenario != null) scenario.close();
    }

    private NavController nav(LoginActivity activity) {
        return ((NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host)).getNavController();
    }

    private ChatFragment chat() {
        final ChatFragment[] found = {null};
        scenario.onActivity(activity -> {
            NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
            Fragment current = host.getChildFragmentManager().getPrimaryNavigationFragment();
            if (current instanceof ChatFragment) found[0] = (ChatFragment) current;
        });
        return found[0];
    }

    private void await(BooleanSupplier condition) {
        AtomicBoolean done = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + 5000;
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return;
            SystemClock.sleep(25);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Lifecycle condition did not complete");
    }

    private ChatState state() {
        return model.state().getValue();
    }

    private void send(String text) {
        onView(withId(R.id.chat_input)).perform(replaceText(text), closeSoftKeyboard());
        onView(withId(R.id.chat_send)).perform(click());
        await(() -> state().sending.busy());
    }

    private void answer(int request, String text, String sessionId) {
        scenario.onActivity(activity -> {
            ChatResponse response = new ChatResponse();
            response.response = text;
            response.sessionId = sessionId;
            response.agentUsed = "rag";
            account.gateway.replies.get(request).accept(ScreenState.success(response));
        });
        await(() -> state().sending.status != ScreenState.Status.LOADING);
    }

    private void leave() {
        scenario.onActivity(activity -> {
            ((FragmentNavigator.Destination) nav(activity).getGraph().findNode(R.id.signupAccountFragment)).setClassName(Fragment.class.getName());
            nav(activity).navigate(R.id.signupAccountFragment);
        });
        await(() -> chat() == null);
    }

    private void back() {
        backToIntroduction();
        if (state().showIntroduction && (state().sessionId != null || !state().messages.isEmpty() || !state().draft.isEmpty())) {
            onView(withId(R.id.chat_resume)).perform(click());
        }
    }

    private void backToIntroduction() {
        scenario.onActivity(activity -> assertTrue(nav(activity).popBackStack()));
        await(() -> chat() != null && chat().getView() != null);
    }

    private void scrollToEnd() {
        await(() -> {
            RecyclerView list = chat().requireView().findViewById(R.id.chat_messages);
            ChatMessageAdapter messages = (ChatMessageAdapter) ((ConcatAdapter) list.getAdapter()).getAdapters().stream()
                .filter(adapter -> adapter instanceof ChatMessageAdapter).findFirst().get();
            ChatSessionAdapter sessions = (ChatSessionAdapter) ((ConcatAdapter) list.getAdapter()).getAdapters().stream()
                .filter(adapter -> adapter instanceof ChatSessionAdapter).findFirst().get();
            int count = state().showIntroduction && model.history().getValue() != null && model.history().getValue().sessions.data != null
                ? model.history().getValue().sessions.data.size() : 0;
            return messages.getItemCount() == (state().showIntroduction ? 0 : state().messages.size()) && sessions.getItemCount() == count;
        });
        scenario.onActivity(activity -> {
            RecyclerView list = chat().requireView().findViewById(R.id.chat_messages);
            list.scrollToPosition(list.getAdapter().getItemCount() - 1);
        });
        await(() -> {
            RecyclerView list = chat().requireView().findViewById(R.id.chat_messages);
            return list.getLayoutManager().findViewByPosition(list.getAdapter().getItemCount() - 1) != null;
        });
        scenario.onActivity(activity -> ((RecyclerView) chat().requireView().findViewById(R.id.chat_messages)).scrollBy(0, Integer.MAX_VALUE));
    }

    private void capture(String name) throws IOException {
        AtomicBoolean drawn = new AtomicBoolean();
        scenario.onActivity(activity -> chat().requireView().postOnAnimation(() -> chat().requireView().postOnAnimation(() -> drawn.set(true))));
        await(drawn::get);
        scenario.onActivity(activity -> Log.i("ChatAcceptance", String.format(Locale.ROOT, "%s viewportDp=%.1fx%.1f fontScale=%.2f", name,
            chat().requireView().getWidth() / activity.getResources().getDisplayMetrics().density,
            chat().requireView().getHeight() / activity.getResources().getDisplayMetrics().density, activity.getResources().getConfiguration().fontScale)));
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(directory, "chat-stage8-" + name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            image.recycle();
        }
    }

    @Test
    public void buttonAndKeyboardActionCannotSendTwiceWhileWaiting() {
        send("Pergunta");
        scenario.onActivity(activity -> {
            ((EditText) chat().requireView().findViewById(R.id.chat_input)).onEditorAction(EditorInfo.IME_ACTION_SEND);
            assertEquals(1, account.gateway.replies.size());
        });
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
    }

    @Test
    public void keyboardSendUsesReturnedSessionForNextMessage() {
        onView(withId(R.id.chat_input)).perform(replaceText("Primeira"), closeSoftKeyboard());
        scenario.onActivity(activity -> ((EditText) chat().requireView().findViewById(R.id.chat_input)).onEditorAction(EditorInfo.IME_ACTION_SEND));
        await(() -> account.gateway.replies.size() == 1);
        assertNull(account.gateway.sessionIds.get(0));
        answer(0, "Resposta", "created-session");
        send("Segunda");
        assertEquals("created-session", account.gateway.sessionIds.get(1));
    }

    @Test
    public void replyReceivedInAnotherDestinationIsVisibleOnlyWhenReturning() throws IOException {
        send("Pergunta fora da tela");
        leave();
        answer(0, "Resposta recebida fora da tela", "created-session");
        assertNull(chat());
        back();
        scrollToEnd();
        onView(withText("Resposta recebida fora da tela")).check(matches(isDisplayed()));
        assertEquals(1, account.gateway.replies.size());
        capture("returned-response");
    }

    @Test
    public void returningBeforeReplyKeepsTheSamePendingRequest() {
        send("Pergunta");
        leave();
        back();
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
        assertEquals(1, account.gateway.replies.size());
        answer(0, "Resposta única", "created-session");
        assertEquals(2, state().messages.size());
    }

    @Test
    public void recreationKeepsModelDraftAndPendingReplyWithoutNewRequest() {
        send("Pergunta");
        onView(withId(R.id.chat_input)).perform(replaceText("Próximo rascunho"), closeSoftKeyboard());
        scenario.recreate();
        await(() -> chat() != null && chat().getView() != null);
        scenario.onActivity(activity -> assertSame(model, ChatViewModel.forActivity(activity)));
        onView(withId(R.id.chat_input)).check(matches(withText("Próximo rascunho")));
        assertEquals(1, account.gateway.replies.size());
        answer(0, "Resposta após recriar", "created-session");
        assertEquals("Próximo rascunho", state().draft);
        assertEquals(2, state().messages.size());
    }

    @Test
    public void rotationKeepsPendingRequestAndReceivesReply() {
        send("Pergunta");
        int[] target = {0};
        scenario.onActivity(activity -> {
            boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            target[0] = landscape ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            activity.setRequestedOrientation(landscape ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });
        await(() -> {
            ChatFragment current = chat();
            return current != null && current.getView() != null && current.getResources().getConfiguration().orientation == target[0];
        });
        scenario.onActivity(activity -> assertSame(model, ChatViewModel.forActivity(activity)));
        assertEquals(1, account.gateway.replies.size());
        answer(0, "Resposta após girar", "created-session");
        assertEquals(2, state().messages.size());
        assertTrue(account.gateway.closed.isEmpty());
    }

    @Test
    public void timeoutWhileAwayKeepsDraftAndRequiresManualRetry() throws IOException {
        send("Rascunho preservado");
        leave();
        scenario.onActivity(activity -> account.gateway.replies.get(0).accept(ScreenState.error(activity.getString(R.string.chat_error_timeout))));
        await(() -> state().sending.status == ScreenState.Status.ERROR);
        back();
        scrollToEnd();
        onView(withId(R.id.chat_input)).check(matches(withText("Rascunho preservado")));
        scenario.onActivity(activity -> {
            TextView notice = chat().requireView().findViewById(R.id.chat_notice);
            assertTrue(notice.getText().toString().contains(activity.getString(R.string.chat_retry_uncertain)));
            assertEquals(1, account.gateway.replies.size());
        });
        capture("timeout");
        onView(withId(R.id.chat_retry)).perform(click());
        assertEquals(2, account.gateway.replies.size());
    }

    @Test
    public void logoutWhileAwayDiscardsLateReplyAndPrivateDraft() {
        send("Texto privado");
        leave();
        scenario.onActivity(activity -> account.valid.setValue(false));
        answer(0, "Resposta privada", "old-session");
        back();
        await(() -> state().messages.isEmpty());
        onView(withId(R.id.chat_input)).check(matches(withText(""))).check(matches(not(isEnabled())));
        assertNull(state().sessionId);
    }

    @Test
    public void lateOldAccountReplyCannotEnterNewAccountConversation() {
        send("Conta antiga");
        scenario.onActivity(activity -> {
            account.valid.setValue(false);
            account.userId = 77;
            account.valid.setValue(true);
        });
        await(() -> Integer.valueOf(77).equals(state().account.data));
        send("Conta nova");
        answer(1, "Resposta nova", "new-session");
        answer(0, "Resposta antiga", "old-session");
        assertEquals(List.of(42, 77), account.identities);
        assertEquals("new-session", state().sessionId);
        assertEquals("Resposta nova", state().messages.get(1).content);
    }

    @Test
    public void delayedAccountValidationCannotPublishIntoLoggedOutScreen() {
        scenario.onActivity(activity -> {
            account.valid.setValue(false);
            account.delayed = true;
            account.valid.setValue(true);
            account.valid.setValue(false);
            account.loads.get(account.loads.size() - 1).accept(ScreenState.success(42));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(ScreenState.Status.IDLE, state().account.status);
        assertTrue(account.gateway.replies.isEmpty());
    }

    @Test
    public void disabledSendingGateBlocksBothModelAndComposer() throws IOException {
        scenario.onActivity(activity -> {
            Session saved = new Session();
            saved.sessionId = "blocked-session";
            saved.createdAt = "2026-10-25T12:00:00Z";
            account.gateway.saved.add(saved);
            activity.getViewModelStore().clear();
            model = new ViewModelProvider(activity, new ViewModelProvider.Factory() {
                @NonNull
                @Override
                public <T extends ViewModel> T create(@NonNull Class<T> type) {
                    return type.cast(new ChatViewModel(activity.getApplication(), account, false));
                }
            }).get(ChatViewModel.class);
            nav(activity).navigate(R.id.loginFragment, null, new NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build());
        });
        await(() -> state().account.status == ScreenState.Status.SUCCESS && chat() != null && chat().getView() != null);
        scenario.onActivity(activity -> {
            model.setDraft("Não enviar");
            model.send();
        });
        onView(withId(R.id.chat_input)).check(matches(not(isEnabled())));
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
        scrollToEnd();
        onView(withId(R.id.chat_notice)).check(matches(withText(R.string.chat_sending_pending)));
        assertTrue(account.gateway.replies.isEmpty());
        capture("blocked");
        scenario.onActivity(activity -> {
            model.setDraft("");
            model.openSession("blocked-session");
        });
        await(() -> "blocked-session".equals(state().sessionId) && model.history().getValue().history.status == ScreenState.Status.SUCCESS);
        scenario.onActivity(activity -> model.newChat(true));
        onView(withId(R.id.chat_new)).check(matches(not(isEnabled())));
        assertTrue(account.gateway.closed.isEmpty());
        assertEquals("blocked-session", state().sessionId);
    }

    @Test
    public void closingActivityRemovesSessionObserverAndInvalidatesPendingReply() {
        send("Pergunta");
        scenario.close();
        scenario = null;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertFalse(account.valid.hasObservers());
            ChatResponse response = new ChatResponse();
            response.response = "Resposta atrasada";
            response.sessionId = "closed-host";
            account.gateway.replies.get(0).accept(ScreenState.success(response));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(1, account.gateway.replies.size());
        assertTrue(account.gateway.closed.isEmpty());
    }

    private void conversation() {
        scenario.onActivity(activity -> {
            Session session = new Session();
            session.sessionId = "previous-session";
            session.createdAt = "2026-10-25T12:00:00Z";
            account.gateway.saved.add(session);
        });
        send("Pergunta anterior");
        answer(0, "Resposta anterior", "previous-session");
    }

    private void closeAnswer(int request) {
        scenario.onActivity(activity -> {
            CloseResponse response = new CloseResponse();
            response.status = "ok";
            response.sessionId = account.gateway.closed.get(request);
            account.gateway.closings.get(request).accept(ScreenState.success(response));
        });
        await(() -> state().sessionId == null);
    }

    @Test
    public void newChatWithoutSessionConfirmsDraftAndDoesNotCallClose() throws IOException {
        onView(withId(R.id.chat_input)).perform(replaceText("Rascunho local"), closeSoftKeyboard());
        onView(withId(R.id.chat_new)).perform(click());
        onView(withText(R.string.chat_new_cancel)).perform(click());
        onView(withId(R.id.chat_input)).check(matches(withText("Rascunho local")));
        onView(withId(R.id.chat_new)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        onView(withId(R.id.chat_input)).check(matches(withText("")));
        assertTrue(account.gateway.closed.isEmpty());
        capture("stage9-new-empty");
    }

    @Test
    public void newChatWaitsForCloseAndPreservesPreviousHistory() throws IOException {
        conversation();
        onView(withId(R.id.chat_new)).perform(click());
        scenario.onActivity(activity -> model.newChat(false));
        assertEquals(List.of("previous-session"), account.gateway.closed);
        onView(withId(R.id.chat_new)).check(matches(not(isEnabled())));
        onView(withId(R.id.chat_input)).check(matches(not(isEnabled())));
        closeAnswer(0);
        await(() -> state().showIntroduction);
        assertTrue(state().messages.isEmpty());
        onView(withId(R.id.chat_resume)).check(matches(withEffectiveVisibility(GONE)));
        scrollToEnd();
        capture("stage9-history-preserved");
        onView(withText("ID: previous-session")).perform(click());
        await(() -> state().messages.size() == 1 && !state().showIntroduction);
        assertEquals("Conversa anterior preservada", state().messages.get(0).content);
        onView(withId(R.id.chat_new)).perform(click());
        closeAnswer(1);
        send("Pergunta nova");
        assertNull(account.gateway.sessionIds.get(1));
    }

    @Test
    public void closeFailurePreservesDraftAndManualRetryShowsConfirmationAgain() throws IOException {
        conversation();
        onView(withId(R.id.chat_input)).perform(replaceText("Rascunho guardado"), closeSoftKeyboard());
        onView(withId(R.id.chat_new)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        scenario.onActivity(activity -> account.gateway.closings.get(0).accept(ScreenState.error(activity.getString(R.string.chat_error_network))));
        await(() -> model.history().getValue().closing.status == ScreenState.Status.ERROR);
        assertEquals("previous-session", state().sessionId);
        onView(withId(R.id.chat_input)).check(matches(withText("Rascunho guardado")));
        scrollToEnd();
        capture("stage9-close-failure");
        onView(withId(R.id.chat_retry)).perform(click());
        onView(withText(R.string.chat_new_confirm_title)).check(matches(isDisplayed()));
        onView(withId(android.R.id.button1)).perform(click());
        assertEquals(2, account.gateway.closed.size());
        closeAnswer(1);
        assertEquals("", state().draft);
    }

    @Test
    public void entryIsInitialAndResumeRestoresResponseWithoutClosing() throws IOException {
        send("Pergunta");
        leave();
        answer(0, "Resposta fora da tela", "previous-session");
        backToIntroduction();
        assertTrue(state().showIntroduction);
        onView(withId(R.id.chat_resume)).check(matches(isDisplayed()));
        scenario.onActivity(activity -> {
            RecyclerView list = chat().requireView().findViewById(R.id.chat_messages);
            ChatMessageAdapter rows = (ChatMessageAdapter) ((ConcatAdapter) list.getAdapter()).getAdapters().stream()
                .filter(adapter -> adapter instanceof ChatMessageAdapter).findFirst().get();
            assertEquals(0, rows.getItemCount());
        });
        capture("stage9-initial-resume");
        onView(withId(R.id.chat_resume)).perform(click());
        await(() -> !state().showIntroduction);
        assertEquals("Resposta fora da tela", state().messages.get(1).content);
        assertTrue(account.gateway.closed.isEmpty());
    }

    @Test
    public void pendingSendBlocksNewChatAndRotationDoesNotCloseIt() {
        send("Pergunta");
        onView(withId(R.id.chat_new)).check(matches(not(isEnabled())));
        scenario.onActivity(activity -> model.newChat(true));
        scenario.recreate();
        await(() -> chat() != null && chat().getView() != null);
        assertFalse(state().showIntroduction);
        assertTrue(account.gateway.closed.isEmpty());
        answer(0, "Resposta", "previous-session");
        assertEquals(2, state().messages.size());
    }

    @Test
    public void closeInProgressSurvivesRecreationAndDoesNotRepeat() {
        conversation();
        onView(withId(R.id.chat_new)).perform(click());
        scenario.recreate();
        await(() -> chat() != null && chat().getView() != null);
        assertTrue(model.history().getValue().closing.busy());
        assertEquals(1, account.gateway.closed.size());
        closeAnswer(0);
        assertTrue(state().showIntroduction);
    }

    @Test
    public void closeTimeoutKeepsSessionAndExplainsRepeatedMemoryGeneration() {
        conversation();
        onView(withId(R.id.chat_new)).perform(click());
        scenario.onActivity(activity -> account.gateway.closings.get(0).accept(ScreenState.error(activity.getString(R.string.chat_error_timeout))));
        await(() -> model.history().getValue().closing.status == ScreenState.Status.ERROR);
        scrollToEnd();
        scenario.onActivity(activity -> assertTrue(((TextView) chat().requireView().findViewById(R.id.chat_notice)).getText().toString()
            .contains(activity.getString(R.string.chat_close_timeout))));
        assertEquals("previous-session", state().sessionId);
        assertEquals(1, account.gateway.closed.size());
    }

    @Test
    public void logoutInvalidatesPendingCloseAndDismissesDraftConfirmation() {
        conversation();
        onView(withId(R.id.chat_input)).perform(replaceText("Rascunho"), closeSoftKeyboard());
        onView(withId(R.id.chat_new)).perform(click());
        scenario.onActivity(activity -> account.valid.setValue(false));
        await(() -> state().account.status == ScreenState.Status.IDLE);
        onView(withText(R.string.chat_new_confirm_title)).check(doesNotExist());
        assertTrue(account.gateway.closed.isEmpty());
        assertEquals("", state().draft);
    }

    @Test
    public void logoutDiscardsCloseResultAlreadyInFlight() {
        conversation();
        onView(withId(R.id.chat_new)).perform(click());
        scenario.onActivity(activity -> account.valid.setValue(false));
        await(() -> state().account.status == ScreenState.Status.IDLE);
        closeAnswer(0);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(ScreenState.Status.IDLE, state().account.status);
        assertTrue(state().messages.isEmpty());
        assertEquals(1, account.gateway.closed.size());
    }

    @Test
    public void leavingDuringAccountRevalidationStillReturnsToIntroduction() {
        conversation();
        scenario.onActivity(activity -> {
            account.delayed = true;
            account.valid.setValue(true);
        });
        leave();
        scenario.onActivity(activity -> account.loads.get(account.loads.size() - 1).accept(ScreenState.success(42)));
        await(() -> state().account.status == ScreenState.Status.SUCCESS);
        backToIntroduction();
        assertTrue(state().showIntroduction);
        assertEquals(2, state().messages.size());
        assertTrue(account.gateway.closed.isEmpty());
    }

    @Test
    public void newChatAndResumeExposeLabeledReachableAccessibilityActions() throws IOException {
        conversation();
        leave();
        backToIntroduction();
        onView(withId(R.id.chat_resume)).check(matches(isDisplayed()));
        scenario.onActivity(activity -> {
            int minimum = activity.getResources().getDimensionPixelSize(R.dimen.chat_composer_min_height);
            for (int id : new int[]{R.id.chat_new, R.id.chat_resume}) {
                View action = chat().requireView().findViewById(id);
                AccessibilityNodeInfo node = action.createAccessibilityNodeInfo();
                assertTrue(node.isEnabled());
                assertTrue(node.isClickable());
                assertTrue(node.isFocusable());
                assertEquals(activity.getString(id == R.id.chat_new ? R.string.chat_new : R.string.chat_resume), node.getText());
                assertTrue(node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK));
                assertTrue(action.getHeight() >= minimum && action.getWidth() >= minimum);
                Rect bounds = new Rect();
                assertTrue(action.getGlobalVisibleRect(bounds));
                assertTrue(bounds.height() >= minimum && bounds.width() >= minimum);
            }
        });
        capture("stage10-accessibility-actions");
        assertTrue(account.gateway.closed.isEmpty());
    }
}
