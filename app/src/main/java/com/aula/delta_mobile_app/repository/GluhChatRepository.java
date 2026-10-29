package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ChatModels;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.network.RetrofitClient;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Response;

public final class GluhChatRepository {
    private final AuthRepository auth;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile Call<ChatModels.Reply> active;
    private volatile boolean closed;

    public GluhChatRepository(AuthRepository auth) { this.auth = auth; }

    public void loadUser(Consumer<ScreenState<Integer>> callback) {
        auth.resource(() -> ScreenState.success(auth.currentProfile().id), callback);
    }

    public void send(int userId, String sessionId, String message, Consumer<ScreenState<ChatModels.Reply>> callback) {
        Call<ChatModels.Reply> call = RetrofitClient.getChatService().chat(new ChatModels.Request(userId, sessionId, message));
        active = call;
        long expected = auth.sessionGeneration();
        auth.resource(() -> auth.currentProfile().id == userId ? ScreenState.success(true)
            : ScreenState.<Boolean>error("Sessão alterada. Entre novamente.", 401, 0), identity -> {
            if (closed || call.isCanceled()) return;
            if (identity.status != ScreenState.Status.SUCCESS) {
                if (active == call) active = null;
                callback.accept(ScreenState.error(identity.message, identity.code, 0));
                return;
            }
            synchronized (this) {
                if (closed) return;
                worker.execute(() -> {
                    ScreenState<ChatModels.Reply> result;
                    try {
                        if (call.isCanceled()) result = ScreenState.error("Envio cancelado.");
                        else if (!auth.hasSession() || expected != auth.sessionGeneration())
                            result = ScreenState.error("Sessão alterada. Entre novamente.", 401, 0);
                        else result = execute(call);
                        if (expected != auth.sessionGeneration() || !auth.hasSession()) result = ScreenState.error("Sessão alterada. Entre novamente.", 401, 0);
                    } catch (java.io.InterruptedIOException timeout) {
                        result = ScreenState.error("O Gluh demorou para responder. Tente novamente.");
                    } catch (Exception failure) {
                        result = ScreenState.error("Não foi possível conectar ao Gluh. Verifique sua conexão e tente novamente.");
                    } finally {
                        if (active == call) active = null;
                    }
                    callback.accept(result);
                });
            }
        });
    }

    private ScreenState<ChatModels.Reply> execute(Call<ChatModels.Reply> call) throws java.io.IOException {
        Response<ChatModels.Reply> response = call.execute();
        if (!response.isSuccessful()) {
            android.util.Log.w("GluhChat", "POST /chat returned HTTP " + response.code());
            return com.aula.delta_mobile_app.services.network.ChatErrors.from(response);
        }
        ChatModels.Reply reply = response.body();
        if (reply == null || reply.response == null || reply.response.trim().isEmpty() || reply.sessionId == null || reply.sessionId.trim().isEmpty())
            return ScreenState.error("O Gluh retornou uma resposta incompleta. Tente novamente.");
        return ScreenState.success(reply);
    }

    public void cancel() {
        Call<ChatModels.Reply> call = active;
        active = null;
        if (call != null) call.cancel();
    }

    public synchronized void close() {
        closed = true;
        cancel();
        worker.shutdownNow();
    }
}
