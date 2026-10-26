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
import com.aula.delta_mobile_app.services.network.RetrofitClient;

public final class ChatViewModel extends AndroidViewModel {
    private final MutableLiveData<ChatState> state = new MutableLiveData<>(ChatState.empty());
    private final MutableLiveData<ChatFlow.HistoryState> history = new MutableLiveData<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AuthRepository auth;
    private final Observer<Boolean> sessionObserver;
    private ChatFlow flow;
    private int generation;
    private boolean cleared, loadingAccount, accountReady;
    private boolean pendingSessions;

    public ChatViewModel(Application application) {
        super(application);
        auth = AuthRepository.getInstance(application);
        sessionObserver = signedIn -> {
            if (Boolean.TRUE.equals(signedIn)) loadAccount();
            else clearAccount();
        };
        auth.session().observeForever(sessionObserver);
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
        if (!auth.hasSession()) {
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
        if (accountReady && !cleared && flow != null && auth.hasSession()) flow.openSession(sessionId);
        else if (!cleared && !auth.hasSession()) clearAccount();
    }

    public void prepare() {
        if (cleared || loadingAccount || accountReady) return;
        loadAccount();
    }

    public void setDraft(String value) {
        if (accountReady && !cleared && flow != null) flow.setDraft(value);
    }

    public void send() {
        if (accountReady && !cleared && flow != null && auth.hasSession()) flow.send();
        else if (!cleared && !auth.hasSession()) clearAccount();
    }

    private void loadAccount() {
        if (cleared) return;
        if (!auth.hasSession()) {
            clearAccount();
            return;
        }
        int request = ++generation;
        loadingAccount = true;
        accountReady = false;
        history.setValue(null);
        state.setValue(ChatState.forAccount(ScreenState.loading()));
        auth.<Integer>resource(() -> ScreenState.success(auth.currentProfile().id), result -> main.post(() -> {
            if (cleared || request != generation) return;
            loadingAccount = false;
            if (!auth.hasSession()) {
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
                flow = new ChatFlow(new ChatRepository(RetrofitClient.getChatbotService(), result.data, app::getString), result.data, action -> main.post(action),
                    this::publishFlow, app::getString, System::currentTimeMillis);
                flow.observeHistory(value -> {
                    if (!cleared && accountReady && flow != null && auth.hasSession()) history.setValue(value);
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
        auth.session().removeObserver(sessionObserver);
        clearAccount();
        main.removeCallbacksAndMessages(null);
    }
}
