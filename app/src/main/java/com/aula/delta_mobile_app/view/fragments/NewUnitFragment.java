package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.view.ProfileFormField;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Arrays;

public final class NewUnitFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private UnitsViewModel model;
    private ProfileFormField name, cep, city, state, type, category;
    private boolean rendering;
    private final android.os.Handler debounce = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable lookup = () -> { if (model != null) model.lookup(); };

    public NewUnitFragment() { super(R.layout.fragment_unit_create); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.profile_background);
        model = new ViewModelProvider(requireActivity()).get(UnitsViewModel.class);
        LinearLayout fields = view.findViewById(R.id.unit_fields);
        name = field(R.string.profile_residence_name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        cep = field(R.string.profile_cep, InputType.TYPE_CLASS_NUMBER);
        city = field(R.string.profile_city, InputType.TYPE_CLASS_TEXT);
        state = field(R.string.profile_state, InputType.TYPE_CLASS_TEXT);
        type = field(R.string.units_type, InputType.TYPE_CLASS_TEXT);
        category = field(R.string.units_category, InputType.TYPE_CLASS_TEXT);
        name.input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(100) });
        cep.input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(8) });
        for (ProfileFormField field : new ProfileFormField[] { name, type, category, cep, city, state }) fields.addView(field);
        name.setEditableHighlightEnabled(true);
        cep.setEditableHighlightEnabled(true);
        selector(type, true);
        selector(category, false);
        view.findViewById(R.id.units_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(NewUnitFragment.this::back); }
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!rendering) model.update(name.value(), cep.value());
            }
            @Override public void afterTextChanged(Editable value) { }
        };
        name.input.addTextChangedListener(watcher);
        cep.input.addTextChangedListener(watcher);
        cep.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (rendering) return;
                debounce.removeCallbacks(lookup);
                if (s.toString().matches("[0-9]{8}")) debounce.postDelayed(lookup, 600);
            }
            @Override public void afterTextChanged(Editable value) { }
        });
        view.findViewById(R.id.unit_continue).setOnClickListener(v -> {
            androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            View focused = view.findFocus();
            if (focused != null) focused.clearFocus();
            model.update(name.value(), cep.value());
            model.create();
        });
        model.changes.observe(getViewLifecycleOwner(), value -> render(view));
        model.postalState.observe(getViewLifecycleOwner(), value -> render(view));
        model.creation.observe(getViewLifecycleOwner(), value -> {
            render(view);
            if (value.status != ScreenState.Status.SUCCESS) return;
            int propertyId = value.data.id;
            WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
            water.consumeMeter();
            water.select(propertyId);
            water.loadProperties();
            model.consumeCreation();
            Bundle args = new Bundle();
            args.putBoolean("unitFlow", true);
            args.putInt("unitPropertyId", propertyId);
            NavHostFragment.findNavController(this).navigate(R.id.qrScanFragment, args, new NavOptions.Builder().setPopUpTo(R.id.newUnitFragment, true).build());
        });
        if (model.draft.cep.matches("[0-9]{8}") && !model.draft.cep.equals(model.draft.confirmedCep) && !model.postalState.getValue().busy()) model.lookup();
    }

    private ProfileFormField field(int label, int inputType) {
        ProfileFormField result = new ProfileFormField(requireContext(), label, inputType);
        result.useOutlinedBox();
        return result;
    }

    private void selector(ProfileFormField field, boolean chooseType) {
        field.input.setTextIsSelectable(false);
        field.input.setFocusable(true);
        field.input.setFocusableInTouchMode(true);
        field.input.setOnClickListener(v -> choose(chooseType));
        field.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
        field.setEndIconDrawable(android.R.drawable.arrow_down_float);
        field.setEndIconContentDescription(R.string.units_choose);
        field.setEndIconOnClickListener(v -> choose(chooseType));
    }

    private void choose(boolean chooseType) {
        if (model.busy()) return;
        String[] values = chooseType ? UnitDraft.TYPES : UnitDraft.CATEGORIES;
        String[] labels = getResources().getStringArray(chooseType ? R.array.units_types : R.array.units_categories);
        String current = chooseType ? model.draft.type : model.draft.classification;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(chooseType ? R.string.units_type : R.string.units_category)
            .setSingleChoiceItems(labels, Arrays.asList(values).indexOf(current), (dialog, index) -> {
                if (chooseType) model.draft.type = values[index]; else model.draft.classification = values[index];
                model.changed();
                dialog.dismiss();
            }).setNegativeButton(R.string.profile_cancel, null).show();
    }

    private void render(View view) {
        if (getView() != view || name == null) return;
        rendering = true;
        boolean busy = model.creation.getValue().busy();
        showLoading(busy);
        UnitDraft draft = model.draft;
        name.value(draft.name); cep.value(draft.cep); city.value(draft.city); state.value(draft.state);
        name.editable(!busy); cep.editable(!busy && !model.addressLocked());
        int typeIndex = Arrays.asList(UnitDraft.TYPES).indexOf(draft.type), categoryIndex = Arrays.asList(UnitDraft.CATEGORIES).indexOf(draft.classification);
        type.value(typeIndex < 0 ? "" : getResources().getStringArray(R.array.units_types)[typeIndex]);
        category.value(categoryIndex < 0 ? "" : getResources().getStringArray(R.array.units_categories)[categoryIndex]);
        type.input.setHint(typeIndex < 0 ? getString(R.string.units_choose) : null);
        category.input.setHint(categoryIndex < 0 ? getString(R.string.units_choose) : null);
        type.input.setEnabled(!busy); category.input.setEnabled(!busy);
        view.findViewById(R.id.unit_consulting).setVisibility(model.postalState.getValue().busy() ? View.VISIBLE : View.GONE);
        cep.setError(model.postalState.getValue().status == ScreenState.Status.ERROR ? model.postalState.getValue().message : null);
        ((TextView) view.findViewById(R.id.units_notice)).setText(model.creation.getValue().status == ScreenState.Status.ERROR ? model.creation.getValue().message : "");
        view.findViewById(R.id.unit_continue).setEnabled(!busy && !model.postalState.getValue().busy());
        rendering = false;
    }

    @Override public void leaveProfile(Runnable action) {
        if (model.busy()) return;
        if (model.draft.name.isEmpty() && model.draft.cep.isEmpty() && model.draft.type.isEmpty() && model.draft.classification.isEmpty()) { action.run(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
            .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { model.beginNew(); action.run(); }).show();
    }

    @Override public void onDestroyView() { debounce.removeCallbacksAndMessages(null); name = cep = city = state = type = category = null; model = null; super.onDestroyView(); }
}
