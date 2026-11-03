package com.aula.delta_mobile_app.viewmodel;

import com.aula.delta_mobile_app.model.ScreenState;

public final class HomeLoadState {
    private HomeLoadState() {}

    public static boolean ready(ScreenState<?> profile, ScreenState<?> properties, boolean hasProperty, ScreenState<?> consumption, ScreenState<?> cost, boolean needsCost) {
        if (!settled(profile) || !settled(properties)) return false;
        if (!hasProperty) return true;
        return settled(consumption) && (!needsCost || settled(cost));
    }

    private static boolean settled(ScreenState<?> state) {
        return state != null && (state.status == ScreenState.Status.SUCCESS || state.status == ScreenState.Status.ERROR);
    }
}
