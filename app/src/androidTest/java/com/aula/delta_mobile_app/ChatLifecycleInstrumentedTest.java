package com.aula.delta_mobile_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatLifecycleInstrumentedTest {
    private static final class Gateway implements ChatActions {
        final List<Consumer<ScreenState<ChatResponse>>> replies = new ArrayList<>();
        final List<String> messages = new ArrayList<>(), sessionIds = new ArrayList<>();

        @Override
        public void sessions(Consumer<ScreenState<List<Session>>> callback) {
            callback.accept(ScreenState.success(List.of()));
        }

        @Override
        public void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback) {
            throw new AssertionError("No history request in lifecycle tests");
        }

        @Override
        public void send(String message, String sessionId, Consumer<ScreenState<ChatResponse>> callback) {
            messages.add(message);
            sessionIds.add(sessionId);
            replies.add(callback);
        }

        @Override
        public void close(String sessionId, Consumer<ScreenState<CloseResponse>> callback) {
            throw new AssertionError("Leaving or rotating must not close the chat");
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

    private void scrollToEnd() {
        await(() -> {
            RecyclerView list = chat().requireView().findViewById(R.id.chat_messages);
            ChatMessageAdapter messages = (ChatMessageAdapter) ((ConcatAdapter) list.getAdapter()).getAdapters().stream()
                .filter(adapter -> adapter instanceof ChatMessageAdapter).findFirst().get();
            return messages.getItemCount() == state().messages.size();
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
    public void disabledSendingGateBlocksBothModelAndComposer() throws IOException {
        scenario.onActivity(activity -> {
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
    }

}
