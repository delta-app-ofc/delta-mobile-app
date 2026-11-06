package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.LinearLayout;
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
        cep.input.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789-"));
        cep.input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        cep.input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(9) });
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
        cep.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable value) {
                if (rendering) return;
                int cursor = Math.max(0, cep.input.getSelectionStart());
                int digitsBeforeCursor = value.subSequence(0, Math.min(cursor, value.length())).toString().replaceAll("\\D", "").length();
                String digits = value.toString().replaceAll("\\D", "");
                model.update(name.value(), digits.substring(0, Math.min(8, digits.length())));
                cep.input.setSelection(Math.min(cep.value().length(), digitsBeforeCursor + (digitsBeforeCursor > 5 ? 1 : 0)));
                debounce.removeCallbacks(lookup);
                if (model.draft.cep.matches("[0-9]{8}") && !model.draft.cep.equals(model.draft.confirmedCep)) debounce.postDelayed(lookup, 600);
            }
        });
        view.findViewById(R.id.unit_continue).setOnClickListener(v -> {
            androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            View focused = view.findFocus();
            if (focused != null) focused.clearFocus();
            model.update(name.value(), cep.value());
            name.setError(null); type.setError(null); category.setError(null);
            String error = model.draft.validation();
            if (error != null) {
                UnitDraft draft = model.draft;
                if (draft.name.trim().isEmpty() || draft.name.trim().length() > 100) name.setError(error);
                else if (!draft.cep.matches("[0-9]{8}") || !draft.cep.equals(draft.confirmedCep) || draft.city.trim().isEmpty() || draft.state.trim().isEmpty()
                    || !draft.hasCoordinates()) cep.setError(error);
                else if (!Arrays.asList(UnitDraft.TYPES).contains(draft.type)) type.setError(error);
                else category.setError(error);
                return;
            }
            model.create();
        });
        model.changes.observe(getViewLifecycleOwner(), value -> render(view));
        model.postalState.observe(getViewLifecycleOwner(), value -> render(view));
        model.creation.observe(getViewLifecycleOwner(), value -> {
            render(view);
            if (value.status == ScreenState.Status.ERROR) {
                new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.units_save_error).setMessage(value.message)
                    .setPositiveButton(android.R.string.ok, null).show();
            }
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
        if (model.draft.cep.matches("[0-9]{8}") && (!model.draft.cep.equals(model.draft.confirmedCep) || !model.draft.hasCoordinates())
            && !model.postalState.getValue().busy()) model.lookup();
    }

    private ProfileFormField field(int label, int inputType) {
        ProfileFormField result = new ProfileFormField(requireContext(), label, inputType);
        result.useOutlinedBox();
        return result;
    }

    private void selector(ProfileFormField field, boolean chooseType) {
        field.input.setTextIsSelectable(false);
        field.input.setFocusable(false);
        field.input.setFocusableInTouchMode(false);
        field.input.setCursorVisible(false);
        field.input.setShowSoftInputOnFocus(false);
        field.input.setOnClickListener(v -> choose(chooseType));
        field.setOnClickListener(v -> choose(chooseType));
        field.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM);
        field.setEndIconDrawable(android.R.drawable.arrow_down_float);
        field.setEndIconContentDescription(R.string.units_choose);
        field.setEndIconOnClickListener(v -> choose(chooseType));
    }

    private void choose(boolean chooseType) {
        if (model.busy()) return;
        View view = requireView();
        androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
        View focused = view.findFocus();
        if (focused != null) focused.clearFocus();
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
        name.editable(!busy); cep.editable(!busy && !model.addressLocked());
        if (!busy && !model.addressLocked()) {
            cep.input.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789-"));
            cep.input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        name.value(draft.name); cep.value(draft.cep.length() > 5 ? draft.cep.substring(0, 5) + "-" + draft.cep.substring(5) : draft.cep); city.value(draft.city); state.value(draft.state);
        int typeIndex = Arrays.asList(UnitDraft.TYPES).indexOf(draft.type), categoryIndex = Arrays.asList(UnitDraft.CATEGORIES).indexOf(draft.classification);
        type.value(typeIndex < 0 ? "" : getResources().getStringArray(R.array.units_types)[typeIndex]);
        category.value(categoryIndex < 0 ? "" : getResources().getStringArray(R.array.units_categories)[categoryIndex]);
        type.input.setHint(typeIndex < 0 ? getString(R.string.units_choose) : null);
        category.input.setHint(categoryIndex < 0 ? getString(R.string.units_choose) : null);
        type.input.setEnabled(!busy); category.input.setEnabled(!busy);
        type.setEnabled(!busy); category.setEnabled(!busy);
        view.findViewById(R.id.unit_consulting).setVisibility(model.postalState.getValue().busy() ? View.VISIBLE : View.GONE);
        cep.setError(model.postalState.getValue().status == ScreenState.Status.ERROR ? model.postalState.getValue().message : null);
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
