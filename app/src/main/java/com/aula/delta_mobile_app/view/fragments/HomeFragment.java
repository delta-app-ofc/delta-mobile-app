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
import com.aula.delta_mobile_app.model.HomeDashboard;
import com.aula.delta_mobile_app.model.RoutineData;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.HomeTemperatureChart;
import com.aula.delta_mobile_app.view.activities.main.MainActivity;
import com.aula.delta_mobile_app.viewmodel.*;
import com.aula.delta_mobile_app.viewmodel.HomeLoadState;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

public class HomeFragment extends AuthFlowFragment {
    private boolean loadingCheckPosted;
    private Integer selectedProperty;
    private HomeViewModel homeModel;
    private ProfileSettingsViewModel profileModel;
    private WaterViewModel waterModel;
    private RoutineViewModel routineModel;
    private RoutineData.Day todayRoutine;
    private boolean routineLoaded;
    private HomeTemperatureChart temperatureChart;
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshConsumption = new Runnable() {
        @Override public void run() {
            if (!isResumed() || waterModel == null) return;
            waterModel.refreshConsumption();
            refreshHandler.postDelayed(this, 60000);
        }
    };

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.home_background);
        android.widget.LinearLayout overview = view.findViewById(R.id.home_goal_overview);
        overview.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            float density = getResources().getDisplayMetrics().density;
            boolean compact = (right - left) / density < 320 || getResources().getConfiguration().fontScale > 1.25f;
            int orientation = compact ? android.widget.LinearLayout.VERTICAL : android.widget.LinearLayout.HORIZONTAL;
            if (overview.getOrientation() == orientation) return;
            overview.setOrientation(orientation);
            View ring = overview.findViewById(R.id.home_goal_progress_ring), goal = overview.findViewById(R.id.home_goal);
            android.widget.LinearLayout.LayoutParams ringParams = new android.widget.LinearLayout.LayoutParams(compact ? -1 : 0, Math.round(190 * density), compact ? 0 : 1);
            ringParams.setMarginEnd(compact ? 0 : Math.round(12 * density));
            ringParams.bottomMargin = compact ? Math.round(12 * density) : 0;
            ring.setLayoutParams(ringParams);
            goal.setLayoutParams(new android.widget.LinearLayout.LayoutParams(compact ? -1 : 0, compact ? -2 : -1, compact ? 0 : 1));
        });
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        waterModel = water;
        HomeViewModel home = new ViewModelProvider(this).get(HomeViewModel.class);
        homeModel = home;
        routineModel = new ViewModelProvider(this, new RoutineViewModel.Factory(requireContext())).get(RoutineViewModel.class);
        routineModel.state().observe(getViewLifecycleOwner(), stateValue -> {
            routineLoaded = stateValue.reading.status == ScreenState.Status.SUCCESS;
            todayRoutine = routineLoaded && stateValue.selectedDate.equals(java.time.LocalDate.now()) ? stateValue.day : null;
            renderTodayChart(temperatureChart, home.dashboard().getValue(), home.todayTemperature.getValue());
        });
        if (routineModel.state().getValue().reading.status == ScreenState.Status.IDLE) routineModel.load();
        profileModel = new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class);
        android.widget.ImageView avatar = view.findViewById(R.id.home_avatar);
        profileModel.photo.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                avatar.setPadding(0, 0, 0, 0);
                avatar.setImageURI(android.net.Uri.fromFile(value.data));
            } else if (value.status == ScreenState.Status.IDLE) {
                int padding = Math.round(9 * getResources().getDisplayMetrics().density);
                avatar.setPadding(padding, padding, padding, padding);
                avatar.setImageResource(R.drawable.home_icon_person);
            }
        });
        home.user.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) profileModel.loadPhoto(value.data, true);
        });
        setHomeLoading(true);
        selectedProperty = null;
        TextView apiStatus = view.findViewById(R.id.home_api_status);
        apiStatus.setText("");

        temperatureChart = view.findViewById(R.id.home_chart);
        temperatureChart.setPoints(Collections.emptyList());
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
                if (selectedProperty == null) {
                    home.selectProperty(null);
                    home.clearReadings();
                    apiStatus.setText(R.string.home_no_properties);
                    water.loadMeterStatus(null);
                } else {
                    home.selectProperty(selectedProperty);
                    home.loadTodayTemperature(selectedProperty);
                    water.loadMeterStatus(selectedProperty);
                    if (!Objects.equals(selectedProperty, water.propertyId) || water.consumption.getValue().status == ScreenState.Status.IDLE)
                        water.select(selectedProperty);
                }
            }
        });
        View addMeterBtn = view.findViewById(R.id.home_add_meter);
        if (addMeterBtn != null) {
            addMeterBtn.setOnClickListener(v -> navigate(R.id.qrScanFragment));
        }
        water.hasMeter.observe(getViewLifecycleOwner(), meterState -> {
            if (addMeterBtn == null) return;
            boolean confirmedMeter = water.isMeterStatusFor(selectedProperty) && meterState.status == ScreenState.Status.SUCCESS && Boolean.TRUE.equals(meterState.data);
            addMeterBtn.setVisibility(selectedProperty == null || confirmedMeter ? View.GONE : View.VISIBLE);
            if (water.isMeterStatusFor(selectedProperty) && meterState.status == ScreenState.Status.SUCCESS && !Boolean.TRUE.equals(meterState.data)
                && water.claimMeterPrompt(selectedProperty)) {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.home_add_meter_title)
                    .setMessage(R.string.home_add_meter_message)
                    .setPositiveButton(R.string.home_add_meter_confirm, (dialog, which) -> navigate(R.id.qrScanFragment))
                    .setNegativeButton(R.string.home_add_meter_cancel, null)
                    .show();
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
        });
        home.dashboard().observe(getViewLifecycleOwner(), summary -> {
            ((TextView) view.findViewById(R.id.home_today)).setText(liters(summary.todayLiters));
            ((TextView) view.findViewById(R.id.home_month)).setText(liters(summary.monthLiters));
            renderTodayChart(temperatureChart, summary, home.todayTemperature.getValue());
        });
        home.todayTemperature.observe(getViewLifecycleOwner(), value -> renderTodayChart(temperatureChart, home.dashboard().getValue(), value));
        home.cost.observe(getViewLifecycleOwner(), value -> {
            ((TextView) view.findViewById(R.id.home_cost)).setText(value.status == ScreenState.Status.SUCCESS && Objects.equals(value.data.propertyId, selectedProperty)
                && value.data.estimatedCost != null ? money(value.data.estimatedCost) : getString(R.string.home_pending_metric));
        });

        home.name().observe(getViewLifecycleOwner(), name -> ((TextView) view.findViewById(R.id.home_greeting)).setText(name == null || name.trim().isEmpty()
            ? getString(R.string.home_greeting_default) : getString(R.string.home_greeting, name.trim().split("\\s+")[0])));
        apiStatus.setOnClickListener(v -> {
                setHomeLoading(true);
            home.loadProfile();
            if (water.properties.getValue().status != ScreenState.Status.SUCCESS || selectedProperty == null) water.loadProperties();
            else water.select(selectedProperty);
        });
        view.findViewById(R.id.home_goal).setOnClickListener(v -> navigate(R.id.monthlyGoalsFragment));
        view.findViewById(R.id.home_settings).setOnClickListener(v -> navigate(R.id.profileSettingsFragment));
        view.findViewById(R.id.home_avatar_action).setOnClickListener(v -> navigate(R.id.profileSettingsFragment));
        if (water.properties.getValue().status == ScreenState.Status.IDLE) water.loadProperties();
        home.loadProfile();
        com.aula.delta_mobile_app.view.MonthlyGoalCard.bind(this, view, home, water);

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
                setHomeLoading(!ready);
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
                setHomeLoading(true);
            selectedProperty = id;
            home.selectProperty(id);
            home.loadTodayTemperature(id);
            water.select(id);
            water.loadMeterStatus(id);
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void setHomeLoading(boolean loading) { showLoading(loading); }

    private void renderTodayChart(HomeTemperatureChart chart, HomeDashboard summary, ScreenState<List<Double>> temperature) {
        if (chart == null || temperature == null || temperature.status != ScreenState.Status.SUCCESS || !routineLoaded) {
            chart.setPoints(Collections.emptyList());
            return;
        }
        double[] hourlyWeights = new double[24];
        double totalRoutineEstimate = 0;
        if (todayRoutine != null) for (RoutineData.Execution execution : todayRoutine.executions)
            if (execution.liters != null && execution.liters.signum() > 0) {
                double liters = execution.liters.doubleValue();
                hourlyWeights[execution.startTime.getHour()] += liters;
                totalRoutineEstimate += liters;
            }
        double dailyLiters = summary != null && summary.hasTodayReading && summary.todayLiters != null ? summary.todayLiters.doubleValue() : totalRoutineEstimate;
        List<HomeTemperatureChart.Point> points = new ArrayList<>();
        for (int hour = 0; hour < hourlyWeights.length; hour++)
            if (temperature.data.size() > hour && temperature.data.get(hour) != null)
                points.add(new HomeTemperatureChart.Point(hour, hourlyWeights[hour] > 0 && totalRoutineEstimate > 0
                    ? dailyLiters * hourlyWeights[hour] / totalRoutineEstimate : Double.NaN, temperature.data.get(hour)));
        chart.setPoints(points);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.removeCallbacks(refreshConsumption);
        refreshConsumption.run();
        if (waterModel != null && selectedProperty != null) waterModel.loadMeterStatus(selectedProperty);
        if (homeModel != null && selectedProperty != null) homeModel.loadTodayTemperature(selectedProperty);
        if (homeModel != null && profileModel != null && homeModel.user.getValue().status == ScreenState.Status.SUCCESS)
            profileModel.loadPhoto(homeModel.user.getValue().data, true);
    }

    @Override
    public void onPause() {
        refreshHandler.removeCallbacks(refreshConsumption);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        setHomeLoading(false);
        loadingCheckPosted = false;
        homeModel = null;
        profileModel = null;
        routineModel = null;
        temperatureChart = null;
        todayRoutine = null;
        routineLoaded = false;
        waterModel = null;
        refreshHandler.removeCallbacks(refreshConsumption);
        super.onDestroyView();
    }

    private String liters(BigDecimal value) {
        if (value == null) return getString(R.string.home_pending_metric);
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(0);
        format.setRoundingMode(java.math.RoundingMode.FLOOR);
        return getString(R.string.home_liters, format.format(value));
    }

    private String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        format.setRoundingMode(java.math.RoundingMode.FLOOR);
        return format.format(value).replace("\u00a0", "").replace(" ", "");
    }
}
