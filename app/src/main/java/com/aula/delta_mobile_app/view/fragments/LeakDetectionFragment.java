package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.LeakMonitoringData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.LeakFlowChart;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class LeakDetectionFragment extends AuthFlowFragment {
    private LeakMonitoringViewModel monitoring;
    private Integer propertyId;
    private int carouselOffset;

    public LeakDetectionFragment() {
        super(R.layout.fragment_leak_detection);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.home_background);
        monitoring = new ViewModelProvider(requireActivity()).get(LeakMonitoringViewModel.class);
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        int argument = getArguments() == null ? 0 : getArguments().getInt("propertyId", 0);
        propertyId = argument > 0 ? argument : water.propertyId;
        carouselOffset = saved == null ? 0 : saved.getInt("carouselOffset", 0);
        view.setVisibility(View.INVISIBLE);
        view.findViewById(R.id.leak_action_valve).setOnClickListener(v -> guide(R.string.leak_check_valve, R.string.leak_valve_guide));
        view.findViewById(R.id.leak_action_pipes).setOnClickListener(v -> guide(R.string.leak_check_pipes, R.string.leak_pipes_guide));
        view.findViewById(R.id.leak_action_alerts).setOnClickListener(v -> guide(R.string.leak_disable_alerts, R.string.leak_alerts_unavailable));
        view.findViewById(R.id.leak_hourly).setOnClickListener(v -> openAnalysis(R.id.leakHourlyFragment));
        view.findViewById(R.id.leak_abnormal).setOnClickListener(v -> openAnalysis(R.id.leakAbnormalFragment));
        monitoring.state().observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.IDLE) return;
            showLoading(value.busy());
            view.setVisibility(value.busy() ? View.INVISIBLE : View.VISIBLE);
            if (value.busy()) return;
            LinearLayout cards = view.findViewById(R.id.leak_events);
            cards.removeAllViews();
            TextView notice = view.findViewById(R.id.leak_notice);
            Button retry = view.findViewById(R.id.leak_retry);
            notice.setText("");
            retry.setVisibility(View.GONE);
            LeakMonitoringData data = value.status == ScreenState.Status.SUCCESS ? value.data : null;
            if (data == null) {
                notice.setText(value.displayMessage());
                retry.setVisibility(View.VISIBLE);
                retry.setText(R.string.leak_retry);
                retry.setOnClickListener(v -> monitoring.load(propertyId, true));
            } else if (data.availability == LeakMonitoringData.Availability.NO_PROPERTY || data.availability == LeakMonitoringData.Availability.NO_METER) {
                notice.setText(LeakHelper.notice(data));
                retry.setVisibility(View.VISIBLE);
                boolean link = data.availability == LeakMonitoringData.Availability.NO_METER;
                retry.setText(link ? R.string.leak_link_meter : R.string.leak_retry);
                retry.setOnClickListener(v -> {
                    if (link) {
                        if (!java.util.Objects.equals(water.propertyId, data.propertyId))
                            water.select(data.propertyId);
                        navigate(R.id.qrScanFragment);
                    } else monitoring.load(propertyId, true);
                });
            }
            if (data == null || data.events.isEmpty()) {
                View card = LeakHelper.eventCard(getLayoutInflater(), cards, null, data == null ? LeakMonitoringData.Availability.UNAVAILABLE : data.availability);
                card.setContentDescription(getString(data != null && data.availability == LeakMonitoringData.Availability.AVAILABLE ? R.string.leak_no_events
                    : R.string.leak_unavailable));
                card.setOnClickListener(v -> guide(R.string.leak_title, data == null ? R.string.leak_no_telemetry_description : LeakHelper.notice(data)));
                addCard(cards, card);
            } else
            for (LeakMonitoringData.Event event : data.events) {
                View card = LeakHelper.eventCard(getLayoutInflater(), cards, event, data.availability);
                card.setTag(event.id);
                card.setOnClickListener(v -> {
                    monitoring.selectEvent(event.id);
                    updateEvent(view);
                });
                addCard(cards, card);
            }
            updateEvent(view);
            HorizontalScrollView scroll = view.findViewById(R.id.leak_event_scroll);
            scroll.post(() -> {
                if (getView() == view) scroll.scrollTo(carouselOffset, 0);
            });
        });
        monitoring.load(propertyId, false);
        if (saved != null) monitoring.selectEvent(saved.getString("selectedEvent"));
    }

    private void addCard(LinearLayout parent, View card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(188), -2);
        params.setMarginEnd(dp(24));
        parent.addView(card, params);
    }

    private void updateEvent(View view) {
        LeakMonitoringData.Event event = monitoring.selected();
        ((LeakFlowChart) view.findViewById(R.id.leak_chart)).setEvent(event);
        ((TextView) view.findViewById(R.id.leak_volume)).setText(getString(R.string.leak_volume, event == null || event.volumeLiters == null ? "—"
            : LeakHelper.number(event.volumeLiters, 0) + " L"));
        LinearLayout cards = view.findViewById(R.id.leak_events);
        for (int i = 0; i < cards.getChildCount(); i++) {
            View card = cards.getChildAt(i);
            card.setSelected(event != null && event.id.equals(card.getTag()));
        }
    }

    private void openAnalysis(int destination) {
        Bundle args = new Bundle();
        ScreenState<LeakMonitoringData> value = monitoring.state().getValue();
        Integer selected = value.status == ScreenState.Status.SUCCESS ? value.data.propertyId : propertyId;
        args.putInt("propertyId", selected == null ? 0 : selected);
        LeakMonitoringData.Event event = monitoring.selected();
        args.putString("eventId", event == null ? "" : event.id);
        NavHostFragment.findNavController(this).navigate(destination, args);
    }

    private void guide(int title, int message) {
        new MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        if (getView() != null) {
            state.putInt("carouselOffset", ((HorizontalScrollView) getView().findViewById(R.id.leak_event_scroll)).getScrollX());
            LeakMonitoringData.Event event = monitoring.selected();
            if (event != null) state.putString("selectedEvent", event.id);
        }
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onLoadingBack() {
        showLoading(false);
        NavHostFragment.findNavController(this).popBackStack();
    }
}
