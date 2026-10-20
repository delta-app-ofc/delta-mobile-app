package com.aula.delta_mobile_app;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.AuthModels.Tokens;
import com.aula.delta_mobile_app.services.session.TokenStore;
import com.aula.delta_mobile_app.viewmodel.SignupViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class AuthStateInstrumentedTest {
    @Test
    public void keystorePersistenceRefreshAndLogout() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TokenStore store = new TokenStore(context);
        store.clear();
        try {
            Tokens initial = new Tokens();
            initial.accessToken = "access-test";
            initial.refreshToken = "refresh-original-test";
            initial.tokenType = "Bearer";
            initial.expiresIn = 900;
            initial.refreshExpiresIn = 86400;
            assertTrue(store.save(initial, store.generation()));
            android.content.SharedPreferences prefs = context.getSharedPreferences("delta_session", Context.MODE_PRIVATE);
            assertNotEquals(initial.accessToken, prefs.getString("access_token", null));
            assertNotEquals(initial.refreshToken, prefs.getString("refresh_token", null));
            assertFalse(prefs.contains("refresh_token_sha256"));
            long refreshExpiry = prefs.getLong("refresh_expires_at", 0);
            prefs.edit().putString("refresh_token_sha256", "legacy-unused-digest").commit();
            TokenStore reopened = new TokenStore(context);
            assertEquals(initial.refreshToken, reopened.refresh());
            assertFalse(prefs.contains("refresh_token_sha256"));
            Tokens renewal = new Tokens();
            renewal.accessToken = "access-renewed-test";
            renewal.tokenType = "Bearer";
            renewal.expiresIn = 600;
            assertTrue(store.updateAccess(renewal, store.generation()));
            assertEquals(initial.refreshToken, store.refresh());
            assertEquals(refreshExpiry, prefs.getLong("refresh_expires_at", 0));
            long beforeLogout = store.generation();
            store.clear();
            assertFalse(store.updateAccess(renewal, beforeLogout));
            assertTrue(prefs.getAll().isEmpty());
        } finally {
            store.clear();
        }
    }

    @Test
    public void registrationValidatesDatesAndKeepsSecretsOutOfDraft() {
        Application app = (Application)
        InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SignupViewModel model = new SignupViewModel(app);
            model.draft.putString("name", "Teste");
            model.draft.putString("birthDate", "29/02/2000");
            assertNull(model.personalError());
            java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu");
            model.draft.putString("birthDate", SignupViewModel.latestAllowedBirthDate().format(format));
            assertNull(model.personalError());
            model.draft.putString("birthDate", SignupViewModel.latestAllowedBirthDate().plusDays(1).format(format));
            assertNotNull(model.personalError());
            model.draft.putString("birthDate", "29/02/2001");
            assertNotNull(model.personalError());
            model.secret("password", "senha-teste");
            assertFalse(model.draft.containsKey("password"));
            assertNull(model.setHabit("shower", 2, Arrays.asList(1, 3)));
            assertNotNull(model.setHabit("yard", 0, Arrays.asList(1)));
            assertNotNull(model.setHabit("yard", 2, Arrays.asList(1, 1)));
            model.reset();
            assertNull(model.habit("shower"));
            assertTrue(model.secret("password").isEmpty());
        });
    }
}
