package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.DashboardMetrics;
import com.aula.delta_mobile_app.model.HomeDashboard;
import com.aula.delta_mobile_app.model.LeakMonitoringData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.DashboardHistoryChart;
import com.aula.delta_mobile_app.viewmodel.*;
import com.aula.delta_mobile_app.viewmodel.HomeLoadState;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

public class DashboardFragment extends AuthFlowFragment {
    private boolean loadingCheckPosted;
    private Integer selectedProperty;
    private int period;
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable refreshTask;
    private WaterViewModel waterModel;

    public DashboardFragment() {
        super(R.layout.fragment_dashboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.home_background);
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        HomeViewModel home = new ViewModelProvider(this).get(HomeViewModel.class);
        view.setVisibility(View.INVISIBLE);
        setDashboardLoading(true);
        selectedProperty = null;
        waterModel = water;
        period = state == null ? 0 : Math.max(0, Math.min(2, state.getInt("dashboardPeriod", 0)));
        int[] tabs = {R.id.dash_daily, R.id.dash_weekly, R.id.dash_monthly};
        for (int i = 0; i < tabs.length; i++) {
            final int selection = i;
            view.findViewById(tabs[i]).setOnClickListener(v -> {
                period = selection;
                renderConsumption(view, water);
            });
        }
        renderConsumption(view, water);
        TextView apiStatus = view.findViewById(R.id.home_api_status);
        apiStatus.setText("");

        view.findViewById(R.id.dash_cost_card).setOnClickListener(v -> navigate(R.id.costsHistoryFragment));
        view.findViewById(R.id.dash_diagnosis).setOnClickListener(v -> navigate(R.id.leakDetectionFragment));
        view.findViewById(R.id.dash_leak_alert).setOnClickListener(v -> navigate(R.id.leakDetectionFragment));
        view.findViewById(R.id.dash_abnormal_alert).setOnClickListener(v -> navigate(R.id.leakAbnormalFragment));
        view.findViewById(R.id.dash_pressure_alert).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.dash_pressure)
            .setMessage(R.string.dash_pressure_description).setPositiveButton(android.R.string.ok, null).show());
        LeakMonitoringViewModel monitoring = new ViewModelProvider(requireActivity()).get(LeakMonitoringViewModel.class);
        monitoring.state().observe(getViewLifecycleOwner(), value -> renderMonitoring(view, value));
        water.properties.observe(getViewLifecycleOwner(), value -> {
            apiStatus.setText("");

            if (value.status == ScreenState.Status.SUCCESS) {
                UserPropertyResponse chosen = null;
                for (UserPropertyResponse link : value.data) {
                    if (link == null || link.propertyId == null) continue;
                    if (chosen == null || Objects.equals(link.propertyId, water.propertyId))
                        chosen = link;
                }
                selectedProperty = chosen == null ? null : chosen.propertyId;
                ((TextView) view.findViewById(R.id.home_greeting)).setText(chosen == null || chosen.propertyName == null ? getString(R.string.dash_property) : chosen.propertyName);
                monitoring.load(selectedProperty, false);
                renderMonitoring(view, monitoring.state().getValue());
                if (selectedProperty == null) {
                    home.selectProperty(null);
                    home.clearReadings();
                    apiStatus.setText(R.string.home_no_properties);
                } else {
                    home.selectProperty(selectedProperty);
                    if (!Objects.equals(selectedProperty, water.propertyId) || water.consumption.getValue().status == ScreenState.Status.IDLE)
                        water.select(selectedProperty);
                }
            }
        });
        water.consumption.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.IDLE) return;
            home.selectProperty(selectedProperty);
            if (value.status == ScreenState.Status.SUCCESS && selectedProperty != null) {
                home.update(value.data);
            } else {
                home.clearReadings();
            }
            renderConsumption(view, water);
            if (value.status == ScreenState.Status.SUCCESS)
                ((TextView) view.findViewById(R.id.dash_updated)).setText(getString(R.string.dash_updated, java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"))));
        });
        home.cost.observe(getViewLifecycleOwner(), value -> {
            ((TextView) view.findViewById(R.id.home_cost)).setText(value.status == ScreenState.Status.SUCCESS && Objects.equals(value.data.propertyId, selectedProperty)
                && value.data.estimatedCost != null ? money(value.data.estimatedCost) : getString(R.string.home_pending_metric));
        });

        apiStatus.setOnClickListener(v -> {
            view.setVisibility(View.INVISIBLE);
            setDashboardLoading(true);
            home.loadProfile();
            if (water.properties.getValue().status != ScreenState.Status.SUCCESS || selectedProperty == null) water.loadProperties();
            else water.select(selectedProperty);
        });
        view.findViewById(R.id.home_goal).setOnClickListener(v -> navigate(R.id.monthlyGoalsFragment));
        view.findViewById(R.id.home_notifications).setOnClickListener(v -> ((android.widget.ScrollView) view).smoothScrollTo(0, view.findViewById(R.id.dash_leak_alert).getTop()));
        view.findViewById(R.id.home_settings).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.home_account_title).setItems(new String[] {
            getString(R.string.home_select_property), getString(R.string.auth_home_meter), getString(R.string.auth_logout)
        }, (dialog, item) -> {
            if (item == 0)
                selectProperty(view, water, home);
            else if (item == 1 && selectedProperty != null)
                navigate(R.id.qrScanFragment);
            else if (item == 1)
                Toast.makeText(requireContext(), R.string.home_no_properties, Toast.LENGTH_SHORT).show();
            else {
                registration().reset();
                water.reset();
                new ViewModelProvider(requireActivity()).get(LoginViewModel.class).logout();
            }
        }).show());
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
        home.loadProfile();

        Observer<Object> readiness = ignored -> {
            if (loadingCheckPosted) return;
            loadingCheckPosted = true;
            view.post(() -> {
                loadingCheckPosted = false;
                if (getView() != view) return;
                HomeDashboard summary = home.dashboard().getValue();
                boolean needsCost = water.consumption.getValue().status == ScreenState.Status.SUCCESS && summary != null && summary.monthLiters != null;
                boolean ready = HomeLoadState.ready(home.user.getValue(), water.properties.getValue(), selectedProperty != null, water.consumption.getValue(),
                    home.cost.getValue(), needsCost);
                view.setVisibility(ready ? View.VISIBLE : View.INVISIBLE);
                setDashboardLoading(!ready);
            });
        };
        water.properties.observe(getViewLifecycleOwner(), readiness::onChanged);
        water.consumption.observe(getViewLifecycleOwner(), readiness::onChanged);
        home.user.observe(getViewLifecycleOwner(), readiness::onChanged);
        home.cost.observe(getViewLifecycleOwner(), readiness::onChanged);
    }

    private void selectProperty(View view, WaterViewModel water, HomeViewModel home) {
        ScreenState<List<UserPropertyResponse>> value = water.properties.getValue();
        List<UserPropertyResponse> links = new ArrayList<>();
        if (value.status == ScreenState.Status.SUCCESS)
            for (UserPropertyResponse link : value.data)
                if (link != null && link.propertyId != null) links.add(link);
        if (links.isEmpty()) {
            Toast.makeText(requireContext(), R.string.home_no_properties, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[links.size()];
        int checked = -1;
        for (int i = 0; i < links.size(); i++) {
            UserPropertyResponse link = links.get(i);
            labels[i] = link.propertyName == null ? "Imóvel " + link.propertyId : link.propertyName;
            if (Objects.equals(selectedProperty, link.propertyId)) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.home_select_property).setSingleChoiceItems(labels, checked, (dialog, item) -> {
            dialog.dismiss();
            Integer id = links.get(item).propertyId;
            if (Objects.equals(id, selectedProperty)) return;
            view.setVisibility(View.INVISIBLE);
            setDashboardLoading(true);
            selectedProperty = id;
            ((TextView) view.findViewById(R.id.home_greeting)).setText(labels[item]);
            home.selectProperty(id);
            water.select(id);
            new ViewModelProvider(requireActivity()).get(LeakMonitoringViewModel.class).load(id, false);
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void setDashboardLoading(boolean loading) {
        showLoading(loading);
    }

    @Override
    public void onDestroyView() {
        refreshHandler.removeCallbacksAndMessages(null);
        refreshTask = null;
        waterModel = null;
        setDashboardLoading(false);
        loadingCheckPosted = false;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("dashboardPeriod", period);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshTask = new Runnable() {
            @Override
            public void run() {
                if (getView() == null || waterModel == null) return;
                if (selectedProperty != null && !waterModel.consumption.getValue().busy())
                    waterModel.select(selectedProperty);
                refreshHandler.postDelayed(this, 300000);
            }
        };
        refreshHandler.postDelayed(refreshTask, 300000);
    }

    @Override
    public void onStop() {
        refreshHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    private void renderMonitoring(View view, ScreenState<LeakMonitoringData> value) {
        LeakMonitoringData data = value.status == ScreenState.Status.SUCCESS && Objects.equals(value.data.propertyId, selectedProperty) ? value.data : null;
        boolean available = data != null && data.availability == LeakMonitoringData.Availability.AVAILABLE;
        int count = 0;
        if (available)
            for (LeakMonitoringData.Event event : data.events)
                if (event.status != LeakMonitoringData.Status.RESOLVED) count++;
        ((TextView) view.findViewById(R.id.dash_leak_count)).setText(available ? String.valueOf(count) : getString(R.string.home_pending_metric));
        ((TextView) view.findViewById(R.id.dash_leak_alert_description)).setText(available ? (count == 0 ? getString(R.string.dash_no_alerts)
            : getString(R.string.dash_active_alerts, count)) : getString(R.string.dash_telemetry_unknown));
    }

    private void renderConsumption(View view, WaterViewModel water) {
        int[] tabs = {R.id.dash_daily, R.id.dash_weekly, R.id.dash_monthly};
        for (int i = 0; i < tabs.length; i++) {
            TextView tab = view.findViewById(tabs[i]);
            tab.setSelected(period == i);
            tab.setTextColor(period == i ? 0xFF14345A : 0xFFE6F2FF);
        }
        ((TextView) view.findViewById(R.id.dash_period)).setText(period == 0 ? R.string.dash_today : period == 1 ? R.string.dash_week : R.string.dash_month);
        ScreenState<List<com.aula.delta_mobile_app.model.ApiModels.ConsumptionDailyResponse>>
        state = water.consumption.getValue();
        DashboardMetrics metrics = state.status == ScreenState.Status.SUCCESS && selectedProperty != null ? DashboardMetrics.from(
            state.data, selectedProperty, java.time.LocalDate.now(), period) : null;
        ((TextView) view.findViewById(R.id.home_today)).setText(liters(metrics == null ? null : metrics.current));
        ((DashboardHistoryChart) view.findViewById(R.id.home_chart)).setMetrics(metrics);
        TextView comparison = view.findViewById(R.id.dash_comparison);
        comparison.setTextColor(0xFFE1E9FA);
        if (metrics == null || metrics.previous == null || metrics.previous.signum() == 0) {
            comparison.setText(R.string.dash_comparison_unknown);
            return;
        }
        BigDecimal delta = metrics.current.subtract(metrics.previous).multiply(BigDecimal.valueOf(100)).divide(metrics.previous, 1, java.math.RoundingMode.HALF_UP);
        String previous = getString(period == 0 ? R.string.dash_yesterday : period == 1 ? R.string.dash_previous_week : R.string.dash_previous_month);
        if (delta.signum() == 0) comparison.setText(getString(R.string.dash_same, previous));
        else {
            NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
            format.setMaximumFractionDigits(1);
            comparison.setText(getString(R.string.dash_comparison, delta.signum() < 0 ? "↘" : "↗", format.format(delta.abs()), getString(
                delta.signum() < 0 ? R.string.dash_below : R.string.dash_above), previous));
            comparison.setTextColor(delta.signum() < 0 ? 0xFF52F0BF : 0xFFFFD0CE);
        }
    }

    private String liters(BigDecimal value) {
        if (value == null) return getString(R.string.home_pending_metric);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(1);
        return getString(R.string.home_liters, format.format(value));
    }

    private String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(value).replace("\u00a0", "").replace(" ", "");
    }
}
