package com.aula.delta_mobile_app.view.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.LoadingView;
import com.aula.delta_mobile_app.view.RoutineAgendaAdapter;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class RoutineDetailsDialogFragment extends DialogFragment {
    private RoutineViewModel model;
    private RoutineUiState.DetailTarget displayedTarget;
    private Runnable retryUpdate;
    private boolean destroyingView;

    public RoutineDetailsDialogFragment() {
        super(R.layout.dialog_routine_details);
    }

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setStyle(STYLE_NO_TITLE, R.style.Routine_DetailsDialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        destroyingView = false;
        model = new ViewModelProvider(requireParentFragment()).get(RoutineViewModel.class);
        view.findViewById(R.id.routine_details_close).setOnClickListener(button -> model.closeDetails());
        view.findViewById(R.id.routine_details_retry).setOnClickListener(button -> model.retryDetail());
        view.findViewById(R.id.routine_details_refresh).setOnClickListener(button -> model.refresh());
        view.findViewById(R.id.routine_details_edit).setOnClickListener(button -> {
            Fragment parent = getParentFragment();
            if (parent == null || parent.getChildFragmentManager().isStateSaved() || !model.beginEditForm()) return;
            dismissAllowingStateLoss();
            new RoutineEditorDialogFragment().showNow(parent.getChildFragmentManager(), "routine_editor");
        });
        view.findViewById(R.id.routine_details_delete).setOnClickListener(button -> {
            Fragment parent = getParentFragment();
            if (parent == null || parent.getChildFragmentManager().isStateSaved() || !model.requestDelete()) return;
            dismissAllowingStateLoss();
            new RoutineDeleteDialogFragment().showNow(parent.getChildFragmentManager(), "routine_delete");
        });
        model.state().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, RoutineUiState state) {
        if (state.detailTarget == null) {
            dismissAllowingStateLoss();
            return;
        }
        displayedTarget = state.detailTarget;
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm", locale);
        ((TextView) view.findViewById(R.id.routine_details_context)).setText(getString(R.string.routine_details_context,
            displayedTarget.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)), displayedTarget.startTime.format(time)));
        setLoading(view, state.detail.busy());
        RoutineUiState.HabitDetails details = state.details;
        view.findViewById(R.id.routine_details_content).setVisibility(details == null ? View.GONE : View.VISIBLE);
        String message = "";
        if (state.detail.status == ScreenState.Status.ERROR) {
            int resource = state.detail.code == 0 ? R.string.routine_details_error_general : state.detail.code == 404 ? R.string.routine_details_error_not_found
                : RoutineFragment.errorResource(state.detail.code);
            message = getString(resource);
        } else if (details != null) {
            ((TextView) view.findViewById(R.id.routine_details_name)).setText(RoutineAgendaAdapter.name(requireContext(), details.habit));
            TextView category = view.findViewById(R.id.routine_details_category);
            boolean hasCategory = details.habit.category != null && !details.habit.category.trim().isEmpty();
            category.setVisibility(hasCategory ? View.VISIBLE : View.GONE);
            category.setText(hasCategory ? getString(R.string.routine_details_category, details.habit.category) : "");
            ((TextView) view.findViewById(R.id.routine_details_count)).setText(getString(R.string.routine_details_count, details.executionCount == null
                ? getString(R.string.routine_unavailable) : NumberFormat.getIntegerInstance(locale).format(details.executionCount)));
            StringBuilder times = new StringBuilder();
            for (LocalTime start : details.times) {
                if (times.length() > 0) times.append('\n');
                times.append(getString(R.string.routine_details_time, start.format(time)));
            }
            ((TextView) view.findViewById(R.id.routine_details_times)).setText(!details.scheduleComplete ? getString(R.string.routine_details_unknown_schedule)
                : times.length() == 0 ? getString(R.string.routine_details_no_executions) : times.toString());
            ((TextView) view.findViewById(R.id.routine_details_execution_estimate)).setText(getString(R.string.routine_estimate,
                RoutineAgendaAdapter.liters(requireContext(), details.executionLiters)));
            ((TextView) view.findViewById(R.id.routine_details_daily_estimate)).setText(RoutineAgendaAdapter.liters(requireContext(), details.dailyLiters));
            if (!details.scheduleComplete) message = getString(R.string.routine_details_incomplete);
            else if (!details.selectedExecutionPresent) message = getString(R.string.routine_details_changed);
            else if (details.executionLiters == null) message = getString(R.string.routine_details_missing_estimate);
        }
        ((StatusNoticeView) view.findViewById(R.id.routine_details_notice)).setText(message);
        boolean refresh = state.detail.code == 404 || (details != null && (!details.scheduleComplete || !details.selectedExecutionPresent));
        view.findViewById(R.id.routine_details_refresh).setVisibility(refresh ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.routine_details_edit).setVisibility(details != null && state.detail.status == ScreenState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.routine_details_edit).setEnabled(!state.reading.busy());
        view.findViewById(R.id.routine_details_delete).setVisibility(details != null && state.detail.status == ScreenState.Status.SUCCESS ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.routine_details_delete).setEnabled(!state.reading.busy());
        renderRetry(view, state, locale);
    }

    private void renderRetry(View view, RoutineUiState state, Locale locale) {
        if (retryUpdate != null) view.removeCallbacks(retryUpdate);
        boolean error = state.detail.status == ScreenState.Status.ERROR;
        long wait = error ? Math.max(0, state.detail.retryAt - System.currentTimeMillis()) : 0;
        MaterialButton retry = view.findViewById(R.id.routine_details_retry);
        retry.setVisibility(error ? View.VISIBLE : View.GONE);
        retry.setEnabled(wait == 0);
        retry.setText(wait == 0 ? getString(R.string.routine_retry) : getString(R.string.routine_retry_after,
            Instant.ofEpochMilli(state.detail.retryAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale))));
        if (wait > 0) {
            retryUpdate = () -> {
                if (getView() == view && getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) render(view, model.state().getValue());
            };
            view.postDelayed(retryUpdate, wait);
        } else retryUpdate = null;
    }

    private void setLoading(View view, boolean loading) {
        FrameLayout container = view.findViewById(R.id.routine_details_loading);
        if (loading && container.getChildCount() == 0) {
            LoadingView indicator = new LoadingView(requireContext());
            indicator.setContentDescription(getString(R.string.routine_details_loading));
            container.addView(indicator, new FrameLayout.LayoutParams(-1, -1));
        } else if (!loading) container.removeAllViews();
        container.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window != null) {
            int maximum = getResources().getDimensionPixelSize(R.dimen.routine_details_max_width);
            int margin = getResources().getDimensionPixelSize(R.dimen.routine_details_window_margin);
            WindowMetrics metrics = requireActivity().getWindowManager().getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            Insets bars = metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int width = maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.min(maximum, Math.max(1, bounds.width() - bars.left - bars.right - margin * 2));
            int height = maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.max(1, bounds.height() - bars.top - bars.bottom - margin * 2);
            window.setLayout(width, height);
            window.setGravity(Gravity.CENTER);
            WindowCompat.setDecorFitsSystemWindows(window, true);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(false);
        }
        if (getView() != null && model != null) render(getView(), model.state().getValue());
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if (model != null) model.closeDetails();
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!destroyingView && model != null && model.state().getValue().detailTarget == displayedTarget) model.closeDetails();
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
        destroyingView = true;
        if (getView() != null) {
            if (retryUpdate != null) getView().removeCallbacks(retryUpdate);
            setLoading(getView(), false);
        }
        retryUpdate = null;
        super.onDestroyView();
    }
}
