package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.SignupActions;

import java.util.function.Consumer;

public final class SignupFlow {
    private final SignupActions auth;
    private final Consumer<Runnable> dispatch;
    private final Consumer<ScreenState<Boolean>> onCreation;
    private final Consumer<ScreenState<Completed>> onCompletion;
    private ScreenState<Boolean> creation = ScreenState.idle();
    private ScreenState<Completed> completion = ScreenState.idle();
    private Completion pending;
    private int epoch;

    public SignupFlow(SignupActions auth, Consumer<Runnable> dispatch, Consumer<ScreenState<Boolean>> onCreation, Consumer<ScreenState<Completed>> onCompletion) {
        this.auth = auth;
        this.dispatch = dispatch;
        this.onCreation = onCreation;
        this.onCompletion = onCompletion;
    }

    private void created(ScreenState<Boolean> state) {
        creation = state;
        onCreation.accept(state);
    }

    private void completed(ScreenState<Completed> state) {
        completion = state;
        onCompletion.accept(state);
    }

    public void create(CreateAccount request) {
        if (creation.busy() || creation.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < creation.retryAt) return;
        if (auth.hasSession()) {
            created(ScreenState.error("Já existe uma sessão. Retome o complemento ou saia antes de criar" + " outra conta."));
            return;
        }
        created(ScreenState.loading());
        int attempt = epoch;
        auth.create(request, result -> dispatch.accept(() -> {
            if (attempt != epoch) return;
            created(result);
            if (pending != null) {
                Completion payload = pending;
                pending = null;
                if (result.status == ScreenState.Status.SUCCESS)
                    send(payload);
                else
                    completed(ScreenState.error("A conta não foi criada. Volte à etapa" + " de credenciais ou entre com uma" + " conta existente."));
            }
        }));
    }

    public void finish(Completion payload) {
        if (completion.busy() || completion.status == ScreenState.Status.SUCCESS || System.currentTimeMillis() < completion.retryAt) return;
        if (creation.busy()) {
            pending = payload;
            completed(ScreenState.loading());
            return;
        }
        send(payload);
    }

    private void send(Completion payload) {
        if (!auth.hasSession()) {
            completed(ScreenState.error("Crie a conta ou entre antes de concluir.", 401, 0));
            return;
        }
        if (payload == null) {
            completed(ScreenState.error("Confira os dados antes de concluir."));
            return;
        }
        completed(ScreenState.loading());
        int attempt = epoch;
        auth.complete(payload, result -> dispatch.accept(() -> {
            if (attempt != epoch) return;
            if (result.status == ScreenState.Status.SUCCESS && (result.data == null || !result.data.completed))
                completed(ScreenState.error("O servidor não confirmou a conclusão."));
            else completed(result);
        }));
    }

    public void consumeCompletion() {
        completed(ScreenState.idle());
    }

    public void reset() {
        epoch++;
        pending = null;
        created(ScreenState.idle());
        completed(ScreenState.idle());
    }
}
