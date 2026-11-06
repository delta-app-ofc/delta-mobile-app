package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.MonthlyGoal;
import com.aula.delta_mobile_app.model.MonthlyGoalContext;
import com.aula.delta_mobile_app.model.ScreenState;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.aula.delta_mobile_app.viewmodel.MonthlyGoalsViewModel;
import com.aula.delta_mobile_app.viewmodel.WaterViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MonthlyGoalsFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private MonthlyGoalsViewModel model;
    private TextInputLayout amount;
    private TextInputEditText amountInput;
    private WaterViewModel water;
    private boolean rendering;
    private final Locale locale = new Locale("pt", "BR");

    public MonthlyGoalsFragment() { super(R.layout.fragment_monthly_goals); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.profile_background);
        model = new ViewModelProvider(this).get(MonthlyGoalsViewModel.class);
        water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        amount = view.findViewById(R.id.goal_amount_field);
        amountInput = view.findViewById(R.id.goal_amount_input);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (root, insets) -> {
            View actions = root.findViewById(R.id.goal_actions);
            int bottom = Math.round((insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) ? 8 : 136) * getResources().getDisplayMetrics().density);
            actions.setPadding(actions.getPaddingLeft(), actions.getPaddingTop(), actions.getPaddingRight(), bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(view);
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!rendering && model.draft != null) { model.draft = s.toString(); amount.setError(null); }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        view.findViewById(R.id.goal_back).setOnClickListener(v -> leaveProfile(this::back));
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveProfile(MonthlyGoalsFragment.this::back); }
        });
        view.findViewById(R.id.goal_edit).setOnClickListener(v -> {
            if (model.draft != null) {
                hideKeyboard(view);
                model.cancel();
                return;
            }
            model.edit();
            if (model.draft != null) {
                amountInput.requestFocus();
                androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).show(androidx.core.view.WindowInsetsCompat.Type.ime());
            }
        });
        view.findViewById(R.id.goal_save).setOnClickListener(v -> {
            hideKeyboard(view);
            if (!java.util.Objects.equals(selectedProperty(null), water.propertyId)) {
                model.cancel();
                model.load(water.propertyId, model.month);
                return;
            }
            try { MonthlyGoal.parse(amountInput.getText().toString()); }
            catch (RuntimeException invalid) { amount.setError(getString(R.string.goal_invalid)); return; }
            model.save();
        });
        view.findViewById(R.id.goal_retry).setOnClickListener(v -> leaveProfile(() -> model.load(water.propertyId, model.month)));
        view.findViewById(R.id.goal_previous).setOnClickListener(v -> changeMonth(-1));
        view.findViewById(R.id.goal_next).setOnClickListener(v -> changeMonth(1));
        model.state.observe(getViewLifecycleOwner(), value -> render(view));
        model.saving.observe(getViewLifecycleOwner(), value -> render(view));
        if (model.state.getValue().status == ScreenState.Status.IDLE || !java.util.Objects.equals(selectedProperty(null), water.propertyId)) {
            model.cancel();
            model.load(water.propertyId, model.month);
        }
        water.properties.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS && !java.util.Objects.equals(selectedProperty(null), water.propertyId)) {
                model.cancel();
                model.load(water.propertyId, model.month);
            }
        });
    }

    private void hideKeyboard(View view) {
        amountInput.clearFocus();
        view.requestFocus();
        androidx.core.view.WindowCompat.getInsetsController(requireActivity().getWindow(), view).hide(androidx.core.view.WindowInsetsCompat.Type.ime());
    }

    private Integer selectedProperty(Integer fallback) {
        MonthlyGoalContext context = model.state.getValue().data;
        return context == null ? fallback : context.property.propertyId;
    }

    private void changeMonth(int offset) {
        leaveProfile(() -> model.load(water.propertyId, model.month.plusMonths(offset)));
    }

    private void render(View view) {
        if (amount == null || getView() != view) return;
        rendering = true;
        ScreenState<MonthlyGoalContext> loaded = model.state.getValue();
        ScreenState<Boolean> saving = model.saving.getValue();
        boolean ready = loaded.status == ScreenState.Status.SUCCESS;
        YearMonth currentMonth = YearMonth.now();
        boolean past = model.month.isBefore(currentMonth);
        boolean future = model.month.isAfter(currentMonth);
        boolean editing = model.draft != null && !past;
        boolean busy = loaded.busy() || saving.busy();
        if (!editing && amountInput.hasFocus()) hideKeyboard(view);
        View content = view.findViewById(R.id.goal_content);
        content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), Math.round((editing ? 16 : 136) * getResources().getDisplayMetrics().density));
        view.findViewById(R.id.goal_actions).setVisibility(editing ? View.VISIBLE : View.GONE);
        showLoading(loaded.status == ScreenState.Status.IDLE || busy);
        MonthlyGoalContext context = loaded.data;
        String monthLabel = model.month.format(DateTimeFormatter.ofPattern("MMMM uuuu", locale));
        ((TextView) view.findViewById(R.id.goal_month)).setText(monthLabel.substring(0, 1).toUpperCase(locale) + monthLabel.substring(1));
        ((TextView) view.findViewById(R.id.goal_property)).setText(!ready ? ""
            : context.property.propertyName == null ? getString(R.string.goal_property, context.property.propertyId) : context.property.propertyName);
        amount.setVisibility(editing ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.goal_fields).setVisibility(editing ? View.VISIBLE : View.GONE);
        String input = editing ? model.draft : "";
        if (!amountInput.getText().toString().equals(input)) amountInput.setText(input);
        amountInput.setEnabled(editing && !busy);
        if (!editing) amount.setError(null);
        TextView confirmed = view.findViewById(R.id.goal_confirmed);
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(2);
        confirmed.setText(!ready || context.liters == null ? past ? getString(R.string.goal_past_not_set, model.month.format(DateTimeFormatter.ofPattern("MMMM", locale)))
            : getString(R.string.goal_not_set) : getString(R.string.home_liters, format.format(context.liters)));
        confirmed.setVisibility(ready && !editing ? View.VISIBLE : View.GONE);
        TextView hint = view.findViewById(R.id.goal_limit_hint);
        hint.setText(future && ready ? context.currentMonthLiters == null ? getString(R.string.goal_current_not_set)
            : getString(R.string.goal_current_reference, format.format(context.currentMonthLiters)) : getString(R.string.goal_limit_hint));
        hint.setVisibility(past || !ready ? View.GONE : View.VISIBLE);
        ImageView edit = view.findViewById(R.id.goal_edit);
        edit.setVisibility(ready && !past ? View.VISIBLE : View.GONE);
        edit.setEnabled(!busy);
        edit.setImageResource(editing ? R.drawable.profile_edit_cancel : R.drawable.profile_edit);
        edit.setContentDescription(getString(editing ? R.string.profile_cancel_edit : R.string.goal_edit));
        view.findViewById(R.id.goal_save).setVisibility(editing ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.goal_save).setEnabled(!busy);
        view.findViewById(R.id.goal_previous).setEnabled(!busy);
        view.findViewById(R.id.goal_next).setEnabled(!busy);
        view.findViewById(R.id.goal_retry).setVisibility(loaded.status == ScreenState.Status.ERROR || saving.status == ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
        ((TextView) view.findViewById(R.id.goal_notice)).setText(loaded.busy() ? getString(R.string.goal_loading) : loaded.status == ScreenState.Status.ERROR ? loaded.message
            : saving.busy() ? getString(R.string.goal_saving) : saving.status == ScreenState.Status.ERROR ? saving.message
            : "");
        rendering = false;
    }

    @Override
    public void leaveProfile(Runnable action) {
        if (model.saving.getValue().busy()) return;
        if (!model.dirty()) { hideKeyboard(requireView()); model.cancel(); action.run(); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.profile_discard_title).setMessage(R.string.profile_discard_message)
            .setNegativeButton(R.string.profile_keep_editing, null).setPositiveButton(R.string.profile_discard, (dialog, which) -> { hideKeyboard(requireView()); model.cancel(); action.run(); }).show();
    }

    @Override public void onDestroyView() { amount = null; amountInput = null; super.onDestroyView(); }
}
