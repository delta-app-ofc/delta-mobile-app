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

public final class LeakAnalysisFragment extends AuthFlowFragment {
    public LeakAnalysisFragment() {
        super(R.layout.fragment_leak_analysis);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        setScreenColor(R.color.home_background);
        LeakMonitoringViewModel monitoring = new ViewModelProvider(requireActivity()).get(LeakMonitoringViewModel.class);
        Bundle args = requireArguments();
        boolean hourly = "hourly".equals(args.getString("analysis", "hourly"));
        int id = args.getInt("propertyId", 0);
        Integer property = id > 0 ? id : new ViewModelProvider(requireActivity()).get(WaterViewModel.class).propertyId;
        ((TextView) view.findViewById(R.id.leak_analysis_title)).setText(hourly ? R.string.leak_hourly_title : R.string.leak_abnormal_title);
        ((TextView) view.findViewById(R.id.leak_analysis_subtitle)).setText(hourly ? R.string.leak_hourly_subtitle : R.string.leak_abnormal_subtitle);
        view.findViewById(R.id.leak_analysis_back).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.leak_analysis_retry).setOnClickListener(v -> monitoring.load(property, true));
        view.setVisibility(View.INVISIBLE);
        monitoring.state().observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.IDLE) return;
            showLoading(value.busy());
            view.setVisibility(value.busy() ? View.INVISIBLE : View.VISIBLE);
            if (value.busy()) return;
            TextView notice = view.findViewById(R.id.leak_analysis_notice);
            View retry = view.findViewById(R.id.leak_analysis_retry);
            LinearLayout cards = view.findViewById(R.id.leak_analysis_cards);
            cards.removeAllViews();
            notice.setText("");
            retry.setVisibility(View.GONE);
            LeakMonitoringData data = value.status == ScreenState.Status.SUCCESS ? value.data : null;
            if (data == null) {
                notice.setText(value.displayMessage());
                retry.setVisibility(View.VISIBLE);
            } else if (data.availability != LeakMonitoringData.Availability.AVAILABLE)
            notice.setText(LeakHelper.notice(data));
            else if (data.events.isEmpty())
                notice.setText(R.string.leak_no_events_description);
            if (data == null || data.events.isEmpty()) {
                View card = LeakHelper.eventCard(getLayoutInflater(), cards, null, data == null ? LeakMonitoringData.Availability.UNAVAILABLE : data.availability);
                card.setClickable(false);
                card.setFocusable(false);
                cards.addView(card, new LinearLayout.LayoutParams(-1, -2));
            } else {
                for (LeakMonitoringData.Event event : data.events) {
                    if (hourly && !event.id.equals(monitoring.selected().id))
                        continue;
                    View card = LeakHelper.eventCard(getLayoutInflater(), cards, event, data.availability);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                    params.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
                    cards.addView(card, params);
                    if (!hourly)
                        card.setOnClickListener(v -> {
                        monitoring.selectEvent(event.id);
                        Bundle next = new Bundle();
                        next.putInt("propertyId", data.propertyId);
                        next.putString("eventId", event.id);
                        NavHostFragment.findNavController(this).navigate(R.id.leakHourlyFragment, next);
                    });
                    else {
                        card.setClickable(false);
                        card.setFocusable(false);
                    }
                }
            }
            LeakFlowChart chart = view.findViewById(R.id.leak_analysis_chart);
            chart.setVisibility(hourly ? View.VISIBLE : View.GONE);
            chart.setEvent(monitoring.selected());
        });
        monitoring.load(property, false);
        String event = args.getString("eventId", "");
        if (!event.isEmpty()) monitoring.selectEvent(event);
    }

    @Override
    protected void onLoadingBack() {
        showLoading(false);
        NavHostFragment.findNavController(this).popBackStack();
    }
}
