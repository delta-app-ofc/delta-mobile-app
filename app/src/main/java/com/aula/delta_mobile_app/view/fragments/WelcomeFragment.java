package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;

public class WelcomeFragment extends AuthFlowFragment {
    public WelcomeFragment() {
        super(R.layout.fragment_welcome);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setScreenColor(R.color.auth_dark);
        View.OnClickListener next = v -> navigate(R.id.signupProfileFragment);
        view.findViewById(R.id.welcome_continue).setOnClickListener(next);
        view.findViewById(R.id.welcome_back).setOnClickListener(v -> backToLogin());
    }
}
