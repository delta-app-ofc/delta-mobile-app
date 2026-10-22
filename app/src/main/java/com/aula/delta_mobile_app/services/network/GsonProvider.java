package com.aula.delta_mobile_app.services.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonProvider {
    private GsonProvider() {
        throw new IllegalStateException("Classe utilitaria nao deve ser instanciada");
    }

    private static final class Holder {
        private static final Gson INSTANCE = new GsonBuilder().create();
    }

    public static Gson getInstance() {
        return Holder.INSTANCE;
    }
}
