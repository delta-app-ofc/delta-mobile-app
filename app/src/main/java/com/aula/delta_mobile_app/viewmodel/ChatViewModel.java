package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.model.ChatState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.ChatRepository;
import com.aula.delta_mobile_app.repository.ChatActions;
import com.aula.delta_mobile_app.services.network.RetrofitClient;

import java.util.Objects;
import java.util.function.Consumer;

public final class ChatViewModel extends AndroidViewModel {
    public interface Account {
        LiveData<Boolean> session();
        boolean hasSession();
        void load(Consumer<ScreenState<Integer>> callback);
        ChatActions chat(int userId);
    }

    private final MutableLiveData<ChatState> state = new MutableLiveData<>(ChatState.empty());
    private final MutableLiveData<ChatFlow.HistoryState> history = new MutableLiveData<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Account account;
    private final boolean sendingEnabled;
    private final Observer<Boolean> sessionObserver;
    private ChatFlow flow;
    private int generation;
    private boolean cleared, loadingAccount, accountReady;
    private boolean pendingSessions;

    public ChatViewModel(Application application) {
        this(application, account(application), true);
    }

    public ChatViewModel(Application application, Account account, boolean sendingEnabled) {
        super(application);
        this.account = Objects.requireNonNull(account);
        this.sendingEnabled = sendingEnabled;
        sessionObserver = signedIn -> {
            if (Boolean.TRUE.equals(signedIn)) loadAccount();
            else clearAccount();
        };
        account.session().observeForever(sessionObserver);
    }

    private static Account account(Application application) {
        AuthRepository auth = AuthRepository.getInstance(application);
        return new Account() {
            @Override
            public LiveData<Boolean> session() {
                return auth.session();
            }

            @Override
            public boolean hasSession() {
                return auth.hasSession();
            }

            @Override
            public void load(Consumer<ScreenState<Integer>> callback) {
                auth.resource(() -> ScreenState.success(auth.currentProfile().id), callback);
            }

            @Override
            public ChatActions chat(int userId) {
                return new ChatRepository(RetrofitClient.getChatbotService(), userId, application::getString);
            }
        };
    }

    public boolean sendingEnabled() {
        return sendingEnabled;
    }

    public static ChatViewModel forActivity(FragmentActivity activity) {
        return new ViewModelProvider(activity).get(ChatViewModel.class);
    }

    public LiveData<ChatState> state() {
        return state;
    }

    public LiveData<ChatFlow.HistoryState> history() {
        return history;
    }

    public void loadSessions() {
        if (cleared) return;
        if (!account.hasSession()) {
            clearAccount();
            return;
        }
        if (accountReady && flow != null) flow.loadSessions();
        else {
            pendingSessions = true;
            prepare();
        }
    }

    public void openSession(String sessionId) {
        if (accountReady && !cleared && flow != null && account.hasSession()) flow.openSession(sessionId);
        else if (!cleared && !account.hasSession()) clearAccount();
    }

    public void prepare() {
        if (cleared || loadingAccount || accountReady) return;
        loadAccount();
    }

    public void showIntroduction() {
        if (!cleared && flow != null) flow.showIntroduction();
    }

    public void resumeConversation() {
        if (accountReady && !cleared && flow != null && account.hasSession()) flow.resumeConversation();
        else if (!cleared && !account.hasSession()) clearAccount();
    }

    public void newChat(boolean discardDraft) {
        if (!cleared && !account.hasSession()) {
            clearAccount();
            return;
        }
        if (accountReady && !cleared && flow != null && (sendingEnabled || flow.state().sessionId == null)) flow.newChat(discardDraft);
    }

    public void setDraft(String value) {
        if (accountReady && !cleared && flow != null && account.hasSession()) flow.setDraft(value);
        else if (!cleared && !account.hasSession()) clearAccount();
    }

    public void send() {
        if (!sendingEnabled) return;
        if (accountReady && !cleared && flow != null && account.hasSession()) flow.send();
        else if (!cleared && !account.hasSession()) clearAccount();
    }

    private void loadAccount() {
        if (cleared) return;
        if (!account.hasSession()) {
            clearAccount();
            return;
        }
        int request = ++generation;
        loadingAccount = true;
        accountReady = false;
        history.setValue(null);
        state.setValue(ChatState.forAccount(ScreenState.loading()));
        account.load(result -> main.post(() -> {
            if (cleared || request != generation) return;
            loadingAccount = false;
            if (!account.hasSession()) {
                clearAccount();
                return;
            }
            if (result.status != ScreenState.Status.SUCCESS || result.data == null || result.data <= 0) {
                state.setValue(ChatState.forAccount(ScreenState.error(result.message, result.code, result.retryAt)));
                return;
            }
            if (flow == null || !result.data.equals(flow.state().account.data)) {
                discardFlow();
                Application app = getApplication();
                flow = new ChatFlow(account.chat(result.data), result.data, action -> main.post(action),
                    this::publishFlow, app::getString, System::currentTimeMillis);
                flow.observeHistory(value -> {
                    if (!cleared && accountReady && flow != null && account.hasSession()) history.setValue(value);
                });
            }
            accountReady = true;
            history.setValue(flow.historyState());
            state.setValue(flow.state());
            if (pendingSessions) {
                pendingSessions = false;
                flow.loadSessions();
            }
        }));
    }

    private void publishFlow(ChatState value) {
        if (!cleared && accountReady && flow != null && flow.state() == value) state.setValue(value);
    }

    private void clearAccount() {
        generation++;
        loadingAccount = false;
        accountReady = false;
        pendingSessions = false;
        discardFlow();
        history.setValue(null);
        if (!cleared) state.setValue(ChatState.empty());
    }

    private void discardFlow() {
        ChatFlow previous = flow;
        flow = null;
        if (previous != null) previous.invalidate();
    }

    @Override
    protected void onCleared() {
        cleared = true;
        account.session().removeObserver(sessionObserver);
        clearAccount();
        main.removeCallbacksAndMessages(null);
    }
}
