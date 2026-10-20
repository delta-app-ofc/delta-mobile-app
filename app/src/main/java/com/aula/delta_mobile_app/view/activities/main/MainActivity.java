package com.aula.delta_mobile_app.view.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_RESTORE_SESSION = "restore_session_in_main";
    private boolean restoringSession;
    private boolean returningToLogin;
    private boolean keyboardVisible;
    private boolean detailScreen;
    private boolean homeLoading = true;
    private com.aula.delta_mobile_app.view.LoadingView homeLoader;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        restoringSession = getIntent().getBooleanExtra(EXTRA_RESTORE_SESSION, false);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        homeLoader = new com.aula.delta_mobile_app.view.LoadingView(this);
        homeLoader.setVisibility(homeLoading ? View.VISIBLE : View.GONE);
        homeLoader.setClickable(true);
        homeLoader.setFocusable(true);
        ((android.widget.FrameLayout) findViewById(R.id.main_shell)).addView(homeLoader, new android.widget.FrameLayout.LayoutParams(-1, -1));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_shell), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            updateMenu();
            return insets;
        });
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
        NavController nav = host.getNavController();
        int[] buttons = {
            R.id.menu_consumption, R.id.menu_costs, R.id.menu_home, R.id.menu_tips, R.id.menu_goals
        };
        int[] screens = {
            R.id.leakDetectionFragment, R.id.costsFragment, R.id.homeFragment, R.id.tipsFragment, R.id.goalsFragment
        };
        for (int i = 0; i < buttons.length; i++) {
            final int destination = screens[i];
            findViewById(buttons[i]).setOnClickListener(v -> {
                if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == destination)
                    return;
                nav.navigate(destination, null, new NavOptions.Builder().setPopUpTo(R.id.homeFragment, false, true).setRestoreState(
                    destination != R.id.leakDetectionFragment).setLaunchSingleTop(true).build());
            });
        }
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            if (destination.getId() != R.id.homeFragment) setHomeLoading(false);
            detailScreen = destination.getId() == R.id.qrScanFragment || destination.getId() == R.id.qrSuccessFragment || destination.getId() == R.id.accountConfigFragment;
            updateMenu();
            for (int i = 0; i < buttons.length; i++) {
                View button = findViewById(buttons[i]);
                boolean leakScreen = destination.getId() == R.id.consumptionFragment || destination.getId() == R.id.leakDetectionFragment
                    || destination.getId() == R.id.leakHourlyFragment || destination.getId() == R.id.leakAbnormalFragment;
                boolean selected = destination.getId() == screens[i] || (i == 0 && leakScreen) || (i == 1 && destination.getId() == R.id.costsHistoryFragment);
                button.setSelected(selected);
                button.setAlpha(selected ? 1f : .55f);
            }
        });
        if (restoringSession && (!AuthRepository.getInstance(this).rememberMe() || !AuthRepository.getInstance(this).hasSession())) {
            openAuthentication(null, false);
        } else if (restoringSession) {
            com.aula.delta_mobile_app.viewmodel.LoginViewModel login = new androidx.lifecycle.ViewModelProvider(this).get(com.aula.delta_mobile_app.viewmodel.LoginViewModel.class);
            login.restoreSession();
            login.state.observe(this, value -> {
                if (!restoringSession || returningToLogin) return;
                if (value.status == com.aula.delta_mobile_app.model.ScreenState.Status.SUCCESS) {
                    if (!Boolean.TRUE.equals(value.data)) {
                        openAuthentication(null, true);
                        return;
                    }
                    restoringSession = false;
                    if (nav.getCurrentDestination() == null)
                        nav.setGraph(R.navigation.main_navigation);
                    updateLoading();
                    updateMenu();
                } else if (value.status == com.aula.delta_mobile_app.model.ScreenState.Status.ERROR) {
                    openAuthentication(value.displayMessage(), false);
                }
            });
        } else if (nav.getCurrentDestination() == null) nav.setGraph(R.navigation.main_navigation);
        AuthRepository.getInstance(this).session().observe(this, valid -> {
            if (!restoringSession && !Boolean.TRUE.equals(valid) && !AuthRepository.getInstance(this).hasSession() && !returningToLogin) {
                returningToLogin = true;
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.main_shell));
    }

    public void setHomeLoading(boolean loading) {
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
        if (host != null && host.getNavController().getCurrentDestination() != null && host.getNavController().getCurrentDestination().getId() != R.id.homeFragment)
            loading = false;
        homeLoading = loading;
        updateLoading();
        updateMenu();
    }

    private void openAuthentication(String message, boolean incompleteRegistration) {
        if (returningToLogin) return;
        returningToLogin = true;
        Intent entry = new Intent(this, LoginActivity.class).putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true).putExtra(
            LoginActivity.EXTRA_CONTINUE_REGISTRATION, incompleteRegistration);
        if (message != null) entry.putExtra(LoginActivity.EXTRA_SESSION_ERROR, message);
        startActivity(entry);
        finish();
    }

    private void updateLoading() {
        if (homeLoader != null)
            homeLoader.setVisibility(restoringSession || homeLoading ? View.VISIBLE : View.GONE);
    }

    private void updateMenu() {
        findViewById(R.id.main_menu).setVisibility(keyboardVisible || detailScreen || restoringSession || homeLoading ? View.GONE : View.VISIBLE);
    }
}
