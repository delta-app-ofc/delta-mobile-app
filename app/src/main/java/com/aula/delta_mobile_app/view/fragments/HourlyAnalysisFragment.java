package com.aula.delta_mobile_app.view.fragments;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.HourlyAnalysisData;
import com.aula.delta_mobile_app.model.HourlyAnalysisData.*;
import com.aula.delta_mobile_app.model.LeakMonitoringData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.util.*;

public final class HourlyAnalysisFragment extends AuthFlowFragment {
    private static final int[] PALETTE = {
        0xFFB3CAF4, 0xFF8DAEE6, 0xFF587FCA, 0xFF234E9B, 0xFF193967
    };

    public HourlyAnalysisFragment() {
        super(R.layout.fragment_hourly_analysis);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.home_background);
        LeakMonitoringViewModel monitoring = new ViewModelProvider(requireActivity()).get(LeakMonitoringViewModel.class);
        int id = requireArguments().getInt("propertyId", 0);
        Integer property = id > 0 ? id : new ViewModelProvider(requireActivity()).get(WaterViewModel.class).propertyId;
        view.findViewById(R.id.hourly_back).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.hourly_retry).setOnClickListener(v -> monitoring.load(property, true));
        view.setVisibility(View.INVISIBLE);
        monitoring.state().observe(getViewLifecycleOwner(), state -> {
            if (state.status == ScreenState.Status.IDLE) return;
            showLoading(state.busy());
            if (state.busy()) {
                view.setVisibility(View.INVISIBLE);
                return;
            }
            TextView notice = view.findViewById(R.id.hourly_notice);
            notice.setText("");
            view.findViewById(R.id.hourly_retry).setVisibility(View.GONE);
            if (state.status == ScreenState.Status.ERROR) {
                notice.setText(state.displayMessage());
                view.findViewById(R.id.hourly_retry).setVisibility(View.VISIBLE);
            } else if (state.data.availability == LeakMonitoringData.Availability.NO_PROPERTY || state.data.availability == LeakMonitoringData.Availability.NO_METER)
            notice.setText(LeakHelper.notice(state.data));

            render(view, HourlyAnalysisData.unavailable());
            view.setVisibility(View.VISIBLE);
        });
        monitoring.load(property, false);
        String event = requireArguments().getString("eventId", "");
        if (!event.isEmpty()) monitoring.selectEvent(event);
    }

    private void render(View view, HourlyAnalysisData data) {
        cells(view.findViewById(R.id.hourly_today_cells), data.today, getString(R.string.hourly_today));
        cells(view.findViewById(R.id.hourly_yesterday_cells), data.yesterday, getString(R.string.hourly_yesterday));
        LinearLayout labels = view.findViewById(R.id.hourly_labels);
        labels.removeAllViews();

        for (String time : new String[] {"0h", "5h", "10h", "15h", "20h", "23h"}) {
            TextView label = text(time, 12, 0xFFFFFFFF);
            labels.addView(label, weighted());
        }
        LinearLayout peaks = view.findViewById(R.id.hourly_peak_cards);
        peaks.removeAllViews();
        if (data.peaks.isEmpty()) {
            TextView empty = text(getString(data.available ? R.string.hourly_no_peaks : R.string.hourly_no_peak_data), 15, 0xFFCFD3DC);
            empty.setBackgroundResource(R.drawable.hourly_peak_card);
            empty.setPadding(dp(14), dp(22), dp(14), dp(22));
            peaks.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else for (Peak peak : data.peaks) peak(peaks, peak);
        ((TextView) view.findViewById(R.id.hourly_measured_peak)).setText(getString(R.string.hourly_flow, LeakHelper.number(data.peakFlow, 0)));
        BigDecimal pct = data.increasePercent();
        String percent = pct == null ? "—" : (pct.signum() > 0 ? "+" : "") + LeakHelper.number(pct, 0) + "%";
        ((TextView) view.findViewById(R.id.hourly_comparison)).setText(getString(R.string.hourly_comparison, LeakHelper.number(data.baselineFlow, 0), percent));
        LinearLayout equipment = view.findViewById(R.id.hourly_equipment_rows);
        equipment.removeAllViews();
        List<Equipment> items = data.equipment;
        if (!data.available && items.isEmpty())
            items = Arrays.asList(new Equipment(getString(R.string.hourly_shower), null, false), new Equipment(getString(R.string.hourly_kitchen), null, false),
            new Equipment(getString(R.string.hourly_washer), null, false));
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) divider(equipment);
            Equipment item = items.get(i);
            LinearLayout row = row();
            TextView label = text(item.name == null ? "—" : item.name, 16, 0xFFFFFFFF);
            row.addView(label, weighted());
            TextView value = text((item.approximate && item.flow != null ? "~" : "") + getString(R.string.hourly_flow, LeakHelper.number(item.flow, 0)), 16, 0xFF3978ED);
            pill(value);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-2, -2);
            valueParams.setMarginStart(dp(8));
            row.addView(value, valueParams);
            equipment.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        ((TextView) view.findViewById(R.id.hourly_total)).setText(getString(R.string.hourly_flow, LeakHelper.number(data.estimatedTotal, 0)));
    }

    private void cells(LinearLayout parent, List<Cell> cells, String day) {
        parent.removeAllViews();
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            final int index = i;
            boolean measured = cell.state == CellState.MEASURED;
            TextView value = text(measured ? (cell.liters.signum() == 0 ? "0" : "") : cell.state == CellState.FUTURE ? "·" : "—", 12, 0xFFFFFFFF);
            int color = cell.intensity == null ? 0xFF7185A4 : PALETTE[cell.intensity];
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(color);
            shape.setCornerRadius(dp(2));
            if (!measured) shape.setStroke(dp(1), 0xFFB2C1D8);
            value.setBackground(new InsetDrawable(shape, 0, dp(8), 0, dp(8)));
            value.setGravity(Gravity.CENTER);
            String details = measured ? getString(R.string.hourly_liters, LeakHelper.number(cell.liters, 0)) : getString(cell.state == CellState.FUTURE ? R.string.hourly_future
                : R.string.hourly_missing);
            value.setContentDescription(getString(R.string.hourly_cell, day, i + 1, details));
            value.setFocusable(true);
            value.setClickable(true);
            value.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(cell.interval == null ? getString(R.string.hourly_cell, day, index + 1, details)
                : day + " · " + cell.interval).setMessage(measured ? details : getString(cell.state == CellState.FUTURE ? R.string.hourly_future : R.string
                .hourly_missing_explanation)).setPositiveButton(android.R.string.ok, null).show());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
            if (i < cells.size() - 1) params.setMarginEnd(dp(8));
            parent.addView(value, params);
        }
    }

    private void peak(LinearLayout parent, Peak peak) {
        LinearLayout card = row();
        card.setBackgroundResource(R.drawable.hourly_peak_card);
        card.setPadding(dp(12), dp(12), dp(8), dp(12));
        LinearLayout left = new LinearLayout(requireContext());
        left.setOrientation(LinearLayout.VERTICAL);
        TextView interval = text(peak.interval == null ? "—" : peak.interval, 19, 0xFFF3F4F5);
        interval.setTypeface(null, android.graphics.Typeface.BOLD);
        left.addView(interval);
        if (peak.description != null && !peak.description.isEmpty())
            left.addView(text(peak.description, 14, 0xFFADB1BA));
        card.addView(left, weighted());
        LinearLayout right = new LinearLayout(requireContext());
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        TextView volume = text(getString(R.string.hourly_liters, LeakHelper.number(peak.liters, 0)), 24, 0xFFF3F4F5);
        volume.setTypeface(null, android.graphics.Typeface.BOLD);
        right.addView(volume);
        TextView badge = text(getString(peak.highest ? R.string.hourly_peak_highest : R.string.hourly_peak_high), 11, 0xFFBD0000);
        pill(badge);
        right.addView(badge);
        card.addView(right, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        if (parent.getChildCount() > 0) params.topMargin = dp(26);
        parent.addView(card, params);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(44));
        return row;
    }

    private TextView text(String text, int size, int color) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, -2, 1);
    }

    private void pill(TextView view) {
        view.setBackgroundResource(R.drawable.leak_pill_background);
        view.setPadding(dp(12), dp(3), dp(12), dp(3));
        view.setGravity(Gravity.CENTER);
    }

    private void divider(LinearLayout parent) {
        View line = new View(requireContext());
        line.setBackgroundColor(0x80FFFFFF);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onLoadingBack() {
        showLoading(false);
        NavHostFragment.findNavController(this).popBackStack();
    }
}
