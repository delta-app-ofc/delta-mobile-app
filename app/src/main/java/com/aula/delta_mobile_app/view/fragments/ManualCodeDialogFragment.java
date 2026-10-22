package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.aula.delta_mobile_app.R;

public class ManualCodeDialogFragment extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_manual_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText code = view.findViewById(R.id.manual_code);
        view.findViewById(R.id.manual_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.manual_confirm).setOnClickListener(v -> {
            String value = code.getText().toString().trim();
            if (value.isEmpty()) {
                code.setError(getString(R.string.auth_code_required));
                code.requestFocus();
                return;
            }
            Bundle result = new Bundle();
            result.putString("code", value);
            getParentFragmentManager().setFragmentResult("manual_code", result);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(metrics.widthPixels - Math.round(32 * metrics.density), Math.round(360 * metrics.density)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
