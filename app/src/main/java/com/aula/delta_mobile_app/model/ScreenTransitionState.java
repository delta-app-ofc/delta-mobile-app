package com.aula.delta_mobile_app.model;

public final class ScreenTransitionState {
    public enum Phase { CONTENT, ENTERING_LOADING, LOADING, ENTERING_CONTENT }
    private Phase phase = Phase.CONTENT;
    private int generation;
    private boolean ready;

    public int begin() { generation++; ready = false; phase = Phase.ENTERING_LOADING; return generation; }
    public int generation() { return generation; }
    public Phase phase() { return phase; }
    public void loadingVisible(int token) { if (token == generation && phase == Phase.ENTERING_LOADING) phase = Phase.LOADING; }
    public void ready(boolean value) { ready = value; }
    public boolean reveal(int token) {
        if (token != generation || phase != Phase.LOADING || !ready) return false;
        phase = Phase.ENTERING_CONTENT;
        return true;
    }
    public void complete(int token) { if (token == generation && phase == Phase.ENTERING_CONTENT) phase = Phase.CONTENT; }
}
