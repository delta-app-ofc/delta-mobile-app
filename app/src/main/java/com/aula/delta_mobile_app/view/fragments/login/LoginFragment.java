package com.aula.delta_mobile_app.view.fragments.login;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.fragments.AuthFlowFragment;
import com.aula.delta_mobile_app.viewmodel.LoginViewModel;

public class LoginFragment extends AuthFlowFragment {
    private boolean leavingLogin;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    protected boolean canCancelLoading() {
        return true;
    }

    @Override
    protected void onLoadingBack() {
        if (leavingLogin) return;
        new ViewModelProvider(requireActivity()).get(LoginViewModel.class).cancelPending();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setScreenColor(R.color.auth_dark);
        leavingLogin = false;
        view.setVisibility(View.INVISIBLE);
        LoginViewModel model = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        android.content.Intent entry = requireActivity().getIntent();
        if (entry.getBooleanExtra(com.aula.delta_mobile_app.view.activities.login.LoginActivity.EXTRA_SKIP_SESSION_RESTORE, false)) {
            String error = entry.getStringExtra(com.aula.delta_mobile_app.view.activities.login.LoginActivity.EXTRA_SESSION_ERROR);
            if (error != null && model.state.getValue().status == ScreenState.Status.IDLE)
                model.state.setValue(ScreenState.error(error));
            entry.removeExtra(com.aula.delta_mobile_app.view.activities.login.LoginActivity.EXTRA_SESSION_ERROR);
        } else model.restoreSession();
        com.google.android.material.checkbox.MaterialCheckBox remember = view.findViewById(R.id.login_remember_me);
        remember.setChecked(model.rememberMe);
        remember.setOnCheckedChangeListener((button, checked) -> model.rememberMe = checked);
        EditText user = view.findViewById(R.id.login_user), password = view.findViewById(R.id.login_password);
        user.setText(model.email);
        password.setSaveEnabled(false);
        password.setText(model.password);
        user.addTextChangedListener(watcher(value -> model.email = value));
        password.addTextChangedListener(watcher(value -> model.password = value));
        TextView next = view.findViewById(R.id.login_continue);
        next.setText("Entrar");
        TextView status = addStatus(view);
        bindPasswordVisibility(password, view.findViewById(R.id.login_password_visibility));
        model.state.observe(getViewLifecycleOwner(), value -> {
            if (leavingLogin) return;
            if (value.status == ScreenState.Status.SUCCESS) {
                leavingLogin = true;
                view.setVisibility(View.INVISIBLE);
                showLoading(true);
                boolean completed = Boolean.TRUE.equals(value.data);
                model.consume();
                password.setText("");
                registration().reset();
                if (completed) openAuthenticated();
                else {
                    showLoading(false);
                    navigate(R.id.signupHabitsFragment);
                }
                return;
            }
            view.setVisibility(value.busy() ? View.INVISIBLE : View.VISIBLE);
            showLoading(value.busy());
            showStatus(status, next, value);
            if (value.busy()) status.setText("");
            user.setEnabled(!value.busy());
            password.setEnabled(!value.busy());
            view.findViewById(R.id.login_register).setEnabled(!value.busy());
            remember.setEnabled(!value.busy());
            if (model.password.isEmpty() && password.length() > 0) password.setText("");
        });
        view.findViewById(R.id.login_register).setOnClickListener(v -> {
            model.logout();
            registration().reset();
            navigate(R.id.welcomeFragment);
        });
        next.setOnClickListener(v -> model.submit());
        for (int id : new int[] {R.id.social_facebook, R.id.social_apple, R.id.social_google}) {
            view.findViewById(id).setVisibility(View.GONE);
        }
    }

    private TextWatcher watcher(java.util.function.Consumer<String> consumer) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                consumer.accept(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }
}
