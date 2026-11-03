package com.aula.delta_mobile_app.viewmodel;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.AuthModels.Credentials;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.AuthActions;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoginFlowTest {
    private static class Gateway implements AuthActions {
        boolean session;
        boolean remembered = true, deferRefresh;
        int refreshes;
        ScreenState<Boolean> refreshResult = ScreenState.success(true);
        Consumer<ScreenState<Boolean>> refreshReply;
        int logins, routes, logouts;
        Credentials credentials;
        Consumer<ScreenState<Boolean>> loginReply, routeReply;

        @Override
        public boolean hasSession() {
            return session;
        }

        @Override
        public boolean rememberMe() {
            return remembered;
        }

        @Override
        public void refreshSession(Consumer<ScreenState<Boolean>> callback) {
            refreshes++;
            refreshReply = callback;
            if (!deferRefresh) callback.accept(refreshResult);
        }

        @Override
        public void login(Credentials input, Consumer<ScreenState<Boolean>> callback) {
            logins++;
            credentials = input;
            loginReply = callback;
        }

        @Override
        public void route(Consumer<ScreenState<Boolean>> callback) {
            routes++;
            routeReply = callback;
        }

        @Override
        public void logout() {
            logouts++;
            session = false;
        }
    }

    private final Gateway gateway = new Gateway();
    private final List<ScreenState<Boolean>> states = new ArrayList<>();
    private int passwordCleared;
    private final LoginFlow flow = new LoginFlow(gateway, Runnable::run, states::add, () -> passwordCleared++, value -> value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"));

    private ScreenState<Boolean> last() {
        return states.get(states.size() - 1);
    }

    @Test
    public void completedLoginWaitsForProfileAndPropertyBeforeHome() {
        flow.submit(" user@example.com ", "secret");
        assertEquals(ScreenState.Status.LOADING, last().status);
        assertEquals("user@example.com", gateway.credentials.email);
        gateway.session = true;
        gateway.loginReply.accept(ScreenState.success(true));
        assertEquals(1, passwordCleared);
        assertEquals(1, gateway.routes);
        assertEquals(ScreenState.Status.LOADING, last().status);
        gateway.routeReply.accept(ScreenState.success(true));
        assertEquals(ScreenState.Status.SUCCESS, last().status);
        assertTrue(last().data);
    }

    @Test
    public void incompleteRegistrationRoutesToComplement() {
        gateway.session = true;
        flow.restoreSession();
        gateway.routeReply.accept(ScreenState.success(false));
        assertEquals(ScreenState.Status.SUCCESS, last().status);
        assertFalse(last().data);
    }

    @Test
    public void persistedSessionIsCheckedOnceWithoutSubmittingPassword() {
        gateway.session = true;
        flow.restoreSession();
        flow.restoreSession();
        assertEquals(1, gateway.routes);
        assertEquals(0, gateway.logins);
        assertEquals(1, gateway.refreshes);
        gateway.routeReply.accept(ScreenState.success(true));
        flow.consume();
        flow.restoreSession();
        assertEquals(1, gateway.routes);
    }

    @Test
    public void uncheckedRememberMeAlwaysStaysOnLoginEvenWithSavedTokens() {
        gateway.session = true;
        gateway.remembered = false;
        flow.restoreSession();
        assertEquals(0, gateway.refreshes);
        assertEquals(0, gateway.routes);
        flow.submit("", "");
        assertEquals(ScreenState.Status.ERROR, last().status);
    }

    @Test
    public void rejectedRefreshDoesNotOpenHome() {
        gateway.session = true;
        gateway.refreshResult = ScreenState.error("Sessão expirada", 401, 0);
        flow.restoreSession();
        assertEquals(401, last().code);
        assertEquals(0, gateway.routes);
    }

    @Test
    public void temporaryRefreshFailureCanBeRetried() {
        gateway.session = true;
        gateway.refreshResult = ScreenState.error("Sem conexão");
        flow.restoreSession();
        assertEquals(ScreenState.Status.ERROR, last().status);
        gateway.refreshResult = ScreenState.success(true);
        flow.submit("", "");
        assertEquals(2, gateway.refreshes);
        assertEquals(1, gateway.routes);
    }

    @Test
    public void refreshMustCompleteBeforeRoutingAndLateReplyAfterCancelIsIgnored() {
        gateway.session = true;
        gateway.deferRefresh = true;
        flow.restoreSession();
        assertEquals(0, gateway.routes);
        assertEquals(ScreenState.Status.LOADING, last().status);
        flow.cancelPending();
        gateway.refreshReply.accept(ScreenState.success(true));
        assertEquals(0, gateway.routes);
        assertEquals(ScreenState.Status.IDLE, last().status);
    }

    @Test
    public void noSessionLeavesLoginReady() {
        flow.restoreSession();
        assertEquals(0, gateway.routes);
        assertTrue(states.isEmpty());
    }

    @Test
    public void invalidFieldsNeverReachApi() {
        flow.submit("invalid", "secret");
        assertEquals(ScreenState.Status.ERROR, last().status);
        flow.submit("user@example.com", " ");
        assertEquals(ScreenState.Status.ERROR, last().status);
        flow.submit("user@example.com", "x".repeat(257));
        assertEquals(0, gateway.logins);
    }

    @Test
    public void rejectedCredentialsStayOnLoginAndAllowRetry() {
        flow.submit("user@example.com", "wrong");
        gateway.loginReply.accept(ScreenState.error("Credenciais inválidas", 401, 0));
        assertEquals(401, last().code);
        assertEquals(0, gateway.routes);
        flow.submit("user@example.com", "correct");
        assertEquals(2, gateway.logins);
    }

    @Test
    public void duplicateTapsDoNotSendAnotherLoginOrRoute() {
        flow.submit("user@example.com", "secret");
        flow.submit("user@example.com", "secret");
        assertEquals(1, gateway.logins);
        gateway.session = true;
        gateway.loginReply.accept(ScreenState.success(true));
        flow.route();
        assertEquals(1, gateway.routes);
    }

    @Test
    public void cancellationInvalidatesSessionAndIgnoresLateCompletion() {
        flow.submit("user@example.com", "secret");
        flow.cancelPending();
        gateway.loginReply.accept(ScreenState.success(true));
        assertEquals(ScreenState.Status.IDLE, last().status);
        assertEquals(1, gateway.logouts);
        assertEquals(0, gateway.routes);
        assertEquals(1, passwordCleared);
    }

    @Test
    public void temporarySessionVerificationFailureCanRetryWithoutPassword() {
        gateway.session = true;
        flow.restoreSession();
        gateway.routeReply.accept(ScreenState.error("Sem conexão"));
        flow.submit("", "");
        assertEquals(2, gateway.routes);
        assertEquals(0, gateway.logins);
    }

    @Test
    public void rateLimitPreventsRequestsUntilRetryTime() {
        flow.submit("user@example.com", "secret");
        gateway.loginReply.accept(ScreenState.error("Aguarde", 429, System.currentTimeMillis() + 60000));
        flow.submit("user@example.com", "secret");
        flow.route();
        assertEquals(1, gateway.logins);
        assertEquals(0, gateway.routes);
    }

    @Test
    public void logoutIgnoresLateVerificationAndClearsPassword() {
        gateway.session = true;
        flow.restoreSession();
        flow.logout();
        gateway.routeReply.accept(ScreenState.success(true));
        assertEquals(ScreenState.Status.IDLE, last().status);
        assertFalse(gateway.session);
        assertEquals(1, passwordCleared);
    }

    @Test
    public void rememberedSessionStaysLoadingUntilHomeDecisionWithoutIdleBetweenSteps() {
        gateway.session = true;
        gateway.deferRefresh = true;
        flow.restoreSession();
        assertEquals(ScreenState.Status.LOADING, last().status);
        gateway.refreshReply.accept(ScreenState.success(true));
        assertEquals(ScreenState.Status.LOADING, last().status);
        gateway.routeReply.accept(ScreenState.success(true));
        assertEquals(3, states.size());
        assertEquals(ScreenState.Status.LOADING, states.get(0).status);
        assertEquals(ScreenState.Status.LOADING, states.get(1).status);
        assertEquals(ScreenState.Status.SUCCESS, states.get(2).status);
        assertEquals(0, gateway.logins);
    }
}
