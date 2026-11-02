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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineUiState;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;
import com.google.android.material.button.MaterialButton;

public final class RoutineDeleteDialogFragment extends DialogFragment {
    private RoutineViewModel model;
    private RoutineUiState.DeleteTarget displayedTarget;
    private Runnable retryUpdate;
    private boolean destroyingView;

    public RoutineDeleteDialogFragment() {
        super(R.layout.dialog_routine_delete);
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
        view.findViewById(R.id.routine_delete_confirm).setOnClickListener(button -> model.confirmDelete());
        view.findViewById(R.id.routine_delete_check).setOnClickListener(button -> model.checkDeletion());
        view.findViewById(R.id.routine_delete_cancel).setOnClickListener(button -> model.cancelDelete());
        requireDialog().setCanceledOnTouchOutside(false);
        model.state().observe(getViewLifecycleOwner(), state -> render(view, state));
    }

    private void render(View view, RoutineUiState state) {
        if (state.deleteTarget == null) {
            dismissAllowingStateLoss();
            return;
        }
        displayedTarget = state.deleteTarget;
        ((TextView) view.findViewById(R.id.routine_delete_habit)).setText(getString(R.string.routine_delete_habit, name(displayedTarget)));
        boolean busy = state.deletion.busy();
        setCancelable(!busy);
        boolean checking = state.uncertainDeletion && state.reading.busy();
        String notice = "";
        if (busy) notice = getString(R.string.routine_delete_sending);
        else if (state.uncertainDeletion) {
            if (checking) notice = getString(R.string.routine_delete_checking);
            else if (state.checkedDeletion) notice = getString(R.string.routine_delete_still_present);
            else if (state.reading.status == ScreenState.Status.ERROR) notice = getString(R.string.routine_delete_check_error);
            else notice = getString(R.string.routine_delete_uncertain);
        } else if (state.deletion.status == ScreenState.Status.ERROR) {
            notice = getString(state.deletion.code == 404 ? R.string.routine_delete_not_found : R.string.routine_delete_error);
        }
        ((StatusNoticeView) view.findViewById(R.id.routine_delete_notice)).setText(notice);
        long deletionWait = Math.max(0, state.deletion.retryAt - System.currentTimeMillis());
        long readingWait = Math.max(0, state.reading.retryAt - System.currentTimeMillis());
        MaterialButton confirm = view.findViewById(R.id.routine_delete_confirm);
        confirm.setVisibility(!state.uncertainDeletion || state.checkedDeletion ? View.VISIBLE : View.GONE);
        confirm.setEnabled(!busy && !checking && deletionWait == 0);
        confirm.setText(state.checkedDeletion ? R.string.routine_delete_retry : R.string.routine_delete_confirm);
        MaterialButton check = view.findViewById(R.id.routine_delete_check);
        check.setVisibility(state.uncertainDeletion && !state.checkedDeletion ? View.VISIBLE : View.GONE);
        check.setEnabled(!busy && !checking && readingWait == 0);
        view.findViewById(R.id.routine_delete_cancel).setEnabled(!busy);
        scheduleRetry(view, Math.max(deletionWait, readingWait));
    }

    private String name(RoutineUiState.DeleteTarget target) {
        if (target.name != null && !target.name.trim().isEmpty()) return target.name;
        String[] labels = getResources().getStringArray(R.array.routine_form_types);
        return target.habitId >= 1 && target.habitId <= labels.length ? labels[target.habitId - 1] : getString(R.string.routine_habit_unknown);
    }

    private void scheduleRetry(View view, long wait) {
        if (retryUpdate != null) view.removeCallbacks(retryUpdate);
        if (wait > 0) {
            retryUpdate = () -> {
                if (getView() == view && getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                    render(view, model.state().getValue());
            };
            view.postDelayed(retryUpdate, wait);
        } else retryUpdate = null;
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
            int height = maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
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
        if (model != null) model.cancelDelete();
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!destroyingView && model != null && model.state().getValue().deleteTarget == displayedTarget) model.cancelDelete();
    }

    @Override
    public void onDestroyView() {
        destroyingView = true;
        if (getView() != null && retryUpdate != null) getView().removeCallbacks(retryUpdate);
        retryUpdate = null;
        super.onDestroyView();
    }
}
