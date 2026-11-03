package com.aula.delta_mobile_app.model;

public final class ScreenState<T> {
    public enum Status {
        IDLE, LOADING, SUCCESS, ERROR
    }

    public final Status status;
    public final T data;
    public final String message;
    public final int code;
    public final long retryAt;

    private ScreenState(Status status, T data, String message, int code, long retryAt) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
        this.retryAt = retryAt;
    }

    public static <T> ScreenState<T> idle() {
        return new ScreenState<>(Status.IDLE, null, "", 0, 0);
    }

    public static <T> ScreenState<T> loading() {
        return new ScreenState<>(Status.LOADING, null, "Aguarde…", 0, 0);
    }

    public static <T> ScreenState<T> success(T data) {
        return new ScreenState<>(Status.SUCCESS, data, "", 0, 0);
    }

    public static <T> ScreenState<T> error(String message) {
        return error(message, 0, 0);
    }

    public static <T> ScreenState<T> error(String message, int code, long retryAt) {
        return new ScreenState<>(Status.ERROR, null, message, code, retryAt);
    }

    public String displayMessage() {
        return status == Status.ERROR ? "" : message;
    }

    public boolean busy() {
        return status == Status.LOADING;
    }
}
