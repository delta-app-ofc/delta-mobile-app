package com.aula.delta_mobile_app.repository;

import android.content.Context;

import com.aula.delta_mobile_app.services.network.GsonProvider;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SignupOptions {
    public static class Entry {
        public int id;
        public String key, label;
    }

    public List<Entry> habits = new ArrayList<>(), days = new ArrayList<>();

    public static SignupOptions load(Context context) {
        try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("registration_catalog.json"), StandardCharsets.UTF_8)) {
            SignupOptions catalog = GsonProvider.getInstance().fromJson(reader, SignupOptions.class);
            if (catalog != null && catalog.habits != null && catalog.days != null) return catalog;
        } catch (Exception ignored) {
        }
        return new SignupOptions();
    }

    public Integer habit(String key) {
        for (Entry entry : habits) if (key.equals(entry.key) && entry.id > 0) return entry.id;
        return null;
    }

}
