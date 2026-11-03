package com.aula.delta_mobile_app;

import static org.junit.Assert.*;

import android.content.Intent;
import android.widget.EditText;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.viewmodel.SignupViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignupAccountInstrumentedTest {
    @Test
    public void shortPasswordShowsTheRequiredCorrectionOnTheField() {
        checkInvalidPassword("short", "short", R.id.password);
    }

    @Test
    public void differentPasswordSpacesAreRejectedOnTheConfirmationField() {
        checkInvalidPassword("password ", "password", R.id.password_confirm);
    }

    private void checkInvalidPassword(String secret, String confirmation, int invalidField) {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true);
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
                host.getNavController().navigate(R.id.signupAccountFragment);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.email)).setText("user@example.com");
                ((EditText) activity.findViewById(R.id.password)).setText(secret);
                ((EditText) activity.findViewById(R.id.password_confirm)).setText(confirmation);
                assertTrue(activity.findViewById(R.id.step_continue).performClick());
                assertNotNull(((EditText) activity.findViewById(invalidField)).getError());
                assertTrue(activity.findViewById(R.id.step_continue).isEnabled());
                SignupViewModel model = new ViewModelProvider(activity).get(SignupViewModel.class);
                assertEquals(ScreenState.Status.IDLE, model.creation.getValue().status);
            });
        }
    }
}
