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
    private ProfileFormField cep, city, state, selected;
    private View cepLoading;
    private String shownError;
    private android.widget.ImageView edit;
    private View actions, save;
    private boolean rendering;
    private final android.os.Handler debounce = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable lookup = () -> { if (model != null) model.lookupCep(); };

    public ResidentialInformationFragment() { super(R.layout.fragment_residential_information); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.profile_background);
        showLoading(true);
        model = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        LinearLayout body = view.findViewById(R.id.profile_content);
        selected = new ProfileFormField(requireContext(), R.string.profile_residence_name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        LinearLayout.LayoutParams nameParams = (LinearLayout.LayoutParams) selected.getLayoutParams();
        nameParams.bottomMargin = dp(32);
        body.addView(selected);
        cep = new ProfileFormField(requireContext(), R.string.profile_cep, InputType.TYPE_CLASS_NUMBER);
        state = new ProfileFormField(requireContext(), R.string.profile_state, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        city = new ProfileFormField(requireContext(), R.string.profile_city, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        for (ProfileFormField field : new ProfileFormField[] { selected, cep, city, state }) field.useOutlinedBox();
        selected.setEditableHighlightEnabled(true);
        cep.setEditableHighlightEnabled(true);
        pair(body, cep, state);
        body.addView(city);
        LinearLayout consulting = new LinearLayout(requireContext());
        consulting.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.ProgressBar progress = new android.widget.ProgressBar(requireContext());
        progress.setIndeterminate(true);
        consulting.addView(progress, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView label = new TextView(requireContext());
        label.setText(R.string.profile_consulting);
        label.setTextAppearance(R.style.Profile_Body);
        label.setPaddingRelative(dp(8), 0, 0, 0);
        consulting.addView(label);
        consulting.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(consulting);
        cepLoading = consulting;
        cepLoading.setVisibility(View.GONE);
        LinearLayout buttons = new LinearLayout(requireContext());
        buttons.setOrientation(LinearLayout.VERTICAL);
        save = action(buttons, R.string.profile_save, () -> {
            syncDraft();
            if (model.draft != null && model.draft.name != null && !model.draft.name.trim().isEmpty()) {
                androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
                View focused = view.findFocus();
                if (focused != null) focused.clearFocus();
            }
            model.saveResidence();
        });
        body.addView(buttons, new LinearLayout.LayoutParams(-1, -2));
        actions = buttons;
        edit = view.findViewById(R.id.profile_edit);
        edit.setOnClickListener(v -> {
            if (model.draft == null) model.editResidence();
            else { debounce.removeCallbacks(lookup); model.cancelEdit(); }
            render();
        });
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
        selected.input.addTextChangedListener(watcher);
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
                model.residenceUnavailable(value);
            }
        });
        if (water.propertyId != null) model.loadResidence(water.propertyId, false);
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
    }

    private void syncDraft() { if (model.draft != null) model.updateDraft(selected.value(), cep.value(), city.value(), state.value()); }

    private void render() {
        if (getView() == null || cep == null) return;
        rendering = true;
        ScreenState<ProfileSettingsData> loaded = model.residence.getValue();
        ScreenState<Boolean> mutation = model.saving.getValue();
        boolean ready = loaded.status == ScreenState.Status.SUCCESS;
        showLoading(loaded.status == ScreenState.Status.IDLE || loaded.busy() || mutation.busy());
        selected.setVisibility(ready ? View.VISIBLE : View.GONE);
        cep.setVisibility(ready ? View.VISIBLE : View.GONE);
        city.setVisibility(ready ? View.VISIBLE : View.GONE);
        state.setVisibility(ready ? View.VISIBLE : View.GONE);
        boolean editing = model.draft != null;
        boolean editable = editing && !mutation.busy();
        if (ready) {
            ProfileSettingsData data = loaded.data;
            selected.value(editing ? model.draft.name : data.property.name);
            cep.value(editing ? model.draft.cep : data.address.cep);
            city.value(editing ? model.draft.city : data.address.city);
            state.value(editing ? model.draft.state : data.address.state);
        } else { cep.value(""); city.value(""); state.value(""); selected.value(""); }
        selected.editable(editable);
        cep.editable(editable);
        city.editable(false);
        state.editable(false);
        edit.setEnabled(ready && !mutation.busy());
        edit.setImageResource(editing ? R.drawable.profile_edit_cancel : R.drawable.profile_edit);
        edit.setContentDescription(getString(editing ? R.string.profile_cancel_edit : R.string.profile_edit_residential));
        actions.setVisibility(editing ? View.VISIBLE : View.GONE);
        save.setEnabled(!mutation.busy() && !model.isConsultingCep());
        cepLoading.setVisibility(editing && model.isConsultingCep() ? View.VISIBLE : View.GONE);
        cep.setError(editing && !model.cepMessage.getValue().isEmpty() ? model.cepMessage.getValue() : null);
        String error = loaded.status == ScreenState.Status.ERROR ? loaded.message : mutation.status == ScreenState.Status.ERROR ? mutation.message : "";
        if (!error.isEmpty() && !error.equals(shownError)) new MaterialAlertDialogBuilder(requireContext()).setMessage(error).setPositiveButton(android.R.string.ok, null).show();
        shownError = error;
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
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2F60D3));
        button.setTextColor(0xFFFFFFFF);
        button.setCornerRadius(dp(24));
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.gravity = android.view.Gravity.END;
        params.topMargin = dp(16);
        parent.addView(button, params);
        return button;
    }

    @Override
    public void leaveProfile(Runnable action) {
        if (model.saving.getValue().busy()) return;
        if (!model.dirty()) { model.cancelEdit(); action.run(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
            .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { model.cancelEdit(); action.run(); }).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        debounce.removeCallbacksAndMessages(null);
        cep = city = state = selected = null;
        cepLoading = null;
        shownError = null;
        edit = null;
        actions = save = null;
        super.onDestroyView();
    }
}
