package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import com.aula.delta_mobile_app.R;

public final class InformationDialogFragment extends DialogFragment {
    public static void show(FragmentManager manager, String title, String message) {
        if (manager.isStateSaved() || manager.findFragmentByTag("information") != null) return;
        InformationDialogFragment dialog = new InformationDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        dialog.setArguments(args);
        dialog.show(manager, "information");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.dialog_information, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        ((TextView) view.findViewById(R.id.information_title)).setText(requireArguments().getString("title"));
        ((TextView) view.findViewById(R.id.information_message)).setText(requireArguments().getString("message"));
        view.findViewById(R.id.information_close).setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) return;
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int available = requireActivity().getWindow().getDecorView().getWidth();
        if (available <= 0) available = metrics.widthPixels;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setLayout(Math.min(available - Math.round(32 * metrics.density), Math.round(360 * metrics.density)), ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
