package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.PersonalDraft;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.ProfileFormField;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.ProfileSettingsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class PersonalInformationFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private ProfileSettingsViewModel model;
    private ProfileFormField name, email, phone, birthDate;
    private StatusNoticeView notice;
    private View edit, save, cancel, retry;
    private boolean rendering;

    public PersonalInformationFragment() { super(R.layout.fragment_personal_information); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.profile_background);
        model = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        name = new ProfileFormField(requireContext(), R.string.profile_name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        email = new ProfileFormField(requireContext(), R.string.profile_email, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        phone = new ProfileFormField(requireContext(), R.string.profile_phone, InputType.TYPE_CLASS_PHONE);
        birthDate = new ProfileFormField(requireContext(), R.string.profile_birth_date, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        body.addView(name);
        body.addView(email);
        body.addView(phone);
        body.addView(birthDate);
        notice = new StatusNoticeView(requireContext());
        body.addView(notice);
        save = action(body, R.string.profile_save, () -> {
            if (model.personalDraft == null) return;
            name.setError(model.personalDraft.error("name"));
            email.setError(model.personalDraft.error("email"));
            phone.setError(model.personalDraft.error("phone"));
            birthDate.setError(model.personalDraft.error("birthDate"));
            if (model.personalDraft.validation() == null) model.saveProfile();
        });
        cancel = action(body, R.string.profile_cancel, () -> model.cancelProfile());
        retry = action(body, R.string.profile_retry, () -> model.loadProfile(true));
        edit = view.findViewById(R.id.profile_edit);
        edit.setOnClickListener(v -> model.editProfile());
        view.findViewById(R.id.profile_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(PersonalInformationFragment.this::back); }
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (rendering || model.personalDraft == null) return;
                model.personalDraft.name = name.value();
                model.personalDraft.email = email.value();
                model.personalDraft.phone = phone.value();
                model.personalDraft.birthDate = birthDate.value();
                for (ProfileFormField field : new ProfileFormField[] { name, email, phone, birthDate }) field.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        for (ProfileFormField field : new ProfileFormField[] { name, email, phone, birthDate }) field.input.addTextChangedListener(watcher);
        model.profile.observe(getViewLifecycleOwner(), value -> render());
        model.savingProfile.observe(getViewLifecycleOwner(), value -> render());
        model.loadProfile(true);
    }

    private void render() {
        if (getView() == null || name == null) return;
        rendering = true;
        ScreenState<Profile> loaded = model.profile.getValue();
        ScreenState<Boolean> mutation = model.savingProfile.getValue();
        boolean ready = loaded.status == ScreenState.Status.SUCCESS;
        boolean editing = model.personalDraft != null;
        if (!editing) for (ProfileFormField field : new ProfileFormField[] { name, email, phone, birthDate }) field.setError(null);
        PersonalDraft values = editing ? model.personalDraft : ready ? new PersonalDraft(loaded.data) : null;
        name.value(values == null ? "" : values.name);
        email.value(values == null ? "" : values.email);
        phone.value(values == null ? "" : values.phone);
        birthDate.value(values == null ? "" : values.birthDate);
        for (ProfileFormField field : new ProfileFormField[] { name, email, phone, birthDate }) field.editable(editing && !mutation.busy());
        edit.setEnabled(ready && !editing && !mutation.busy());
        save.setVisibility(editing ? View.VISIBLE : View.GONE);
        cancel.setVisibility(editing ? View.VISIBLE : View.GONE);
        save.setEnabled(!mutation.busy());
        cancel.setEnabled(!mutation.busy());
        retry.setVisibility(loaded.status == ScreenState.Status.ERROR || (!editing && ready) ? View.VISIBLE : View.GONE);
        notice.setText(loaded.busy() ? getString(R.string.profile_loading) : loaded.status == ScreenState.Status.ERROR ? loaded.message
            : mutation.busy() ? getString(R.string.profile_personal_saving) : mutation.status == ScreenState.Status.ERROR ? mutation.message
            : mutation.status == ScreenState.Status.SUCCESS ? getString(R.string.profile_personal_saved) : "");
        rendering = false;
    }

    private View action(LinearLayout parent, int label, Runnable action) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-2, -2));
        return button;
    }

    @Override
    public void leaveProfile(Runnable action) {
        if (model.savingProfile.getValue().busy()) return;
        if (!model.personalDirty()) { model.cancelProfile(); action.run(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
            .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { model.cancelProfile(); action.run(); }).show();
    }

    @Override
    public void onDestroyView() {
        name = email = phone = birthDate = null;
        notice = null;
        edit = save = cancel = retry = null;
        super.onDestroyView();
    }
}
