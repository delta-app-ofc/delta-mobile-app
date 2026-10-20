package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.viewmodel.SignupValidation;

public class SignupAccountFragment extends AuthFlowFragment {
    public SignupAccountFragment() {
        super(R.layout.fragment_signup_account);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 1, R.string.auth_step1_title, R.string.auth_step1_subtitle, R.string.auth_step_description);
        EditText email = view.findViewById(R.id.email);
        EditText password = view.findViewById(R.id.password);
        EditText confirm = view.findViewById(R.id.password_confirm);
        restore(email, "email");
        restore(password, "password");
        restore(confirm, "passwordConfirm");
        bindPasswordVisibility(password, view.findViewById(R.id.password_visibility));
        bindPasswordVisibility(confirm, view.findViewById(R.id.password_confirm_visibility));
        TextView next = view.findViewById(R.id.step_continue);
        watchButton(next, email, password, confirm);
        TextView status = addStatus(view);
        registration().creation.observe(getViewLifecycleOwner(), state -> {
            showLoading(state.busy());
            status.setText(state.status == com.aula.delta_mobile_app.model.ScreenState.Status.ERROR ? state.displayMessage() : "");
            status.setVisibility(state.status == com.aula.delta_mobile_app.model.ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
            next.setEnabled(!state.busy());
            next.setAlpha(state.busy() ? 0.5f : 1f);
            next.setText(state.status == ScreenState.Status.ERROR ? R.string.auth_retry : R.string.auth_continue);
            email.setEnabled(!state.busy());
            password.setEnabled(!state.busy());
            confirm.setEnabled(!state.busy());
            if (state.status == com.aula.delta_mobile_app.model.ScreenState.Status.SUCCESS) {
                password.setText("");
                confirm.setText("");
                androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.signupHabitsFragment, null,
                    new androidx.navigation.NavOptions.Builder().setPopUpTo(R.id.signupAccountFragment, true).build());
            }
        });
        TextView login = new TextView(requireContext());
        login.setText("Já tem uma conta? Entrar");
        login.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.auth_white));
        login.setPadding(0, 16, 0, 16);
        ((android.view.ViewGroup) status.getParent()).addView(login);
        login.setOnClickListener(v -> backToLogin());
        next.setOnClickListener(v -> {
            com.aula.delta_mobile_app.model.ScreenState<Boolean> account = registration().creation.getValue();
            if (account != null && account.status == com.aula.delta_mobile_app.model.ScreenState.Status.SUCCESS) {
                navigate(R.id.signupHabitsFragment);
                return;
            }
            if (!required(email) || !required(password) || !required(confirm)) return;
            email.setError(null);
            password.setError(null);
            confirm.setError(null);
            String emailError = SignupValidation.email(text(email), value -> Patterns.EMAIL_ADDRESS.matcher(value).matches());
            if (emailError != null) {
                email.setError(emailError);
                email.requestFocus();
                return;
            }
            String secret = password.getText().toString();
            String confirmation = confirm.getText().toString();
            String passwordError = SignupValidation.password(secret);
            if (passwordError != null) {
                password.setError(passwordError);
                password.requestFocus();
                return;
            }
            if (!secret.equals(confirmation)) {
                confirm.setError(getString(R.string.auth_passwords_differ));
                confirm.requestFocus();
                return;
            }
            save(email, "email");
            registration().secret("password", secret);
            registration().secret("passwordConfirm", confirmation);
            registration().create();
        });
    }
}
