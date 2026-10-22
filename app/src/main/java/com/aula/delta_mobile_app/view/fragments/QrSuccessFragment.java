package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;

public class QrSuccessFragment extends AuthFlowFragment {
    public QrSuccessFragment() {
        super(R.layout.fragment_qr_success);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 4, R.string.auth_qr_title, R.string.auth_qr_subtitle, R.string.auth_qr_description);
        view.findViewById(R.id.back_button).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.success_continue).setOnClickListener(v -> navigate(R.id.accountConfigFragment));
    }
}
