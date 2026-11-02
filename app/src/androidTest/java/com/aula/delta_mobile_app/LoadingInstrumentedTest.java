package com.aula.delta_mobile_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;
import com.aula.delta_mobile_app.viewmodel.LoginViewModel;
import com.aula.delta_mobile_app.viewmodel.SignupViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoadingInstrumentedTest {
    @Test
    public void splashStartsVisibleWithoutLoadingOverlay() {
        try (ActivityScenario<com.aula.delta_mobile_app.view.activities.splash.SplashActivity> scenario = ActivityScenario.launch(com.aula.delta_mobile_app.view.activities.splash.SplashActivity.class)) {
            scenario.onActivity(activity -> {
                org.junit.Assert.assertTrue(activity.findViewById(R.id.delta_splash).isShown());
                org.junit.Assert.assertNull(activity.findViewById(R.id.screen_flow_loading));
            });
        }
    }

    @Test
    public void loadingAppearsAndClosesOnErrorInBothFlows() throws Exception {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            awaitLoading(scenario, false);
            onView(withId(R.id.login_user)).check(matches(isDisplayed()));
            awaitLoading(scenario, false);
            onView(withId(R.id.screen_flow_loading)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            scenario.onActivity(activity -> org.junit.Assert.assertEquals(ScreenState.Status.IDLE, new ViewModelProvider(activity).get(LoginViewModel.class).state.getValue()
                .status));
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(LoginViewModel.class).state.setValue(ScreenState.loading()));
            awaitLoading(scenario, true);
            onView(withId(R.id.screen_flow_loading)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                NavHostFragment host = (NavHostFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
                org.junit.Assert.assertEquals(android.view.View.INVISIBLE, host.getChildFragmentManager().getPrimaryNavigationFragment().requireView().getVisibility());
            });
            android.graphics.Bitmap preview = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            if (preview != null) {
                java.io.File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null);
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(directory, "loading-preview.png"))) {
                    preview.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
                }
                preview.recycle();
            }
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(LoginViewModel.class).state.setValue(ScreenState.error("Teste de falha")));
            awaitLoading(scenario, false);
            onView(withId(R.id.screen_flow_loading)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.login_user)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(LoginViewModel.class).state.setValue(ScreenState.loading()));
            pressBack();
            awaitLoading(scenario, false);
            onView(withId(R.id.screen_flow_loading)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            scenario.onActivity(activity -> ((NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host)).getNavController()
                .navigate(R.id.signupHabitsFragment));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> ((NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host)).getNavController().popBackStack());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            onView(withId(R.id.login_user)).check(matches(isDisplayed()));
            awaitLoading(scenario, false);
            onView(withId(R.id.screen_flow_loading)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            scenario.onActivity(activity -> ((NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.auth_nav_host)).getNavController()
                .navigate(R.id.signupAddressFragment));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(SignupViewModel.class).completion.setValue(ScreenState.loading()));
            awaitLoading(scenario, true);
            onView(withId(R.id.screen_flow_loading)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> new ViewModelProvider(activity).get(SignupViewModel.class).completion.setValue(ScreenState.error("Teste de falha")));
            awaitLoading(scenario, false);
            onView(withId(R.id.screen_flow_loading)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }
    private void awaitLoading(ActivityScenario<LoginActivity> scenario, boolean expected) throws Exception {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        java.util.concurrent.atomic.AtomicBoolean matched = new java.util.concurrent.atomic.AtomicBoolean();
        do {
            scenario.onActivity(activity -> {
                android.view.View loading = activity.findViewById(R.id.screen_flow_loading);
                matched.set(loading != null && loading.isShown() == expected);
            });
            if (matched.get()) return;
            Thread.sleep(25);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Loading visibility did not settle: " + expected);
    }
}
