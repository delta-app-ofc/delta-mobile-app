package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ChatModels.*;
import com.aula.delta_mobile_app.model.ScreenState;

import java.util.List;
import java.util.function.Consumer;

public interface ChatActions {
    void sessions(Consumer<ScreenState<List<Session>>> callback);

    void history(String sessionId, Consumer<ScreenState<List<HistoryMessage>>> callback);
}
