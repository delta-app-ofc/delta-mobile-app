package com.aula.delta_mobile_app.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenTransitionStateTest {
    @Test
    public void contentOnlyEntersAfterLoadingHasEnteredAndDataIsReady() {
        ScreenTransitionState state = new ScreenTransitionState();
        int token = state.begin();
        state.ready(true);
        assertFalse(state.reveal(token));
        state.loadingVisible(token);
        assertTrue(state.reveal(token));
        assertEquals(ScreenTransitionState.Phase.ENTERING_CONTENT, state.phase());
        state.complete(token);
        assertEquals(ScreenTransitionState.Phase.CONTENT, state.phase());
    }

    @Test
    public void pendingDataKeepsLoadingVisible() {
        ScreenTransitionState state = new ScreenTransitionState();
        int token = state.begin();
        state.loadingVisible(token);
        assertFalse(state.reveal(token));
        assertEquals(ScreenTransitionState.Phase.LOADING, state.phase());
        state.ready(true);
        assertTrue(state.reveal(token));
    }

    @Test
    public void lateAnimationCallbacksCannotRevealAnotherDestination() {
        ScreenTransitionState state = new ScreenTransitionState();
        int old = state.begin();
        state.loadingVisible(old);
        state.ready(true);
        assertTrue(state.reveal(old));
        int current = state.begin();
        state.complete(old);
        state.loadingVisible(old);
        assertFalse(state.reveal(old));
        assertEquals(ScreenTransitionState.Phase.ENTERING_LOADING, state.phase());
        state.loadingVisible(current);
        assertFalse(state.reveal(current));
    }

    @Test
    public void cachedDataStillPassesThroughLoading() {
        ScreenTransitionState state = new ScreenTransitionState();
        int token = state.begin();
        state.ready(true);
        state.complete(token);
        assertEquals(ScreenTransitionState.Phase.ENTERING_LOADING, state.phase());
        state.loadingVisible(token);
        assertTrue(state.reveal(token));
    }

    @Test
    public void readinessCanBeRevokedBeforeTheReveal() {
        ScreenTransitionState state = new ScreenTransitionState();
        int token = state.begin();
        state.loadingVisible(token);
        state.ready(true);
        state.ready(false);
        assertFalse(state.reveal(token));
    }

    @Test
    public void settledErrorCanRevealItsRetryStateWithoutEndlessLoading() {
        ScreenTransitionState state = new ScreenTransitionState();
        int token = state.begin();
        state.loadingVisible(token);
        assertFalse(state.reveal(token));
        state.ready(true);
        assertTrue(state.reveal(token));
        assertFalse(state.reveal(token));
    }
}
