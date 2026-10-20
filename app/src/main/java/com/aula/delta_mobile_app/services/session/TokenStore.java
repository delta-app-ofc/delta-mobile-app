package com.aula.delta_mobile_app.services.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.aula.delta_mobile_app.model.AuthModels.Tokens;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class TokenStore implements SessionStore {
    private static final String ALIAS = "delta_session_aes";
    private final SharedPreferences prefs;
    private volatile long generation;

    public TokenStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("delta_session", Context.MODE_PRIVATE);

        if (prefs.contains("refresh_token_sha256"))
            prefs.edit().remove("refresh_token_sha256").apply();
    }

    public long generation() {
        return generation;
    }

    public boolean hasSession() {
        return prefs.contains("access_token") && prefs.getLong("refresh_expires_at", 0) > System.currentTimeMillis();
    }

    public String access() {
        return read("access_token");
    }

    public String refresh() {
        return read("refresh_token");
    }

    public long accessExpiresAt() {
        return prefs.getLong("access_expires_at", 0);
    }

    public boolean save(Tokens tokens, long expected) throws Exception {
        if (expected != generation) return false;
        if (tokens == null || !"Bearer".equalsIgnoreCase(tokens.tokenType) || empty(tokens.accessToken) || empty(tokens.refreshToken) || tokens.expiresIn <= 0
            || tokens.refreshExpiresIn <= 0) throw new IllegalArgumentException();
        long now = System.currentTimeMillis();
        String access = encrypt(tokens.accessToken), refresh = encrypt(tokens.refreshToken);
        synchronized (this) {
            if (expected != generation) return false;
            boolean saved = prefs.edit().clear().putString("access_token", access).putString("refresh_token", refresh).putLong("access_expires_at", expiry(now, tokens.expiresIn))
                .putLong("refresh_expires_at", expiry(now, tokens.refreshExpiresIn)).commit();
            if (!saved) {
                clear();
                throw new java.io.IOException("Session storage unavailable");
            }
            generation++;
            return true;
        }
    }

    public boolean updateAccess(Tokens tokens, long expected) throws Exception {
        if (generation != expected) return false;
        if (tokens == null || empty(tokens.accessToken) || tokens.expiresIn <= 0 || !"Bearer".equalsIgnoreCase(tokens.tokenType))
            throw new IllegalArgumentException();
        String encrypted = encrypt(tokens.accessToken);
        synchronized (this) {
            if (expected != generation) return false;
            if (!prefs.edit().putString("access_token", encrypted).putLong("access_expires_at", expiry(System.currentTimeMillis(), tokens.expiresIn))
                .commit()) throw new java.io.IOException("Session storage unavailable");
            return true;
        }
    }

    public synchronized void clear() {
        generation++;
        prefs.edit().clear().apply();
    }

    public void saveProfile(com.aula.delta_mobile_app.model.AuthModels.Profile profile, long expected)
    throws Exception {
        if (generation != expected) return;
        String json = com.aula.delta_mobile_app.services.network.GsonProvider.getInstance().toJson(profile);
        String encrypted = encrypt(json);
        synchronized (this) {
            if (generation != expected) return;
            if (!prefs.edit().putString("profile", encrypted).commit())
                throw new java.io.IOException("Profile storage unavailable");
        }
    }

    private String read(String key) {
        long expected = generation;
        String value = prefs.getString(key, null);
        if (value == null) return null;
        try {
            String[] parts = value.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            String decrypted = new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
            return generation == expected ? decrypted : null;
        } catch (Exception failure) {
            synchronized (this) {
                if (generation == expected) clear();
            }
            return null;
        }
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        return Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" + Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (SecretKey) store.getKey(ALIAS, null);
    }

    private static long expiry(long now, long seconds) {
        return Math.addExact(now, Math.multiplyExact(seconds, 1000L));
    }

    private static boolean empty(String value) {
        return value == null || value.isEmpty();
    }
}
