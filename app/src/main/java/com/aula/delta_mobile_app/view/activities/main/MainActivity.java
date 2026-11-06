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
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.repository.AuthRepository;
import com.aula.delta_mobile_app.services.notifications.RoutineReminderScheduler;
import com.aula.delta_mobile_app.view.activities.login.LoginActivity;

public class MainActivity extends com.aula.delta_mobile_app.view.activities.ScreenFlowActivity {
    public static final String EXTRA_RESTORE_SESSION = "restore_session_in_main";
    public static final String EXTRA_OPEN_ROUTINE = "open_routine_from_notification";
    public static final String ACTION_OPEN_ROUTINE = "com.aula.delta_mobile_app.action.OPEN_ROUTINE";
    private boolean restoringSession;
    private boolean openRoutinePending;
    private boolean returningToLogin;
    private boolean keyboardVisible;
    private boolean detailScreen;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        restoringSession = getIntent().getBooleanExtra(EXTRA_RESTORE_SESSION, false);
        openRoutinePending = getIntent().getBooleanExtra(EXTRA_OPEN_ROUTINE, false);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
                Runnable navigate = () -> MainNavigation.open(nav, destination);
                androidx.fragment.app.Fragment current = host.getChildFragmentManager().getPrimaryNavigationFragment();
                if (current instanceof com.aula.delta_mobile_app.view.fragments.ProfileNavigationGuard)
                    ((com.aula.delta_mobile_app.view.fragments.ProfileNavigationGuard) current).leaveProfile(navigate);
                else navigate.run();
            });
        }
        nav.addOnDestinationChangedListener((controller, destination, args) -> {
            detailScreen = destination.getId() == R.id.qrScanFragment || destination.getId() == R.id.qrSuccessFragment || destination.getId() == R.id.accountConfigFragment;
            updateMenu();
            updateLoading();
            for (int i = 0; i < buttons.length; i++) {
                View button = findViewById(buttons[i]);
                boolean selected = buttons[i] == R.id.menu_home ? destination.getId() == R.id.homeFragment : buttons[i] == MainNavigation.tab(destination.getId());
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
                    openRoutineIfRequested(nav);
                    updateLoading();
                    updateMenu();
                } else if (value.status == com.aula.delta_mobile_app.model.ScreenState.Status.ERROR) {
                    openAuthentication(value.displayMessage(), false);
                }
            });
        } else {
            if (nav.getCurrentDestination() == null) nav.setGraph(R.navigation.main_navigation);
            openRoutineIfRequested(nav);
        }
        AuthRepository.getInstance(this).session().observe(this, valid -> {
            if (Boolean.TRUE.equals(valid)) RoutineReminderScheduler.scheduleStored(this, true);
            if (!restoringSession && !Boolean.TRUE.equals(valid) && !AuthRepository.getInstance(this).hasSession() && !returningToLogin) {
                returningToLogin = true;
                openScreen(new Intent(this, LoginActivity.class), true);
            }
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.main_shell));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!intent.getBooleanExtra(EXTRA_OPEN_ROUTINE, false)) return;
        openRoutinePending = true;
        if (!restoringSession && AuthRepository.getInstance(this).hasSession()) {
            NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
            if (host != null) openRoutineIfRequested(host.getNavController());
        }
    }

    private void openRoutineIfRequested(NavController nav) {
        if (!openRoutinePending || nav.getCurrentDestination() == null) return;
        openRoutinePending = false;
        MainNavigation.open(nav, R.id.goalsFragment);
    }

    public void setHomeLoading(boolean loading) {
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_nav_host);
        androidx.fragment.app.Fragment current = host == null ? null : host.getChildFragmentManager().getPrimaryNavigationFragment();
        if (current != null && current.getView() != null) setScreenLoading(current.getView(), loading);
    }

    private void openAuthentication(String message, boolean incompleteRegistration) {
        if (returningToLogin) return;
        returningToLogin = true;
        Intent entry = new Intent(this, LoginActivity.class).putExtra(LoginActivity.EXTRA_SKIP_SESSION_RESTORE, true).putExtra(
            LoginActivity.EXTRA_CONTINUE_REGISTRATION, incompleteRegistration);
        if (openRoutinePending) entry.putExtra(LoginActivity.EXTRA_OPEN_ROUTINE, true);
        if (message != null) entry.putExtra(LoginActivity.EXTRA_SESSION_ERROR, message);
        openScreen(entry, true);
    }

    private void updateLoading() { blockScreen(restoringSession); }

    public void setSectionLoading(View owner, boolean loading) { setScreenLoading(owner, loading); }

    private void updateMenu() {
        findViewById(R.id.main_menu).setVisibility(keyboardVisible || detailScreen || restoringSession ? View.GONE : View.VISIBLE);
    }
}
