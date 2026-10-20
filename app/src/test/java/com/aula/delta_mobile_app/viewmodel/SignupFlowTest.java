package com.aula.delta_mobile_app.viewmodel;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.SignupActions;

import org.junit.Test;

import java.util.function.Consumer;

public class SignupFlowTest {
    private static class Gateway implements SignupActions {
        boolean session;
        int creates, completions;
        Completion payload;
        Consumer<ScreenState<Boolean>> creationReply;
        Consumer<ScreenState<Completed>> completionReply;

        public boolean hasSession() {
            return session;
        }

        public void create(CreateAccount input, Consumer<ScreenState<Boolean>> callback) {
            creates++;
            creationReply = callback;
        }

        public void complete(Completion input, Consumer<ScreenState<Completed>> callback) {
            completions++;
            payload = input;
            completionReply = callback;
        }
    }

    private final Gateway gateway = new Gateway();
    private ScreenState<Boolean> creation = ScreenState.idle();
    private ScreenState<Completed> completion = ScreenState.idle();
    private final SignupFlow flow = new SignupFlow(gateway, Runnable::run, value -> creation = value, value -> completion = value);

    private CreateAccount account() {
        return new CreateAccount("Teste", "2000-02-29", "", "user@example.com", "password");
    }

    private Completed confirmed() {
        Completed result = new Completed();
        result.completed = true;
        return result;
    }

    @Test
    public void completeFlowCreatesAccountThenWaitsForConfirmedCompletion() {
        flow.create(account());
        assertEquals(ScreenState.Status.LOADING, creation.status);
        gateway.session = true;
        gateway.creationReply.accept(ScreenState.success(true));
        Completion payload = new Completion();
        flow.finish(payload);
        assertSame(payload, gateway.payload);
        assertEquals(ScreenState.Status.LOADING, completion.status);
        gateway.completionReply.accept(ScreenState.success(confirmed()));
        assertEquals(ScreenState.Status.SUCCESS, completion.status);
        assertTrue(completion.data.completed);
    }

    @Test
    public void duplicateTapsDoNotDuplicateAccountOrProperty() {
        flow.create(account());
        flow.create(account());
        assertEquals(1, gateway.creates);
        gateway.session = true;
        gateway.creationReply.accept(ScreenState.success(true));
        flow.create(account());
        assertEquals(1, gateway.creates);
        flow.finish(new Completion());
        flow.finish(new Completion());
        assertEquals(1, gateway.completions);
    }

    @Test
    public void existingEmailErrorAllowsRetryWithoutNavigatingForward() {
        flow.create(account());
        gateway.creationReply.accept(ScreenState.error("E-mail já cadastrado", 409, 0));
        assertEquals(409, creation.code);
        assertEquals(0, gateway.completions);
        flow.create(account());
        assertEquals(2, gateway.creates);
    }

    @Test
    public void activeSessionDoesNotCreateAnotherAccount() {
        gateway.session = true;
        flow.create(account());
        assertEquals(ScreenState.Status.ERROR, creation.status);
        assertEquals(0, gateway.creates);
    }

    @Test
    public void completionWithoutSessionIsBlocked() {
        flow.finish(new Completion());
        assertEquals(401, completion.code);
        assertEquals(0, gateway.completions);
    }

    @Test
    public void resumedIncompleteAccountCanFinishWithoutBeingCreatedAgain() {
        gateway.session = true;
        flow.finish(new Completion());
        assertEquals(0, gateway.creates);
        assertEquals(1, gateway.completions);
    }

    @Test
    public void pendingCompletionWaitsForCreationAndPropagatesCreationFailure() {
        flow.create(account());
        flow.finish(new Completion());
        assertEquals(0, gateway.completions);
        gateway.creationReply.accept(ScreenState.error("Sem conexão"));
        assertEquals(ScreenState.Status.ERROR, completion.status);
        assertEquals(0, gateway.completions);
    }

    @Test
    public void pendingCompletionStartsOnlyAfterSessionExists() {
        flow.create(account());
        flow.finish(new Completion());
        gateway.session = true;
        gateway.creationReply.accept(ScreenState.success(true));
        assertEquals(1, gateway.completions);
    }

    @Test
    public void unconfirmedSuccessDoesNotNavigateToHome() {
        gateway.session = true;
        flow.finish(new Completion());
        gateway.completionReply.accept(ScreenState.success(new Completed()));
        assertEquals(ScreenState.Status.ERROR, completion.status);
    }

    @Test
    public void networkFailureDuringCompletionAllowsRetry() {
        gateway.session = true;
        flow.finish(new Completion());
        gateway.completionReply.accept(ScreenState.error("Sem conexão"));
        flow.finish(new Completion());
        assertEquals(2, gateway.completions);
    }

    @Test
    public void rateLimitBlocksAccountAndCompletionRetries() {
        flow.create(account());
        gateway.creationReply.accept(ScreenState.error("Aguarde", 429, System.currentTimeMillis() + 60000));
        flow.create(account());
        assertEquals(1, gateway.creates);
        gateway.session = true;
        flow.finish(new Completion());
        gateway.completionReply.accept(ScreenState.error("Aguarde", 429, System.currentTimeMillis() + 60000));
        flow.finish(new Completion());
        assertEquals(1, gateway.completions);
    }

    @Test
    public void resetIgnoresOldCallbacksAndClearsDraftState() {
        flow.create(account());
        flow.reset();
        gateway.creationReply.accept(ScreenState.success(true));
        assertEquals(ScreenState.Status.IDLE, creation.status);
        gateway.session = true;
        flow.finish(new Completion());
        flow.reset();
        gateway.completionReply.accept(ScreenState.success(confirmed()));
        assertEquals(ScreenState.Status.IDLE, completion.status);
    }
}
