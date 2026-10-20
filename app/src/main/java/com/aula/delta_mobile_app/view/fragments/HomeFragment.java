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
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.HomeEfficiencyView;
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

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.home_background);
        WaterViewModel water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        HomeViewModel home = new ViewModelProvider(this).get(HomeViewModel.class);
        view.setVisibility(View.INVISIBLE);
        setHomeLoading(true);
        selectedProperty = null;
        TextView apiStatus = view.findViewById(R.id.home_api_status);
        apiStatus.setText("");

        ((HomeEfficiencyView) view.findViewById(R.id.home_efficiency_gauge)).setScore(null, null);
        ((HomeTemperatureChart) view.findViewById(R.id.home_chart)).setPoints(Collections.emptyList());
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
        });
        home.dashboard().observe(getViewLifecycleOwner(), summary -> {
            ((TextView) view.findViewById(R.id.home_today)).setText(liters(summary.todayLiters));
            ((TextView) view.findViewById(R.id.home_month)).setText(liters(summary.monthLiters));
        });
        home.cost.observe(getViewLifecycleOwner(), value -> {
            ((TextView) view.findViewById(R.id.home_cost)).setText(value.status == ScreenState.Status.SUCCESS && Objects.equals(value.data.propertyId, selectedProperty)
                && value.data.estimatedCost != null ? money(value.data.estimatedCost) : getString(R.string.home_pending_metric));
        });

        home.name().observe(getViewLifecycleOwner(), name -> ((TextView) view.findViewById(R.id.home_greeting)).setText(name == null || name.trim().isEmpty()
            ? getString(R.string.home_greeting_default) : getString(R.string.home_greeting, name.trim().split("\\s+")[0])));
        apiStatus.setOnClickListener(v -> {
            view.setVisibility(View.INVISIBLE);
            setHomeLoading(true);
            home.loadProfile();
            if (water.properties.getValue().status != ScreenState.Status.SUCCESS || selectedProperty == null) water.loadProperties();
            else water.select(selectedProperty);
        });
        view.findViewById(R.id.home_goal).setOnClickListener(v -> navigate(R.id.monthlyGoalsFragment));
        view.findViewById(R.id.home_goal_suggestion).setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.home_suggestion)
            .setMessage(R.string.home_suggestion_unavailable).setPositiveButton(android.R.string.ok, null).show());
        view.findViewById(R.id.home_notifications).setOnClickListener(v -> Toast.makeText(requireContext(), R.string.home_notifications_empty, Toast.LENGTH_SHORT).show());
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
            view.setVisibility(View.INVISIBLE);
            setHomeLoading(true);
            selectedProperty = id;
            home.selectProperty(id);
            water.select(id);
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void setHomeLoading(boolean loading) {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) requireActivity()).setHomeLoading(loading);
    }

    @Override
    public void onDestroyView() {
        setHomeLoading(false);
        loadingCheckPosted = false;
        super.onDestroyView();
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
