package com.aula.delta_mobile_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.ChatMessage;
import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.ChatActions;
import com.aula.delta_mobile_app.view.ChatMessageAdapter;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.view.fragments.ChatFragment;
import com.aula.delta_mobile_app.viewmodel.ChatFlow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatRenderingInstrumentedTest {
    private static class Gateway implements ChatActions {
        final List<Consumer<ScreenState<ChatResponse>>> replies = new ArrayList<>();

        @Override
        public void sessions(Consumer<ScreenState<List<Session>>> callback) {
            throw new AssertionError("No session requests during rendering tests");
        }

        @Override
        public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
            throw new AssertionError("No history requests during rendering tests");
        }

        @Override
        public void send(String message, String sessionId, Consumer<ScreenState<ChatResponse>> callback) {
            replies.add(callback);
        }

        @Override
        public void close(String sessionId, Consumer<ScreenState<CloseResponse>> callback) {
            throw new AssertionError("No close requests during rendering tests");
        }
    }

    private ActivityScenario<LoginActivity> scenario;
    private final Gateway gateway = new Gateway();
    private final MutableLiveData<ChatState> state = new MutableLiveData<>();
    private ChatFlow flow;
    private ChatFragment fragment;

    @Before
    public void openControlledConversation() {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LoginActivity.class)
            .putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true);
        scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> {
            flow = new ChatFlow(gateway, 42, activity::runOnUiThread, state::setValue, activity::getString, System::currentTimeMillis);
            state.setValue(flow.state());
            fragment = new ChatFragment();
            fragment.bindConversation(state, flow::setDraft, flow::send);
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.auth_nav_host, fragment).commitNow();
        });
        await(() -> list().getAdapter() != null && list().getWidth() > 0);
    }

    @After
    public void closeHost() {
        if (scenario != null) scenario.close();
    }

    private RecyclerView list() {
        return fragment.requireView().findViewById(R.id.chat_messages);
    }

    private ChatMessageAdapter messages() {
        return (ChatMessageAdapter) ((ConcatAdapter) list().getAdapter()).getAdapters().get(1);
    }

    private void await(BooleanSupplier condition) {
        AtomicBoolean done = new AtomicBoolean();
        long until = SystemClock.uptimeMillis() + 5000;
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return;
            SystemClock.sleep(25);
        } while (SystemClock.uptimeMillis() < until);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View button = fragment.requireView().findViewById(R.id.chat_send);
            Rect visible = new Rect();
            button.getGlobalVisibleRect(visible);
            fail("Rendering condition did not complete; send=" + button.getWidth() + "x" + button.getHeight() + "; visible=" + visible
                + "; root=" + fragment.requireView().getWidth() + "x" + fragment.requireView().getHeight() + "; insets=" + ViewCompat.getRootWindowInsets(fragment.requireView()));
        });
    }

    private void scrollToEnd() {
        scenario.onActivity(activity -> list().scrollToPosition(list().getAdapter().getItemCount() - 1));
        await(() -> list().getLayoutManager().findViewByPosition(list().getAdapter().getItemCount() - 1) != null);
        scenario.onActivity(activity -> list().scrollBy(0, Integer.MAX_VALUE));
    }

    private void send(String text) {
        onView(withId(R.id.chat_input)).perform(replaceText(text), closeSoftKeyboard());
        await(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(fragment.requireView());
            View button = fragment.requireView().findViewById(R.id.chat_send);
            Rect visible = new Rect();
            return insets != null && !insets.isVisible(WindowInsetsCompat.Type.ime()) && !fragment.requireView().isLayoutRequested()
                && button.getGlobalVisibleRect(visible) && visible.height() == button.getHeight() && visible.width() == button.getWidth();
        });
        onView(withId(R.id.chat_send)).perform(click());
        await(() -> messages().getItemCount() == 1);
        scrollToEnd();
    }

    private void answer(String text) {
        scenario.onActivity(activity -> {
            ChatResponse response = new ChatResponse();
            response.response = text;
            response.agentUsed = "rag";
            response.sessionId = "test-session";
            gateway.replies.get(gateway.replies.size() - 1).accept(ScreenState.success(response));
        });
        await(() -> messages().getItemCount() == 2);
        scrollToEnd();
    }

    private void capture(String name) throws IOException {
        await(() -> !list().isAnimating() && !list().isComputingLayout() && !list().isLayoutRequested());
        AtomicBoolean drawn = new AtomicBoolean();
        scenario.onActivity(activity -> list().postOnAnimation(() -> list().postOnAnimation(() -> drawn.set(true))));
        await(drawn::get);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try (FileOutputStream output = new FileOutputStream(new File(directory, "chat-rendering-" + name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            image.recycle();
        }
    }

    @Test
    public void productionEntryHasNoDemoConversationAndNoSendConnection() throws IOException {
        scenario.onActivity(activity -> {
            fragment = new ChatFragment();
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.auth_nav_host, fragment).commitNow();
        });
        await(() -> list().getWidth() > 0);
        onView(withId(R.id.chat_input)).check(matches(not(isEnabled())));
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
        scenario.onActivity(activity -> {
            assertEquals(0, messages().getItemCount());
            assertTrue(gateway.replies.isEmpty());
        });
        capture("entry");
    }

    @Test
    public void preservesLongSelectableTextMarkdownSourcesAndUrlsWithoutOpeningLinks() throws IOException {
        send("Como economizar água? 💧");
        String reply = "**Dicas**\n- Torneiras\n- Chuveiro\n\nFonte RAG: documento de teste\nhttps://example.org/" + "a".repeat(500) + "\n" + "Água 💧\n".repeat(300);
        answer(reply);
        scenario.onActivity(activity -> {
            View row = list().getLayoutManager().findViewByPosition(2);
            TextView text = row.findViewById(R.id.chat_message_text);
            assertEquals(reply, text.getText().toString());
            assertTrue(text.isTextSelectable());
            assertFalse(text.getLinksClickable());
            assertEquals(0, text.getUrls().length);
            assertEquals(0, text.getLayout().getEllipsisCount(text.getLayout().getLineCount() - 1));
            assertEquals(activity.getString(R.string.chat_message_accessibility, activity.getString(R.string.chat_assistant_author), reply), text.getContentDescription());
        });
        capture("response");
    }

    @Test
    public void waitingAppearsInsideConversationAndBlocksAnotherSend() throws IOException {
        send("Água");
        scenario.onActivity(activity -> list().scrollToPosition(1));
        await(() -> list().getLayoutManager().findViewByPosition(1) != null);
        onView(allOf(withId(R.id.chat_message_text), withText("Água"))).check(matches(isDisplayed()));
        scenario.onActivity(activity -> {
            TextView status = list().getLayoutManager().findViewByPosition(1).findViewById(R.id.chat_message_status);
            assertEquals(status.length(), status.getLayout().getLineEnd(status.getLayout().getLineCount() - 1));
            for (int line = 0; line < status.getLayout().getLineCount(); line++) assertTrue(status.getLayout().getLineWidth(line) <= status.getWidth());
        });
        scrollToEnd();
        onView(withId(R.id.chat_waiting)).check(matches(isDisplayed()));
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
        scenario.onActivity(activity -> {
            assertEquals(1, gateway.replies.size());
            assertEquals(ScreenState.Status.LOADING, messages().getCurrentList().get(0).status);
            assertTrue(fragment.requireView().findViewById(R.id.chat_waiting).isAttachedToWindow());
        });
        capture("waiting");
    }

    @Test
    public void failureKeepsDraftAndRetryOnlySendsAfterExplicitClick() throws IOException {
        send("Meu rascunho");
        scenario.onActivity(activity -> gateway.replies.get(0).accept(ScreenState.error(activity.getString(R.string.chat_error_network))));
        await(() -> fragment.requireView().findViewById(R.id.chat_retry) != null);
        scrollToEnd();
        onView(withId(R.id.chat_notice)).check(matches(withText(InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.chat_error_network))));
        onView(withId(R.id.chat_input)).check(matches(withText("Meu rascunho")));
        scenario.onActivity(activity -> assertEquals(1, gateway.replies.size()));
        capture("error");
        onView(withId(R.id.chat_retry)).perform(click());
        scenario.onActivity(activity -> assertEquals(2, gateway.replies.size()));
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
    }

    @Test
    public void timeoutExplainsUncertainCompletionAndPossibleRepeatedOperation() {
        send("Água");
        scenario.onActivity(activity -> gateway.replies.get(0).accept(ScreenState.error(activity.getString(R.string.chat_error_timeout))));
        await(() -> fragment.requireView().findViewById(R.id.chat_notice) != null);
        scrollToEnd();
        scenario.onActivity(activity -> {
            TextView notice = fragment.requireView().findViewById(R.id.chat_notice);
            assertEquals(activity.getString(R.string.chat_error_timeout) + "\n" + activity.getString(R.string.chat_retry_uncertain), notice.getText().toString());
            assertEquals(1, gateway.replies.size());
        });
    }

    @Test
    public void unicodeCounterDoesNotTruncateDraftAndDisablesOverlongSend() {
        String content = "💧".repeat(2001);
        onView(withId(R.id.chat_input)).perform(replaceText(content), closeSoftKeyboard());
        onView(withId(R.id.chat_send)).check(matches(not(isEnabled())));
        scenario.onActivity(activity -> {
            EditText input = fragment.requireView().findViewById(R.id.chat_input);
            assertEquals(content, input.getText().toString());
            assertEquals(activity.getString(R.string.chat_error_message_length), input.getError());
            assertEquals(activity.getString(R.string.chat_character_count, 2001), ((TextView) fragment.requireView().findViewById(R.id.chat_character_count)).getText().toString());
            assertTrue(gateway.replies.isEmpty());
        });
    }

    @Test
    public void longConversationRecyclesRowsAndDoesNotLeakFailedStatus() {
        scenario.onActivity(activity -> {
            List<ChatMessage> content = new ArrayList<>();
            for (int index = 0; index < 80; index++) content.add(new ChatMessage(index + 1, ChatMessage.Author.USER, "Mensagem " + index,
                index == 0 ? ScreenState.Status.ERROR : ScreenState.Status.SUCCESS, null));
            state.setValue(new ChatState(ScreenState.success(42), false, "", "test-session", content, ScreenState.idle()));
        });
        await(() -> messages().getItemCount() == 80);
        scrollToEnd();
        scenario.onActivity(activity -> {
            View row = list().getLayoutManager().findViewByPosition(80);
            assertEquals("Mensagem 79", ((TextView) row.findViewById(R.id.chat_message_text)).getText().toString());
            assertEquals(View.GONE, row.findViewById(R.id.chat_message_status).getVisibility());
            assertTrue(list().getChildCount() < 80);
            list().scrollToPosition(1);
        });
        await(() -> list().getLayoutManager().findViewByPosition(1) != null);
        scenario.onActivity(activity -> assertEquals(View.VISIBLE, list().getLayoutManager().findViewByPosition(1).findViewById(R.id.chat_message_status).getVisibility()));
    }

    @Test
    public void keyboardRemovesOnlyTheChatMenuClearanceAndKeepsTouchTarget() throws IOException {
        scenario.onActivity(activity -> {
            EditText input = fragment.requireView().findViewById(R.id.chat_input);
            input.requestFocus();
            InputMethodManager keyboard = activity.getSystemService(InputMethodManager.class);
            keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
        await(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(fragment.requireView());
            return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime())
                && fragment.requireView().findViewById(R.id.chat_send).getHeight() >= fragment.getResources().getDimensionPixelSize(R.dimen.chat_composer_min_height);
        });
        capture("keyboard");
        scenario.onActivity(activity -> {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) fragment.requireView().findViewById(R.id.chat_content).getLayoutParams();
            assertEquals(0, params.bottomMargin);
            View button = fragment.requireView().findViewById(R.id.chat_send);
            assertTrue("Send height=" + button.getHeight() + "; root height=" + fragment.requireView().getHeight(),
                button.getHeight() >= activity.getResources().getDimensionPixelSize(R.dimen.chat_composer_min_height));
        });
        onView(withId(R.id.chat_input)).perform(closeSoftKeyboard());
        await(() -> ((ConstraintLayout.LayoutParams) fragment.requireView().findViewById(R.id.chat_content).getLayoutParams()).bottomMargin > 0);
        onView(withId(R.id.chat_character_count)).check(matches(isDisplayed()));
        scenario.onActivity(activity -> assertEquals(4, ((EditText) fragment.requireView().findViewById(R.id.chat_input)).getMaxLines()));
    }

    @Test
    public void clearingAccountRemovesPrivateRowsAndDraftFromView() {
        send("Texto privado");
        answer("Resposta privada");
        scenario.onActivity(activity -> flow.invalidate());
        await(() -> messages().getItemCount() == 0);
        onView(withId(R.id.chat_input)).check(matches(withText(""))).check(matches(not(isEnabled())));
        scenario.onActivity(activity -> assertEquals(1, list().getAdapter().getItemCount()));
    }
}
