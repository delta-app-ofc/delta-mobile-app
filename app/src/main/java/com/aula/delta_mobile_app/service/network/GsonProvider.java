package com.aula.delta_mobile_app.service.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Fornece a configuracao unica do Gson utilizada pela camada de rede. */
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
