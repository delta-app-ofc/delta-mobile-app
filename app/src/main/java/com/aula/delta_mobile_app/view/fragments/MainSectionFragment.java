package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.HomeDashboard;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.view.HomeConsumptionChart;
import com.aula.delta_mobile_app.viewmodel.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

public class MainSectionFragment extends AuthFlowFragment {
    private LinearLayout cards;
    private TextView status;
    private String screen;
    private WaterViewModel water;
    private HomeViewModel home;
    private SectionsViewModel sections;
    private final Locale locale = new Locale("pt", "BR");

    public MainSectionFragment() {
        super(R.layout.fragment_main_section);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        setScreenColor(R.color.home_background);
        screen = requireArguments().getString("screen", "consumption");
        cards = view.findViewById(R.id.section_cards);
        status = view.findViewById(R.id.section_status);
        water = new ViewModelProvider(requireActivity()).get(WaterViewModel.class);
        home = new ViewModelProvider(this).get(HomeViewModel.class);
        sections = new ViewModelProvider(this).get(SectionsViewModel.class);
        ((TextView) view.findViewById(R.id.section_title)).setText(requireArguments().getString("title"));
        TextView subtitle = view.findViewById(R.id.section_subtitle);
        Spinner property = view.findViewById(R.id.section_property);
        View retry = view.findViewById(R.id.section_retry);
        boolean readings = screen.equals("consumption") || screen.equals("costs");
        property.setVisibility(readings ? View.VISIBLE : View.GONE);
        if (readings) {
            subtitle.setText(screen.equals("costs") ? "Custo previsto e histórico de contas" : "Acompanhe as leituras do seu imóvel");
            water.properties.observe(getViewLifecycleOwner(), value -> {
                property.setEnabled(value.status == ScreenState.Status.SUCCESS && !value.data.isEmpty());
                if (value.status != ScreenState.Status.SUCCESS) {
                    show(value.displayMessage());
                    return;
                }
                if (value.data.isEmpty()) {
                    show(getString(R.string.home_no_properties));
                    return;
                }
                List<UserPropertyResponse> links = value.data;
                List<String> labels = new ArrayList<>();
                int selected = 0;
                for (int i = 0; i < links.size(); i++) {
                    labels.add(links.get(i).propertyName == null ? "Imóvel " + links.get(i).propertyId : links.get(i).propertyName);
                    if (Objects.equals(links.get(i).propertyId, water.propertyId))
                        selected = i;
                }
                property.setOnItemSelectedListener(null);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.auth_spinner_item, labels);
                adapter.setDropDownViewResource(R.layout.auth_spinner_item);
                property.setAdapter(adapter);
                property.setSelection(selected);
                property.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View row, int position, long id) {
                        Integer selectedId = links.get(position).propertyId;
                        if (selectedId == null) return;
                        home.selectProperty(selectedId);
                        if (!Objects.equals(water.propertyId, selectedId) || water.consumption.getValue().status == ScreenState.Status.IDLE)
                            water.select(selectedId);
                        else renderReadings(water.consumption.getValue());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {}
                });
            });
            if (water.propertyId != null) home.selectProperty(water.propertyId);
            water.consumption.observe(getViewLifecycleOwner(), this::renderReadings);
            home.cost.observe(getViewLifecycleOwner(), value -> {
                if (screen.equals("costs")) renderCosts();
            });
            sections.bills.observe(getViewLifecycleOwner(), value -> {
                if (screen.equals("costs")) renderCosts();
            });
            retry.setOnClickListener(v -> {
                if (water.propertyId != null) water.select(water.propertyId);
                else water.loadProperties();
                if (screen.equals("costs")) sections.loadBills();
            });
            if (water.properties.getValue().status == ScreenState.Status.IDLE)
                water.loadProperties();
            if (screen.equals("costs")) sections.loadBills();
        } else if (screen.equals("ranking")) {
            subtitle.setText("Compare a eficiência das suas unidades");
            sections.rankings.observe(getViewLifecycleOwner(), value -> {
                cards.removeAllViews();
                show(value.displayMessage());
                if (value.status != ScreenState.Status.SUCCESS) return;
                if (value.data.isEmpty())
                    card("Ainda não há dados de ranking para suas unidades.", false);
                for (PropertyRankingResponse row : value.data)
                    card((row.rankEfficiency == null ? "—" : row.rankEfficiency + "º") + " · " + (row.propertyName == null ? "Imóvel " + row.propertyId : row.propertyName)
                    + "\nConsumo total: " + number(row.totalLiters) + " L\nCusto total: " + money(row.totalCost) + "\nConsumo por área: " + number(row.litersPerM2) + " L/m²",
                    false);
            });
            retry.setOnClickListener(v -> sections.loadRankings());
            sections.loadRankings();
        } else {
            retry.setVisibility(View.GONE);
            subtitle.setText(screen.equals("chat") ? "Monitore cada gota com mais precisão." : "Planeje seu consumo de água");
            card(screen.equals("chat") ? "O Gluh Helper ainda não está conectado. Acompanhe seu consumo e" + " organize seus hábitos na Rotina."
                : "As metas mensais ainda não estão disponíveis para esta conta.", false);
            action("Organizar minha rotina", () -> navigate(R.id.goalsFragment));
        }
    }

    private void renderReadings(ScreenState<List<ConsumptionDailyResponse>> value) {
        if (value.status == ScreenState.Status.SUCCESS) home.update(value.data);
        else if (value.status != ScreenState.Status.IDLE) home.clearReadings();
        if (screen.equals("costs")) {
            renderCosts();
            return;
        }
        cards.removeAllViews();
        show(value.displayMessage());
        if (value.status != ScreenState.Status.SUCCESS) {
            leakEntry();
            return;
        }
        List<ConsumptionDailyResponse> readings = new ArrayList<>();
        for (ConsumptionDailyResponse row : value.data)
            if (row != null && Objects.equals(row.propertyId, water.propertyId)) readings.add(row);
        HomeDashboard summary = HomeDashboard.from(readings, LocalDate.now());
        card("Consumo hoje\n" + number(summary.todayLiters) + " L\n\nConsumo este mês\n" + number(summary.monthLiters) + " L", true);
        card("Evolução do consumo · litros", false);
        HomeConsumptionChart chart = new HomeConsumptionChart(requireContext(), null);
        chart.setReadings(summary.history);
        cards.addView(chart, new LinearLayout.LayoutParams(-1, dp(180)));
        for (int i = summary.history.size() - 1; i >= 0; i--) {
            ConsumptionDailyResponse row = summary.history.get(i);
            card(date(row.fullDate) + "\n" + number(row.totalLiters) + " L", false);
        }
        leakEntry();
    }

    private void leakEntry() {
        TextView entry = card(getString(R.string.leak_entry) + "\n" + getString(R.string.leak_entry_description) + "  →", false);
        entry.setClickable(true);
        entry.setFocusable(true);
        entry.setContentDescription(getString(R.string.leak_open));
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true))
            entry.setForeground(androidx.core.content.ContextCompat.getDrawable(requireContext(), ripple.resourceId));
        entry.setOnClickListener(v -> {
            android.os.Bundle args = new android.os.Bundle();
            args.putInt("propertyId", water.propertyId == null ? 0 : water.propertyId);
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.leakDetectionFragment, args);
        });
    }

    private void renderCosts() {
        if (cards == null) return;
        cards.removeAllViews();
        List<String> messages = new ArrayList<>();
        ScreenState<WaterCostResponse> cost = home.cost.getValue();
        card("Custo previsto\n" + (cost.status == ScreenState.Status.SUCCESS && Objects.equals(cost.data.propertyId, water.propertyId) ? money(cost.data.estimatedCost) : "—"),
            true);
        if (cost.status == ScreenState.Status.SUCCESS)
            card("Referência: " + date(cost.data.referenceDate) + "\nConsumo tarifado: " + number(cost.data.consumptionM3) + " m³\nTarifa: " + money(cost.data.ratePerM3) + "/m³",
            false);
        if (!cost.displayMessage().isEmpty()) messages.add(cost.displayMessage());
        ScreenState<List<ConsumptionDailyResponse>> readings = water.consumption.getValue();
        if (readings.status != ScreenState.Status.SUCCESS && !readings.displayMessage().isEmpty())
            messages.add(readings.displayMessage());
        action("Cadastrar conta de água", () -> navigate(R.id.accountConfigFragment));
        action("Comparar minhas unidades", () -> navigate(R.id.rankingFragment));
        card("Histórico de contas da sua conta", false);
        ScreenState<List<LastWaterBillResponse>> bills = sections.bills.getValue();
        if (bills.status == ScreenState.Status.SUCCESS) {
            if (bills.data.isEmpty()) card("Nenhuma conta cadastrada.", false);
            for (LastWaterBillResponse row : bills.data)
                card(date(row.month) + "\n" + money(row.totalValue) + " · " + number(row.m3Value) + " m³", false);
        } else if (!bills.displayMessage().isEmpty()) messages.add(bills.displayMessage());
        show(String.join("\n", messages));
    }

    private TextView card(String text, boolean blue) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextColor(0xFFF3F4F5);
        label.setTextSize(16);
        label.setPadding(dp(16), dp(16), dp(16), dp(16));
        label.setBackgroundResource(blue ? R.drawable.home_summary_background : R.drawable.home_outline_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(16);
        cards.addView(label, params);
        return label;
    }

    private void action(String text, Runnable onClick) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        cards.addView(button, params);
        button.setOnClickListener(v -> onClick.run());
    }

    private void show(String message) {
        status.setText(message);
        status.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String number(BigDecimal value) {
        return value == null ? "—" : NumberFormat.getNumberInstance(locale).format(value);
    }

    private String money(BigDecimal value) {
        return value == null ? "—" : NumberFormat.getCurrencyInstance(locale).format(value);
    }

    private String date(String value) {
        try {
            return LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException invalid) {
            return "—";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        cards = null;
        status = null;
        super.onDestroyView();
    }
}
