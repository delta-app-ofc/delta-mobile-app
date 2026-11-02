package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.*;

public final class UnitsFragment extends AuthFlowFragment implements ProfileNavigationGuard {
    private UnitsViewModel model;
    private WaterViewModel water;

    public UnitsFragment() { super(R.layout.fragment_units); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.profile_background);
        model = new ViewModelProvider(requireActivity()).get(UnitsViewModel.class);
        water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        view.findViewById(R.id.units_back).setOnClickListener(v -> leaveProfile(this::back));
        view.findViewById(R.id.units_add).setOnClickListener(v -> { model.beginNew(); navigate(R.id.newUnitFragment); });
        view.findViewById(R.id.units_notice).setOnClickListener(v -> model.load());
        model.units.observe(getViewLifecycleOwner(), value -> render(view));
        water.properties.observe(getViewLifecycleOwner(), value -> render(view));
        model.deletion.observe(getViewLifecycleOwner(), value -> {
            if (value.status == ScreenState.Status.SUCCESS) {
                int deleted = value.data;
                model.consumeDeletion();
                if (Objects.equals(water.propertyId, deleted)) water.reset();
                water.loadProperties();
                model.load();
            } else render(view);
        });
    }

    @Override public void onResume() { super.onResume(); if (model != null) model.load(); }

    private void render(View view) {
        if (getView() != view) return;
        ScreenState<List<UnitSummary>> loaded = model.units.getValue();
        ScreenState<Integer> deletion = model.deletion.getValue();
        boolean busy = loaded.busy() || model.busy();
        showLoading(loaded.status == ScreenState.Status.IDLE || busy);
        view.findViewById(R.id.units_add).setEnabled(!busy);
        TextView notice = view.findViewById(R.id.units_notice);
        notice.setText(deletion.status == ScreenState.Status.ERROR ? deletion.message : loaded.status == ScreenState.Status.ERROR ? loaded.message
            : loaded.status == ScreenState.Status.SUCCESS && loaded.data.isEmpty() ? getString(R.string.units_empty) : "");
        LinearLayout cards = view.findViewById(R.id.units_cards);
        cards.removeAllViews();
        if (loaded.status != ScreenState.Status.SUCCESS) return;
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(2);
        for (UnitSummary unit : loaded.data) {
            View card = getLayoutInflater().inflate(R.layout.item_unit, cards, false);
            String name = unit.property.propertyName == null || unit.property.propertyName.trim().isEmpty() ? getString(R.string.goal_property, unit.property.propertyId) : unit.property.propertyName;
            ((TextView) card.findViewById(R.id.unit_name)).setText(name);
            String city = unit.address == null || unit.address.city == null ? "" : unit.address.city.trim();
            String state = unit.address == null || unit.address.state == null ? "" : unit.address.state.trim();
            ((TextView) card.findViewById(R.id.unit_location)).setText(city.isEmpty() ? getString(R.string.units_city_unavailable) : state.isEmpty() ? city : getString(R.string.units_location, city, state));
            ((TextView) card.findViewById(R.id.unit_consumption)).setText(unit.monthlyLiters == null ? getString(R.string.units_volume_unavailable) : getString(R.string.units_monthly_volume, format.format(unit.monthlyLiters)));
            boolean selected = Objects.equals(water.propertyId, unit.property.propertyId);
            card.setBackgroundResource(selected ? R.drawable.units_card_selected : R.drawable.units_card_background);
            TextView select = card.findViewById(R.id.unit_select);
            select.setText(selected ? R.string.units_current : R.string.units_select);
            card.setEnabled(!busy);
            card.setSelected(selected);
            card.setOnClickListener(v -> {
                if (busy || selected) return;
                water.select(unit.property.propertyId);
                new ViewModelProvider(requireActivity()).get(ProfileSettingsViewModel.class).loadResidence(unit.property.propertyId, true);
                render(view);
            });
            card.findViewById(R.id.unit_delete).setEnabled(!busy);
            card.findViewById(R.id.unit_delete).setOnClickListener(v -> confirmDelete(unit, name));
            cards.addView(card);
        }
    }

    private void confirmDelete(UnitSummary unit, String name) {
        if (model.busy()) return;
        View content = getLayoutInflater().inflate(R.layout.dialog_delete_unit, null, false);
        ((TextView) content.findViewById(R.id.units_delete_message)).setText(getString(R.string.units_delete_message, name));
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(content).create();
        content.findViewById(R.id.units_delete_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.units_delete_confirm).setOnClickListener(v -> { dialog.dismiss(); model.delete(unit.property.propertyId); });
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    @Override public void leaveProfile(Runnable action) { if (!model.busy()) action.run(); }
    @Override public void onDestroyView() { model = null; water = null; super.onDestroyView(); }
}
