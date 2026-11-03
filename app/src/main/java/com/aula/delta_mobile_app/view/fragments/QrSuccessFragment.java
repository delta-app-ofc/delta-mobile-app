package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;

public class QrSuccessFragment extends AuthFlowFragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoContinue = () -> {
        if (!isResumed()) return;
        if (getArguments() != null && getArguments().getBoolean("unitFlow", false)) {
            androidx.navigation.NavController nav = androidx.navigation.fragment.NavHostFragment.findNavController(this);
            if (!nav.popBackStack(R.id.unitsFragment, false)) nav.navigate(R.id.unitsFragment);
        } else navigate(R.id.accountConfigFragment);
    };

    public QrSuccessFragment() {
        super(R.layout.fragment_qr_success);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 4, R.string.auth_qr_title, R.string.auth_qr_subtitle, R.string.auth_qr_description);
        view.findViewById(R.id.back_button).setVisibility(View.GONE);
        view.findViewById(R.id.progress_row).setVisibility(View.GONE);

        handler.postDelayed(autoContinue, 2000L);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(autoContinue);
        super.onDestroyView();
    }
}
