package com.aula.delta_mobile_app.repository;

import android.content.Context;

import com.aula.delta_mobile_app.services.network.GsonProvider;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public final class SignupOptions {
    public static class Entry {
        public int id;
        public String key, label;
    }

    public static class Region {
        public int regionId;
        public String state, city;
    }

    public List<Entry> habits = new ArrayList<>(), days = new ArrayList<>();
    public List<Region> regions = new ArrayList<>();

    public static SignupOptions load(Context context) {
        try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("registration_catalog.json"), StandardCharsets.UTF_8)) {
            SignupOptions catalog = GsonProvider.getInstance().fromJson(reader, SignupOptions.class);
            if (catalog != null && catalog.habits != null && catalog.days != null && catalog.regions != null) return catalog;
        } catch (Exception ignored) {
        }
        return new SignupOptions();
    }

    public Integer habit(String key) {
        for (Entry entry : habits) if (key.equals(entry.key) && entry.id > 0) return entry.id;
        return null;
    }

    public Integer region(String state, String city) {
        Integer result = null;
        for (Region entry : regions)
            if (entry.regionId > 0 && normalize(state).equals(normalize(entry.state)) && normalize(city).equals(normalize(entry.city))) {
            if (result != null && result != entry.regionId) return null;
            result = entry.regionId;
        }
        return result;
    }

    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }
}
