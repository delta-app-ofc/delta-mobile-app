package com.aula.delta_mobile_app;

import static org.junit.Assert.*;

import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.view.activities.login.LoginActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PasswordVisibilityInstrumentedTest {
    @Test
    public void loginAndBothSignupPasswordsToggleIndependently() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
                host.getNavController().navigate(R.id.loginFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                checkToggle(activity.findViewById(R.id.login_password), activity.findViewById(R.id.login_password_visibility));
                androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
                host.getNavController().navigate(R.id.signupAccountFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                EditText password = activity.findViewById(R.id.password);
                EditText confirm = activity.findViewById(R.id.password_confirm);
                checkToggle(password, activity.findViewById(R.id.password_visibility));
                assertTrue(confirm.getTransformationMethod() instanceof PasswordTransformationMethod);
                checkToggle(confirm, activity.findViewById(R.id.password_confirm_visibility));
                assertTrue(password.getTransformationMethod() instanceof PasswordTransformationMethod);
            });
        }
    }

    private void checkToggle(EditText field, View eye) {
        assertNotNull(field);
        assertNotNull(eye);
        field.setText("Senha de teste 123!");
        field.setSelection(4);
        assertTrue(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        eye.performClick();
        assertFalse(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals("Ocultar senha", eye.getContentDescription());
        assertEquals(4, field.getSelectionStart());
        eye.performClick();
        assertTrue(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals("Mostrar senha", eye.getContentDescription());
    }
}
