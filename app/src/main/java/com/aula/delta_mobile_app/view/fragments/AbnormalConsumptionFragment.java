package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AbnormalConsumptionData;
import com.aula.delta_mobile_app.model.AbnormalConsumptionData.*;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.AbnormalConsumptionChart;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class AbnormalConsumptionFragment extends AuthFlowFragment {
    private AbnormalConsumptionViewModel model;

    public AbnormalConsumptionFragment() {
        super(R.layout.fragment_abnormal_consumption);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.home_background);
        model = new ViewModelProvider(this).get(AbnormalConsumptionViewModel.class);
        if (saved != null)
            try {
            model.period = Period.valueOf(saved.getString("period", "WEEK"));
        } catch (IllegalArgumentException ignored) {
        }
        int id = requireArguments().getInt("propertyId", 0);
        Integer property = id > 0 ? id : new ViewModelProvider(requireActivity()).get(WaterViewModel.class).propertyId;
        view.findViewById(R.id.abnormal_back).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.abnormal_retry).setOnClickListener(v -> model.load(property, true));
        view.findViewById(R.id.abnormal_summary).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.abnormal_unavailable)
            .setMessage(R.string.abnormal_missing_message).setPositiveButton(android.R.string.ok, null).show());
        int[] tabs = {R.id.abnormal_tab_week, R.id.abnormal_tab_month, R.id.abnormal_tab_year};
        for (int i = 0; i < tabs.length; i++) {
            final Period period = Period.values()[i];
            view.findViewById(tabs[i]).setOnClickListener(v -> {
                model.period = period;
                comparison(view);
            });
        }
        view.findViewById(R.id.abnormal_history_chart).setOnClickListener(v -> details(false));
        view.findViewById(R.id.abnormal_bars).setOnClickListener(v -> details(true));
        view.setVisibility(View.INVISIBLE);
        model.state().observe(getViewLifecycleOwner(), state -> {
            if (state.status == ScreenState.Status.IDLE) return;
            showLoading(state.busy());
            if (state.busy()) {
                view.setVisibility(View.INVISIBLE);
                return;
            }
            TextView notice = view.findViewById(R.id.abnormal_notice);
            notice.setText(state.status == ScreenState.Status.ERROR ? state.displayMessage() : "");
            view.findViewById(R.id.abnormal_retry).setVisibility(state.status == ScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
            ((AbnormalConsumptionChart) view.findViewById(R.id.abnormal_history_chart)).setHistory(state.status == ScreenState.Status.SUCCESS ? state.data : null);
            comparison(view);
            view.setVisibility(View.VISIBLE);
        });
        model.load(property, false);
    }

    private void comparison(View view) {
        ScreenState<AbnormalConsumptionData> state = model.state().getValue();
        AbnormalConsumptionData data = state.status == ScreenState.Status.SUCCESS ? state.data : null;
        int[] tabs = {R.id.abnormal_tab_week, R.id.abnormal_tab_month, R.id.abnormal_tab_year};
        for (int i = 0; i < tabs.length; i++) {
            View tab = view.findViewById(tabs[i]);
            boolean selected = model.period == Period.values()[i];
            tab.setSelected(selected);
            tab.setBackgroundResource(selected ? R.drawable.abnormal_tab_selected : R.drawable.abnormal_tab_idle);
        }
        int index = model.period.ordinal();
        ((TextView) view.findViewById(R.id.abnormal_period_title)).setText(new int[] {
            R.string.abnormal_week_title, R.string.abnormal_month_title, R.string.abnormal_year_title
        }
            [index]);
        ((TextView) view.findViewById(R.id.abnormal_period_badge)).setText(new int[] {
            R.string.abnormal_week_badge, R.string.abnormal_month_badge, R.string.abnormal_year_badge
        }
            [index]);
        ((AbnormalConsumptionChart) view.findViewById(R.id.abnormal_bars)).setComparison(data, model.period);
        BigDecimal current = null, previous = null, change = null;
        if (data != null) {
            List<Bucket> values = data.periods(model.period);
            current = values.get(3).liters;
            previous = values.get(2).liters;
            change = AbnormalConsumptionData.variation(current, previous);
        }
        ((TextView) view.findViewById(R.id.abnormal_current)).setText(getString(R.string.abnormal_current, LeakHelper.number(current, 0), change == null ? "—"
            : (change.signum() > 0 ? "+" : "") + LeakHelper.number(change, 0) + "%"));
        ((TextView) view.findViewById(R.id.abnormal_previous)).setText(getString(R.string.abnormal_previous, LeakHelper.number(previous, 0)));
        ((TextView) view.findViewById(R.id.abnormal_aligned)).setText(data == null ? "" : getString(R.string.abnormal_aligned, data.periods(model.period).get(3).end.format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    private void details(boolean comparison) {
        ScreenState<AbnormalConsumptionData> state = model.state().getValue();
        StringBuilder message = new StringBuilder();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (state.status == ScreenState.Status.SUCCESS) {
            if (comparison)
                for (Bucket bucket : state.data.periods(model.period))
                    message.append(getString(R.string.abnormal_period_value, bucket.start.format(format), bucket.end.format(format), LeakHelper.number(bucket.liters, 0)))
                .append("\n");
            else
                for (int i = 6; i >= 0; i--) {
                java.time.LocalDate day = state.data.reference.minusDays(i);
                message.append(getString(R.string.abnormal_day_value, day.format(format), LeakHelper.number(state.data.day(day), 0))).append("\n");
            }
        } else message.append(getString(R.string.abnormal_graph_unavailable));
        new MaterialAlertDialogBuilder(requireContext()).setTitle(comparison ? R.string.abnormal_comparison : R.string.abnormal_history).setMessage(message.toString().trim())
            .setPositiveButton(android.R.string.ok, null).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle saved) {
        saved.putString("period", model.period.name());
        super.onSaveInstanceState(saved);
    }

    @Override
    protected void onLoadingBack() {
        showLoading(false);
        NavHostFragment.findNavController(this).popBackStack();
    }
}
