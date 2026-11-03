package com.aula.delta_mobile_app.services.session;

import com.aula.delta_mobile_app.model.AuthModels.Tokens;

public interface SessionStore {
    long generation();

    boolean hasSession();

    String access();

    String refresh();

    long accessExpiresAt();

    boolean updateAccess(Tokens tokens, long expected) throws Exception;

    void clear();
}
