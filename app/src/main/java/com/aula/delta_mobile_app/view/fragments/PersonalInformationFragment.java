package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.ProfileFormField;
import com.aula.delta_mobile_app.viewmodel.ProfileSettingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class PersonalInformationFragment extends AuthFlowFragment {
    public PersonalInformationFragment() { super(R.layout.fragment_personal_information); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.profile_background);
        ProfileSettingsViewModel model = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        ProfileFormField name = new ProfileFormField(requireContext(), R.string.profile_name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        ProfileFormField email = new ProfileFormField(requireContext(), R.string.profile_email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        ProfileFormField password = new ProfileFormField(requireContext(), R.string.profile_password, InputType.TYPE_CLASS_TEXT);
        ProfileFormField phone = new ProfileFormField(requireContext(), R.string.profile_phone, InputType.TYPE_CLASS_PHONE);
        body.addView(name);
        body.addView(email);
        password.value("••••••••");
        password.input.setTextIsSelectable(false);
        password.input.setFocusable(false);
        password.input.setContentDescription(getString(R.string.profile_protected_password));
        body.addView(password);
        MaterialButton change = new MaterialButton(requireContext(), null, android.R.attr.borderlessButtonStyle);
        change.setText(R.string.profile_change_password);
        change.setTextColor(0xFFB0B2B3);
        change.setAllCaps(false);
        change.setEnabled(false);
        body.addView(change, new LinearLayout.LayoutParams(-2, -2));
        body.addView(phone);
        TextView notice = new TextView(requireContext());
        notice.setTextAppearance(R.style.Profile_Body);
        notice.setTextColor(0xFFB0B2B3);
        notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(notice);
        MaterialButton retry = new MaterialButton(requireContext());
        retry.setText(R.string.profile_retry);
        retry.setAllCaps(false);
        retry.setOnClickListener(v -> model.loadProfile());
        body.addView(retry, new LinearLayout.LayoutParams(-2, -2));
        view.findViewById(R.id.profile_back).setOnClickListener(v -> back());
        view.findViewById(R.id.profile_edit).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_edit_personal)
            .setMessage(R.string.profile_personal_unsupported).setPositiveButton(android.R.string.ok, null).show());
        model.profile.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) { name.value(value.data.name); email.value(value.data.email); }
            else { name.value(""); email.value(""); }
            notice.setText(value.busy() ? getString(R.string.profile_loading) : value.status == ScreenState.Status.ERROR ? value.message : getString(R.string.profile_personal_unsupported));
            retry.setVisibility(value.status == ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.profile_edit).setEnabled(value.status == ScreenState.Status.SUCCESS);
        });
        model.loadProfile();
    }
}
