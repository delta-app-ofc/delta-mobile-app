package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScreenStateTest {
    @Test
    public void errorsKeepTheirDetailsWithoutDisplayingThem() {
        ScreenState<Object> state = ScreenState.error("Connection failed", 503, 1234);

        assertEquals("", state.displayMessage());
        assertEquals("Connection failed", state.message);
        assertEquals(ScreenState.Status.ERROR, state.status);
        assertEquals(503, state.code);
        assertEquals(1234, state.retryAt);
        assertFalse(state.busy());
    }

    @Test
    public void localErrorsAreAlsoHidden() {
        assertEquals("", ScreenState.error("Invalid response").displayMessage());
    }

    @Test
    public void loadingAndSuccessKeepTheirNormalMessages() {
        ScreenState<Object> loading = ScreenState.loading();
        assertEquals(loading.message, loading.displayMessage());
        assertTrue(loading.busy());
        assertEquals("", ScreenState.success("saved").displayMessage());
    }
}
