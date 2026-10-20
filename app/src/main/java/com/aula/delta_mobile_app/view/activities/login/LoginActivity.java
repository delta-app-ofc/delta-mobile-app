package com.aula.delta_mobile_app.view.activities.login;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aula.delta_mobile_app.R;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_SKIP_SESSION_RESTORE = "skip_session_restore";
    public static final String EXTRA_SESSION_ERROR = "session_restore_error";
    public static final String EXTRA_CONTINUE_REGISTRATION = "continue_registration";
    private Bundle flowData;
    private boolean observedSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.aula.delta_mobile_app.viewmodel.SignupViewModel model = new androidx.lifecycle.ViewModelProvider(this).get(com.aula.delta_mobile_app.viewmodel.SignupViewModel.class);
        flowData = model.draft;
        boolean processRestored = savedInstanceState != null && !model.hostInitialized;
        model.hostInitialized = true;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        androidx.navigation.fragment.NavHostFragment host = (androidx.navigation.fragment.NavHostFragment)
        getSupportFragmentManager().findFragmentById(R.id.auth_nav_host);
        if (host != null && (processRestored || host.getNavController().getCurrentDestination() == null)) {
            androidx.navigation.NavGraph graph = host.getNavController().getNavInflater().inflate(R.navigation.auth_navigation);
            if (getIntent().getBooleanExtra(EXTRA_CONTINUE_REGISTRATION, false))
                graph.setStartDestination(R.id.signupHabitsFragment);
            host.getNavController().setGraph(graph);
        }
        com.aula.delta_mobile_app.repository.AuthRepository.getInstance(this).session().observe(this, valid -> {
            if (Boolean.TRUE.equals(valid)) observedSession = true;
            else if (observedSession && host != null) {
                observedSession = false;
                model.reset();
                new androidx.lifecycle.ViewModelProvider(this).get(com.aula.delta_mobile_app.viewmodel.WaterViewModel.class).reset();
                host.getNavController().navigate(R.id.loginFragment, null, new androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.auth_navigation, true)
                    .build());
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_nav_host), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, keyboard.bottom));
            return insets;
        });
    }

    public Bundle getFlowData() {
        return flowData;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
