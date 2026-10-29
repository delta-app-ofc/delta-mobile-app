package com.aula.delta_mobile_app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;
import com.aula.delta_mobile_app.model.ChatConversation;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.repository.GluhChatRepository;

public final class GluhChatViewModel extends AndroidViewModel {
    public final ChatConversation conversation = new ChatConversation();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AuthRepository auth;
    private final GluhChatRepository repository;
    private final SavedStateHandle saved;
    private final Observer<Boolean> sessionObserver;
    private Integer userId;
    private long generation;
    private boolean loadingUser, cleared;
    private String error = "";

    public GluhChatViewModel(Application app, SavedStateHandle saved) {
        super(app);
        this.saved = saved;
        auth = AuthRepository.getInstance(app);
        repository = new GluhChatRepository(auth);
        generation = auth.sessionGeneration();
        sessionObserver = valid -> {
            if (!auth.hasSession() || generation != auth.sessionGeneration()) clear();
        };
        auth.session().observeForever(sessionObserver);
    }

    public LiveData<Integer> changes() { return changes; }
    public String draft() { String value = saved.get("chat_draft"); return value == null ? "" : value; }
    public void draft(String value) { saved.set("chat_draft", value); publish(); }
    public String error() { return error; }
    public boolean available() { return userId != null && auth.hasSession() && generation == auth.sessionGeneration(); }
    public boolean loadingUser() { return loadingUser; }
    public boolean canSend() {
        String text = draft().trim();
        return available() && !conversation.pending() && !conversation.failed() && !text.isEmpty() && text.codePointCount(0, text.length()) <= 2000;
    }

    public void connect() {
        if (generation != auth.sessionGeneration()) clear();
        if (available() || loadingUser) return;
        loadingUser = true;
        error = "";
        long expected = generation;
        publish();
        repository.loadUser(value -> main.post(() -> {
            if (cleared || expected != generation) return;
            if (expected != auth.sessionGeneration() || !auth.hasSession()) { clear(); return; }
            loadingUser = false;
            if (value.status == ScreenState.Status.SUCCESS) userId = value.data;
            else error = "Não foi possível conectar o Gluh à sua conta. Tente novamente.";
            publish();
        }));
    }

    public boolean send() {
        if (!canSend()) return false;
        long request = conversation.begin(draft());
        if (request == 0) return false;
        saved.set("chat_draft", "");
        request(request);
        return true;
    }

    public void retry() {
        if (!available()) { connect(); return; }
        long request = conversation.retry();
        if (request != 0) request(request);
    }

    private void request(long request) {
        error = "";
        long expected = generation;
        publish();
        repository.send(userId, conversation.sessionId(), conversation.pendingText(), value -> main.post(() -> {
            if (cleared || expected != generation) return;
            if (expected != auth.sessionGeneration() || !auth.hasSession()) { clear(); return; }
            if (value.status == ScreenState.Status.SUCCESS) conversation.complete(request, value.data.response, value.data.sessionId);
            else if (conversation.fail(request)) error = value.message == null || value.message.isEmpty() ? "Não foi possível enviar. Tente novamente." : value.message;
            publish();
        }));
    }

    private void clear() {
        repository.cancel();
        generation = auth.sessionGeneration();
        userId = null;
        loadingUser = false;
        conversation.clear();
        saved.set("chat_draft", "");
        error = "";
        publish();
    }

    private void publish() { changes.setValue(changes.getValue() + 1); }

    @Override
    protected void onCleared() {
        cleared = true;
        auth.session().removeObserver(sessionObserver);
        repository.close();
        conversation.clear();
        main.removeCallbacksAndMessages(null);
    }
}
