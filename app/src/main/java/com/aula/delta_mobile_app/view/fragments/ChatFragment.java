package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aula.delta_mobile_app.R;

public final class ChatFragment extends Fragment {
    public ChatFragment() {
        super(R.layout.fragment_chat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        view.setAccessibilityPaneTitle(getText(R.string.chat_title));
        view.findViewById(R.id.chat_heading).setAccessibilityHeading(true);
    }
}
