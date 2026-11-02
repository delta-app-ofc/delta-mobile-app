package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ProfileSettingsData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.ProfileFormField;
import com.aula.delta_mobile_app.viewmodel.ProfileSettingsViewModel;
import com.aula.delta_mobile_app.viewmodel.WaterViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ResidentialInformationFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private ProfileSettingsViewModel model;
    private ProfileFormField cep, city, state;
    private TextView notice, selected;
    private View edit, actions, save, cancel, retry;
    private boolean rendering;
    private final android.os.Handler debounce = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable lookup = () -> { if (model != null) model.lookupCep(); };

    public ResidentialInformationFragment() { super(R.layout.fragment_residential_information); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.profile_background);
        model = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        selected = new TextView(requireContext());
        selected.setTextAppearance(R.style.Profile_Body);
        selected.setPadding(0, 0, 0, dp(16));
        body.addView(selected);
        cep = new ProfileFormField(requireContext(), R.string.profile_cep, InputType.TYPE_CLASS_NUMBER);
        state = new ProfileFormField(requireContext(), R.string.profile_state, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        city = new ProfileFormField(requireContext(), R.string.profile_city, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        pair(body, cep, state);
        body.addView(city);
        notice = new com.aula.delta_mobile_app.view.StatusNoticeView(requireContext());
        notice.setTextAppearance(R.style.Profile_Body);
        notice.setTextColor(0xFFB0B2B3);
        notice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(notice);
        LinearLayout buttons = new LinearLayout(requireContext());
        buttons.setOrientation(LinearLayout.VERTICAL);
        save = action(buttons, R.string.profile_save, () -> { syncDraft(); model.saveResidence(); });
        cancel = action(buttons, R.string.profile_cancel, () -> { model.cancelEdit(); render(); });
        body.addView(buttons);
        actions = buttons;
        retry = action(body, R.string.profile_retry, () -> leaveProfile(() -> model.loadResidence(water.propertyId, true)));
        edit = view.findViewById(R.id.profile_edit);
        edit.setOnClickListener(v -> { model.editResidence(); render(); });
        view.findViewById(R.id.profile_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(ResidentialInformationFragment.this::back); }
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!rendering) syncDraft();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        cep.input.addTextChangedListener(watcher);
        city.input.addTextChangedListener(watcher);
        state.input.addTextChangedListener(watcher);
        cep.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (rendering) return;
                debounce.removeCallbacks(lookup);
                if (s.toString().matches("[0-9]{8}")) debounce.postDelayed(lookup, 600);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        model.residence.observe(getViewLifecycleOwner(), value -> render());
        model.saving.observe(getViewLifecycleOwner(), value -> render());
        model.cepMessage.observe(getViewLifecycleOwner(), value -> render());
        water.properties.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                if (water.propertyId == null && value.data != null) {
                    for (com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse link : value.data) {
                        if (link != null && link.propertyId != null) { water.propertyId = link.propertyId; break; }
                    }
                }
                model.loadResidence(water.propertyId, false);
            } else if (value.status == ScreenState.Status.ERROR) {
                notice.setText(value.message);
                retry.setVisibility(View.VISIBLE);
            } else if (value.busy() && water.propertyId == null) notice.setText(R.string.profile_loading);
        });
        retry.setOnClickListener(v -> leaveProfile(() -> {
            if (water.properties.getValue().status != ScreenState.Status.SUCCESS) water.loadProperties();
            else model.loadResidence(water.propertyId, true);
        }));
        if (water.propertyId != null) model.loadResidence(water.propertyId, false);
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
    }

    private void syncDraft() { if (model.draft != null) model.updateDraft(cep.value(), city.value(), state.value()); }

    private void render() {
        if (getView() == null || cep == null) return;
        rendering = true;
        ScreenState<ProfileSettingsData> loaded = model.residence.getValue();
        ScreenState<Boolean> mutation = model.saving.getValue();
        boolean ready = loaded.status == ScreenState.Status.SUCCESS;
        boolean editing = model.draft != null;
        boolean editable = editing && !mutation.busy();
        if (ready) {
            ProfileSettingsData data = loaded.data;
            selected.setText(data.property.name);
            cep.value(editing ? model.draft.cep : data.address.cep);
            city.value(editing ? model.draft.city : data.address.city);
            state.value(editing ? model.draft.state : data.address.state);
        } else { cep.value(""); city.value(""); state.value(""); selected.setText(""); }
        cep.editable(editable);
        city.editable(editable);
        state.editable(editable);
        edit.setEnabled(ready && !mutation.busy() && !editing);
        actions.setVisibility(editing ? View.VISIBLE : View.GONE);
        save.setEnabled(!mutation.busy());
        cancel.setEnabled(!mutation.busy());
        retry.setVisibility(loaded.status == ScreenState.Status.ERROR || mutation.status == ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
        String text = loaded.busy() ? getString(R.string.profile_loading) : loaded.status == ScreenState.Status.ERROR ? loaded.message
            : mutation.busy() ? getString(R.string.profile_saving) : mutation.status == ScreenState.Status.ERROR ? mutation.message
            : mutation.status == ScreenState.Status.SUCCESS ? getString(R.string.profile_address_saved) : getString(R.string.profile_address_partial);
        if (editing && !mutation.busy() && mutation.status != ScreenState.Status.ERROR && !model.cepMessage.getValue().isEmpty()) text += "\n" + model.cepMessage.getValue();
        notice.setText(text);
        rendering = false;
    }

    private void pair(LinearLayout body, ProfileFormField first, ProfileFormField second) {
        boolean stacked = getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.25f;
        if (stacked) { body.addView(first); body.addView(second); return; }
        LinearLayout row = new LinearLayout(requireContext());
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 2.5f);
        left.setMarginEnd(dp(20));
        left.bottomMargin = dp(24);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
        right.bottomMargin = dp(24);
        row.addView(first, left);
        row.addView(second, right);
        body.addView(row);
    }

    private View action(LinearLayout parent, int text, Runnable action) {
        MaterialButton button = new MaterialButton(requireContext(), null, android.R.attr.borderlessButtonStyle);
        button.setText(text);
        button.setTextColor(0xFFB5CAF6);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(-2, -2));
        return button;
    }

    @Override
    public void leaveProfile(Runnable action) {
        if (model.saving.getValue().busy()) { action.run(); return; }
        if (!model.dirty()) { model.cancelEdit(); action.run(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
            .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { model.cancelEdit(); action.run(); }).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        debounce.removeCallbacksAndMessages(null);
        cep = city = state = null;
        notice = selected = null;
        edit = actions = save = cancel = retry = null;
        super.onDestroyView();
    }
}
