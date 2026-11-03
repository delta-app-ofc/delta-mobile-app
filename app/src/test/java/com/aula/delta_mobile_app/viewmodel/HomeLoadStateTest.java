package com.aula.delta_mobile_app.viewmodel;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ScreenState;

import org.junit.Test;

public class HomeLoadStateTest {
    private final ScreenState<String> ok = ScreenState.success("data");
    private final ScreenState<String> idle = ScreenState.idle();
    private final ScreenState<String> loading = ScreenState.loading();
    private final ScreenState<String> error = ScreenState.error("Falha de rede");

    @Test
    public void waitsForProfileAndPropertyBeforeShowingAnyMetrics() {
        assertFalse(HomeLoadState.ready(loading, ok, true, ok, ok, true));
        assertFalse(HomeLoadState.ready(ok, idle, false, idle, idle, false));
    }

    @Test
    public void waitsForCostStartedAfterConsumptionFinishes() {
        assertFalse(HomeLoadState.ready(ok, ok, true, loading, idle, false));
        assertFalse(HomeLoadState.ready(ok, ok, true, ok, idle, true));
        assertFalse(HomeLoadState.ready(ok, ok, true, ok, loading, true));
        assertTrue(HomeLoadState.ready(ok, ok, true, ok, ok, true));
    }

    @Test
    public void noPropertyOrNoReadingsDoesNotWaitForAnImpossibleCostRequest() {
        assertTrue(HomeLoadState.ready(ok, ok, false, idle, idle, false));
        assertTrue(HomeLoadState.ready(ok, ok, true, ok, idle, false));
    }

    @Test
    public void failedRequestsRevealRetryInsteadOfLoadingForever() {
        assertTrue(HomeLoadState.ready(error, error, false, idle, idle, false));
        assertTrue(HomeLoadState.ready(ok, ok, true, error, idle, false));
        assertTrue(HomeLoadState.ready(ok, ok, true, ok, error, true));
    }

    @Test
    public void refreshingPropertyHidesPreviouslyLoadedDashboard() {
        assertFalse(HomeLoadState.ready(ok, loading, true, ok, ok, true));
        assertFalse(HomeLoadState.ready(ok, ok, true, loading, ok, true));
    }
}
