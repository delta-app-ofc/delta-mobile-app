package com.aula.delta_mobile_app.view.fragments;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.RoutineFormValidator;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.RoutineAgendaAdapter;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class RoutineEditorDialogFragment extends DialogFragment {
    private static final String TIME_PICKER_PREFIX = "routine_time_";

    private RoutineViewModel model;
    private MaterialAutoCompleteTextView type;
    private LinearLayout schedule;
    private NestedScrollView scroll;
    private AlertDialog confirmation;
    private AlertDialog resendConfirmation;
    private Runnable retryUpdate;
    private ScreenState.Status lastCreationStatus;
    private boolean lastChecked;
    private final Map<DayOfWeek, Chip> days = new EnumMap<>(DayOfWeek.class);
    private final Map<DayOfWeek, View> dayCards = new EnumMap<>(DayOfWeek.class);
    private long token;
    private boolean binding, destroyingView;

    public RoutineEditorDialogFragment() {
        super(R.layout.dialog_routine_editor);
    }

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setStyle(STYLE_NO_TITLE, R.style.Routine_EditorDialog);
        setCancelable(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        destroyingView = false;
        model = new ViewModelProvider(requireParentFragment()).get(RoutineViewModel.class);
        RoutineFormState form = model.state().getValue().form;
        if (form == null) {
            dismissAllowingStateLoss();
            return;
        }
        token = form.token;
        type = view.findViewById(R.id.routine_form_type);
        type.setSimpleItems(R.array.routine_form_types);
        type.setOnItemClickListener((parent, item, position, id) -> {
            if (!binding && current() != null) model.setFormHabit(position + 1);
        });
        ChipGroup group = view.findViewById(R.id.routine_form_days);
        for (DayOfWeek day : DayOfWeek.values()) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(day.getDisplayName(TextStyle.SHORT, locale()).toUpperCase(locale()));
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (!binding && current() != null) model.selectFormDayDerived(day, checked);
            });
            group.addView(chip);
            days.put(day, chip);
        }
        scroll = view.findViewById(R.id.routine_form_scroll);
        schedule = view.findViewById(R.id.routine_form_schedule);
        view.findViewById(R.id.routine_form_close).setOnClickListener(button -> model.requestFormClose(token));
        view.findViewById(R.id.routine_form_save).setOnClickListener(button -> {
            model.saveForm();
            RoutineFormState current = current();
            if (current != null && !current.errors.isEmpty()) view.post(() -> focusError(current.errors.keySet().iterator().next()));
        });
        Dialog dialog = requireDialog();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog instanceof ComponentDialog)
            ((ComponentDialog) dialog).getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    model.requestFormClose(token);
                }
            });
        ViewCompat.setOnApplyWindowInsetsListener(view, (root, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int bottom = Math.max(bars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            root.setPadding(bars.left, bars.top, bars.right, bottom);
            return insets;
        });
        model.state().observe(getViewLifecycleOwner(), state -> render(state.form));
        render(form);
        restoreTimePicker();
    }

    private RoutineFormState current() {
        if (destroyingView || getView() == null || model == null) return null;
        RoutineFormState form = model.state().getValue().form;
        return form != null && form.token == token ? form : null;
    }

    private Locale locale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    private void render(RoutineFormState form) {
        if (form == null || form.token != token) {
            dismissAllowingStateLoss();
            return;
        }
        RoutineUiState state = model.state().getValue();
        boolean busy = state.creation.busy();
        long wait = Math.max(0, state.creation.retryAt - System.currentTimeMillis());
        View root = requireView();
        if (retryUpdate != null) root.removeCallbacks(retryUpdate);
        retryUpdate = null;
        if (wait > 0) {
            retryUpdate = () -> { if (current() != null) render(current()); };
            root.postDelayed(retryUpdate, wait);
        }
        Button save = root.findViewById(R.id.routine_form_save);
        save.setText(form.mode == RoutineFormState.Mode.EDIT ? R.string.routine_form_update : R.string.routine_form_save);
        save.setEnabled(!busy && wait == 0 && !state.reading.busy());
        root.findViewById(R.id.routine_form_close).setEnabled(!busy);
        bindHeader(form, busy);
        bindSchedule(form, busy);
        bindSummary(form);
        bindNotice(form, state);
        if (state.confirmResend && (resendConfirmation == null || !resendConfirmation.isShowing())) {
            resendConfirmation = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.routine_form_resend_title)
                .setMessage(R.string.routine_form_resend_warning)
                .setPositiveButton(R.string.routine_form_resend, (dialog, which) -> model.confirmResend())
                .setNegativeButton(R.string.routine_form_cancel, (dialog, which) -> model.cancelResend())
                .setOnCancelListener(dialog -> model.cancelResend()).show();
        } else if (!state.confirmResend && resendConfirmation != null) {
            resendConfirmation.dismiss();
            resendConfirmation = null;
        }
        if ((state.creation.busy() || state.creation.status == ScreenState.Status.ERROR)
            && (lastCreationStatus != state.creation.status || lastChecked != state.checkedCreation)) scroll.smoothScrollTo(0, 0);
        lastCreationStatus = state.creation.status;
        lastChecked = state.checkedCreation;
        if (form.confirmDiscard && (confirmation == null || !confirmation.isShowing())) {
            confirmation = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.routine_form_discard_title).setMessage(R.string.routine_form_discard_message)
                .setPositiveButton(R.string.routine_form_discard, (dialog, which) -> model.discardForm(token))
                .setNegativeButton(R.string.routine_form_continue, (dialog, which) -> model.cancelFormDiscard(token))
                .setOnCancelListener(dialog -> model.cancelFormDiscard(token)).show();
        } else if (!form.confirmDiscard && confirmation != null) {
            confirmation.dismiss();
            confirmation = null;
        }
    }

    private void bindHeader(RoutineFormState form, boolean busy) {
        binding = true;
        View root = requireView();
        ((TextView) root.findViewById(R.id.routine_form_title)).setText(form.mode == RoutineFormState.Mode.CREATE ? R.string.routine_form_create : R.string.routine_form_edit);
        TextView help = root.findViewById(R.id.routine_form_effective_from);
        boolean editing = form.mode == RoutineFormState.Mode.EDIT && form.effectiveFrom != null;
        help.setVisibility(editing ? View.VISIBLE : View.GONE);
        if (editing) help.setText(getString(R.string.routine_form_edit_context, getString(R.string.routine_form_edit_help),
            getString(R.string.routine_form_effective_from, form.effectiveFrom.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale())))));
        String[] labels = getResources().getStringArray(R.array.routine_form_types);
        String label = form.habitId == null ? "" : form.habitId >= 1 && form.habitId <= labels.length ? labels[form.habitId - 1] : getString(R.string.routine_form_unknown_type);
        if (!type.getText().toString().equals(label)) type.setText(label, false);
        type.setEnabled(!busy);
        ((TextInputLayout) root.findViewById(R.id.routine_form_type_layout)).setError(error(form.errors.get(RoutineFormState.TYPE)));
        for (DayOfWeek day : DayOfWeek.values()) {
            days.get(day).setChecked(form.days.get(day).selected);
            days.get(day).setEnabled(!busy);
        }
        groupError(R.id.routine_form_days_error, form.errors.containsKey(RoutineFormState.DAYS) ? getString(R.string.routine_form_days_required) : "");
        groupError(R.id.routine_form_distribution_error, error(form.errors.get(RoutineFormState.DISTRIBUTION)));
        binding = false;
    }

    private void bindSchedule(RoutineFormState form, boolean busy) {
        schedule.removeAllViews();
        dayCards.clear();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DayOfWeek day : DayOfWeek.values()) {
            RoutineFormState.DayDraft draft = form.days.get(day);
            if (!draft.selected) continue;
            View card = inflater.inflate(R.layout.item_routine_schedule_day, schedule, false);
            ((TextView) card.findViewById(R.id.routine_schedule_day_title)).setText(day.getDisplayName(TextStyle.FULL, locale()));
            ChipGroup times = card.findViewById(R.id.routine_schedule_times);
            for (int i = 0; i < draft.times.size(); i++) {
                int index = i;
                Chip chip = new Chip(requireContext());
                chip.setText(draft.times.get(i));
                chip.setCheckable(false);
                chip.setCloseIconVisible(true);
                chip.setEnsureMinTouchTargetSize(true);
                chip.setEnabled(!busy);
                chip.setOnClickListener(view -> openTimePicker(day, index));
                chip.setOnCloseIconClickListener(view -> model.removeFormTime(day, index));
                times.addView(chip);
            }
            TextView error = card.findViewById(R.id.routine_schedule_error);
            String message = dayError(form, day);
            error.setText(message);
            error.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
            View add = card.findViewById(R.id.routine_schedule_add_time);
            add.setEnabled(!busy);
            add.setOnClickListener(view -> openTimePicker(day, -1));
            schedule.addView(card);
            dayCards.put(day, card);
        }
    }

    private String dayError(RoutineFormState form, DayOfWeek day) {
        RoutineFormValidator.Error quantity = form.errors.get(RoutineFormState.quantityKey(day));
        if (quantity != null) return quantity == RoutineFormValidator.Error.TOO_MANY_TIMES ? error(quantity) : getString(R.string.routine_form_day_needs_time);
        RoutineFormState.DayDraft draft = form.days.get(day);
        for (int i = 0; i < draft.times.size(); i++) {
            RoutineFormValidator.Error time = form.errors.get(RoutineFormState.timeKey(day, i));
            if (time != null) return error(time);
        }
        return "";
    }

    private void bindSummary(RoutineFormState form) {
        int count = form.executionCount();
        BigDecimal perExecution = form.habitId == null ? null : RoutineData.estimateForHabit(form.habitId);
        BigDecimal estimate = perExecution == null ? null : perExecution.multiply(BigDecimal.valueOf(count));
        ((TextView) requireView().findViewById(R.id.routine_form_summary)).setText(getResources().getQuantityString(R.plurals.routine_form_summary,
            count, count, RoutineAgendaAdapter.liters(requireContext(), estimate)));
    }

    private void bindNotice(RoutineFormState form, RoutineUiState state) {
        String notice = form.legacy ? getString(R.string.routine_form_legacy) : "";
        if (state.creation.busy()) notice = getString(R.string.routine_form_sending);
        else if (state.creation.status == ScreenState.Status.ERROR) {
            notice = state.creation.message;
            if (state.creation.retryAt > System.currentTimeMillis()) notice += "\n" + getString(R.string.routine_form_wait,
                Instant.ofEpochMilli(state.creation.retryAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale())));
        }
        ((StatusNoticeView) requireView().findViewById(R.id.routine_form_notice)).setText(notice);
    }

    private void openTimePicker(DayOfWeek day, int index) {
        if (getChildFragmentManager().isStateSaved() || current() == null) return;
        RoutineFormState.DayDraft draft = current().days.get(day);
        LocalTime initial = index >= 0 && index < draft.times.size() ? parseTime(draft.times.get(index)) : LocalTime.now();
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(initial.getHour()).setMinute(initial.getMinute())
            .setTitleText(R.string.routine_form_add_time).build();
        bindTimePicker(picker, day, index);
        picker.showNow(getChildFragmentManager(), timePickerTag(day, index));
    }

    private void restoreTimePicker() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (!(fragment instanceof MaterialTimePicker) || fragment.getTag() == null || !fragment.getTag().startsWith(TIME_PICKER_PREFIX)) continue;
            String[] values = fragment.getTag().substring(TIME_PICKER_PREFIX.length()).split("_");
            if (values.length == 2) bindTimePicker((MaterialTimePicker) fragment, DayOfWeek.of(Integer.parseInt(values[0])), Integer.parseInt(values[1]));
        }
    }

    private void bindTimePicker(MaterialTimePicker picker, DayOfWeek day, int index) {
        picker.addOnPositiveButtonClickListener(view -> model.putFormTime(day, index, String.format(Locale.ROOT, "%02d:%02d", picker.getHour(), picker.getMinute())));
    }

    private String timePickerTag(DayOfWeek day, int index) {
        return TIME_PICKER_PREFIX + day.getValue() + "_" + index;
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (RuntimeException invalid) {
            return LocalTime.now();
        }
    }

    private void focusError(String key) {
        View target;
        if (RoutineFormState.TYPE.equals(key)) target = type;
        else if (RoutineFormState.DAYS.equals(key)) target = days.get(DayOfWeek.MONDAY);
        else {
            DayOfWeek day = dayFromKey(key);
            View card = day == null ? null : dayCards.get(day);
            target = card == null ? null : card.findViewById(R.id.routine_schedule_add_time);
        }
        if (target == null) return;
        target.requestFocus();
        target.requestRectangleOnScreen(new Rect(0, 0, target.getWidth(), target.getHeight()), true);
        scroll.smoothScrollTo(0, Math.max(0, target.getTop() - getResources().getDimensionPixelSize(R.dimen.routine_item_spacing)));
    }

    private DayOfWeek dayFromKey(String key) {
        String[] values = key.split(":");
        if (values.length < 2) return null;
        try {
            return DayOfWeek.of(Integer.parseInt(values[1]));
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private void groupError(int id, String message) {
        TextView view = requireView().findViewById(id);
        view.setText(message);
        view.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String error(RoutineFormValidator.Error error) {
        if (error == null) return null;
        switch (error) {
            case REQUIRED: return getString(R.string.routine_form_required);
            case INVALID_TYPE: return getString(R.string.routine_form_invalid_type);
            case INVALID_NUMBER: return getString(R.string.routine_form_invalid_number);
            case POSITIVE: return getString(R.string.routine_form_positive);
            case TOO_MANY_TIMES: return getString(R.string.routine_form_too_many_times);
            case INVALID_TIME: return getString(R.string.routine_form_invalid_time);
            case DUPLICATE_TIME: return getString(R.string.routine_form_duplicate_time);
            default: return getString(R.string.routine_form_mismatch);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            int maximum = getResources().getDimensionPixelSize(R.dimen.routine_form_max_width);
            int margin = getResources().getDimensionPixelSize(R.dimen.routine_form_window_margin);
            Rect bounds = requireActivity().getWindowManager().getCurrentWindowMetrics().getBounds();
            window.setLayout(maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.min(maximum, Math.max(1, bounds.width() - margin * 2)),
                maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.max(1, bounds.height() - margin * 2));
            window.setGravity(Gravity.CENTER);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(false);
        }
        if (getView() != null) ViewCompat.requestApplyInsets(getView());
    }

    @Override
    public void onDestroyView() {
        destroyingView = true;
        if (getView() != null && retryUpdate != null) getView().removeCallbacks(retryUpdate);
        retryUpdate = null;
        if (resendConfirmation != null) resendConfirmation.dismiss();
        resendConfirmation = null;
        if (confirmation != null) confirmation.dismiss();
        confirmation = null;
        schedule = null;
        scroll = null;
        type = null;
        days.clear();
        dayCards.clear();
        super.onDestroyView();
    }
}
