package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.LoadingView;
import com.aula.delta_mobile_app.view.RoutineAgendaAdapter;
import com.aula.delta_mobile_app.view.RoutineDayAdapter;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class RoutineFragment extends AuthFlowFragment {
    private static final String PICKER = "routine_date_picker";
    private static final String DETAILS = "routine_habit_details";
    private static final LocalDate FIRST = LocalDate.of(1900, 1, 1), LAST = LocalDate.of(2100, 12, 31);
    private RoutineViewModel model;
    private RoutineDayAdapter days;
    private RoutineAgendaAdapter agenda;
    private LocalDate visibleSelection;
    private Runnable retryUpdate;

    public RoutineFragment() {
        super(R.layout.fragment_routine);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.routine_background);
        model = new ViewModelProvider(this, new RoutineViewModel.Factory(requireContext())).get(RoutineViewModel.class);
        days = new RoutineDayAdapter(this::select, FIRST, LAST);
        agenda = new RoutineAgendaAdapter();
        RecyclerView week = view.findViewById(R.id.routine_days);
        week.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        week.setAdapter(days);
        week.addOnLayoutChangeListener((target, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (days != null && right - left != oldRight - oldLeft) {
                days.resize(right - left - target.getPaddingStart() - target.getPaddingEnd());
                week.scrollToPosition(model.state().getValue().selectedDate.getDayOfWeek().getValue() - 1);
            }
        });
        RecyclerView habits = view.findViewById(R.id.routine_habits);
        habits.setLayoutManager(new LinearLayoutManager(requireContext()));
        habits.setNestedScrollingEnabled(false);
        habits.setAdapter(agenda);
        habits.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View child) {
                bindHabitClick(habits, child);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View child) {}
        });
        view.findViewById(R.id.routine_back).setOnClickListener(button -> back());
        view.findViewById(R.id.routine_previous_month).setOnClickListener(button -> changeMonth(-1));
        view.findViewById(R.id.routine_next_month).setOnClickListener(button -> changeMonth(1));
        view.findViewById(R.id.routine_month).setOnClickListener(button -> chooseDate());
        view.findViewById(R.id.routine_today).setOnClickListener(button -> select(LocalDate.now()));
        view.findViewById(R.id.routine_refresh).setOnClickListener(button -> model.refresh());
        view.findViewById(R.id.routine_retry).setOnClickListener(button -> model.retry());
        restorePicker();
        view.findViewById(R.id.routine_add).setOnClickListener(button -> {
            if (getChildFragmentManager().isStateSaved()) return;
            if (model.beginCreateForm()) new RoutineEditorDialogFragment().showNow(getChildFragmentManager(), "routine_editor");
        });
        model.state().observe(getViewLifecycleOwner(), state -> {
            render(view, state);
            if (state.reading.status == ScreenState.Status.IDLE) model.load();
        });
    }

    private void select(LocalDate date) {
        if (!date.isBefore(FIRST) && !date.isAfter(LAST)) model.selectDate(date);
    }

    private void bindHabitClick(RecyclerView list, View child) {
        child.setFocusable(true);
        child.setEnabled(canOpenDetails(model.state().getValue()));
        ViewCompat.setAccessibilityDelegate(child, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
        child.setOnClickListener(button -> {
            RoutineUiState state = model.state().getValue();
            int position = list.getChildAdapterPosition(button);
            if (!canOpenDetails(state) || position == RecyclerView.NO_POSITION || position >= state.day.executions.size() || getChildFragmentManager().isStateSaved()) return;
            RoutineData.Execution execution = state.day.executions.get(position);
            if (model.openDetails(execution.habit.id, execution.startTime) && getChildFragmentManager().findFragmentByTag(DETAILS) == null)
                new RoutineDetailsDialogFragment().showNow(getChildFragmentManager(), DETAILS);
        });
    }

    private boolean canOpenDetails(RoutineUiState state) {
        return state.reading.status == ScreenState.Status.SUCCESS && state.day != null && !state.stale;
    }

    private void changeMonth(int offset) {
        YearMonth month = model.state().getValue().displayedMonth.plusMonths(offset);
        if (!month.isBefore(YearMonth.from(FIRST)) && !month.isAfter(YearMonth.from(LAST))) model.showMonth(month);
    }

    private void render(View view, RoutineUiState state) {
        view.findViewById(R.id.routine_add).setEnabled(state.form == null && !state.creation.busy() && !state.reading.busy() && !state.savedAwaitingReload);
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        String month = state.displayedMonth.format(DateTimeFormatter.ofPattern("LLLL uuuu", locale));
        MaterialButton monthButton = view.findViewById(R.id.routine_month);
        monthButton.setText(month);
        monthButton.setContentDescription(getString(R.string.routine_month_description, month));
        view.findViewById(R.id.routine_previous_month).setEnabled(state.displayedMonth.isAfter(YearMonth.from(FIRST)));
        view.findViewById(R.id.routine_next_month).setEnabled(state.displayedMonth.isBefore(YearMonth.from(LAST)));
        LocalDate today = LocalDate.now();
        view.findViewById(R.id.routine_today).setEnabled(!today.isBefore(FIRST) && !today.isAfter(LAST));
        days.submit(state.selectedDate, today);
        if (!state.selectedDate.equals(visibleSelection)) {
            ((RecyclerView) view.findViewById(R.id.routine_days)).scrollToPosition(state.selectedDate.getDayOfWeek().getValue() - 1);
            visibleSelection = state.selectedDate;
        }
        ((TextView) view.findViewById(R.id.routine_agenda_title)).setText(getString(R.string.routine_agenda_date,
            state.selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))));
        boolean initialLoading = state.reading.busy() && state.data == null;
        setLoading(view, initialLoading);
        view.findViewById(R.id.routine_agenda).setVisibility(state.day == null ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.routine_empty).setVisibility(state.day != null && state.day.confirmedEmpty() ? View.VISIBLE : View.GONE);
        agenda.submit(state.day);
        RecyclerView habits = view.findViewById(R.id.routine_habits);
        for (int i = 0; i < habits.getChildCount(); i++) habits.getChildAt(i).setEnabled(canOpenDetails(state));
        TextView total = view.findViewById(R.id.routine_total);
        total.setText(state.day == null ? getString(R.string.routine_unavailable) : state.day.totalComplete ? RoutineAgendaAdapter.liters(requireContext(), state.day.totalLiters)
            : getString(R.string.routine_incomplete_total));
        TextView subtotal = view.findViewById(R.id.routine_subtotal);
        boolean incompleteTotal = state.day != null && !state.day.totalComplete;
        subtotal.setVisibility(incompleteTotal ? View.VISIBLE : View.GONE);
        if (incompleteTotal) subtotal.setText(getString(R.string.routine_known_subtotal, RoutineAgendaAdapter.liters(requireContext(), state.day.knownLiters)));
        ((StatusNoticeView) view.findViewById(R.id.routine_data_notice)).setText(dataNotice(state.day));
        renderFeedback(view, state, locale);
    }

    private String dataNotice(RoutineData.Day day) {
        if (day == null) return "";
        if (!day.incompleteHabits.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (RoutineData.Habit habit : day.incompleteHabits) {
                if (names.length() > 0) names.append('\n');
                names.append(getString(R.string.routine_incomplete_habit, RoutineAgendaAdapter.name(requireContext(), habit)));
            }
            return getString(R.string.routine_incomplete_notice, names);
        }
        return day.totalComplete ? "" : getString(R.string.routine_missing_estimate);
    }

    private void renderFeedback(View view, RoutineUiState state, Locale locale) {
        if (retryUpdate != null) view.removeCallbacks(retryUpdate);
        boolean error = state.reading.status == ScreenState.Status.ERROR;
        String message = error ? getString(errorResource(state.reading.code)) : state.reading.busy() && state.data != null ? getString(R.string.routine_updating) : "";
        if (error && state.stale) message = getString(R.string.routine_error_stale, message, getString(R.string.routine_stale));
        if (error && state.savedAwaitingReload) message = getString(R.string.routine_saved_reload_error);
        ((StatusNoticeView) view.findViewById(R.id.routine_notice)).setText(message);
        long wait = error ? Math.max(0, state.reading.retryAt - System.currentTimeMillis()) : 0;
        MaterialButton retry = view.findViewById(R.id.routine_retry);
        retry.setVisibility(error ? View.VISIBLE : View.GONE);
        retry.setEnabled(wait == 0);
        retry.setText(wait == 0 ? getString(R.string.routine_retry) : getString(R.string.routine_retry_after,
            Instant.ofEpochMilli(state.reading.retryAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale))));
        view.findViewById(R.id.routine_refresh).setEnabled(!state.reading.busy() && wait == 0);
        if (wait > 0) {
            retryUpdate = () -> {
                if (getView() == view && isStarted()) render(view, model.state().getValue());
            };
            view.postDelayed(retryUpdate, wait);
        } else retryUpdate = null;
    }

    private boolean isStarted() {
        return getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED);
    }

    static int errorResource(int code) {
        switch (code) {
            case 400: return R.string.routine_error_validation;
            case 401: return R.string.routine_error_auth;
            case 403: return R.string.routine_error_forbidden;
            case 404: return R.string.routine_error_not_found;
            case 409: return R.string.routine_error_conflict;
            case 429: return R.string.routine_error_rate;
            default: return code >= 500 ? R.string.routine_error_server : R.string.routine_error_general;
        }
    }

    private void setLoading(View view, boolean loading) {
        FrameLayout container = view.findViewById(R.id.routine_loading);
        if (loading && container.getChildCount() == 0) {
            LoadingView indicator = new LoadingView(requireContext());
            indicator.setContentDescription(getString(R.string.routine_loading));
            container.addView(indicator, new FrameLayout.LayoutParams(-1, -1));
        } else if (!loading) container.removeAllViews();
        container.setVisibility(loading ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.routine_scroll).setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void chooseDate() {
        if (getChildFragmentManager().findFragmentByTag(PICKER) != null) return;
        long selected = model.state().getValue().selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        CalendarConstraints constraints = new CalendarConstraints.Builder().setStart(FIRST.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            .setEnd(LAST.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()).setOpenAt(selected).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.routine_choose_date).setTheme(R.style.Routine_Calendar)
            .setPositiveButtonText(R.string.routine_confirm).setNegativeButtonText(R.string.routine_cancel).setCalendarConstraints(constraints).setSelection(selected).build();
        bindPicker(picker);
        picker.show(getChildFragmentManager(), PICKER);
    }

    @SuppressWarnings("unchecked")
    private void restorePicker() {
        Fragment picker = getChildFragmentManager().findFragmentByTag(PICKER);
        if (picker instanceof MaterialDatePicker) bindPicker((MaterialDatePicker<Long>) picker);
    }

    private void bindPicker(MaterialDatePicker<Long> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(value -> select(Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getView() != null && model != null) render(getView(), model.state().getValue());
    }

    @Override
    public void onStop() {
        if (getView() != null) {
            if (retryUpdate != null) getView().removeCallbacks(retryUpdate);
            setLoading(getView(), false);
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        View view = getView();
        if (view != null) {
            if (retryUpdate != null) view.removeCallbacks(retryUpdate);
            ((RecyclerView) view.findViewById(R.id.routine_days)).setAdapter(null);
            ((RecyclerView) view.findViewById(R.id.routine_habits)).setAdapter(null);
            setLoading(view, false);
        }
        Fragment picker = getChildFragmentManager().findFragmentByTag(PICKER);
        if (picker instanceof MaterialDatePicker) ((MaterialDatePicker<?>) picker).clearOnPositiveButtonClickListeners();
        retryUpdate = null;
        visibleSelection = null;
        days = null;
        agenda = null;
        super.onDestroyView();
    }
}
