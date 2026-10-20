package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.*;
import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.HomeDashboard;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.repository.SignupOptions;
import com.aula.delta_mobile_app.view.HomeConsumptionChart;
import com.aula.delta_mobile_app.viewmodel.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private android.app.Dialog editor;
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
        } else if (screen.equals("routine")) {
            subtitle.setText("Monitore e otimize sua rotina hídrica");
            sections.habits.observe(getViewLifecycleOwner(), this::renderRoutine);
            sections.mutation.observe(getViewLifecycleOwner(), value -> {
                show(value.status == ScreenState.Status.SUCCESS ? "Rotina atualizada." : value.displayMessage());
                retry.setEnabled(!value.busy());
            });
            retry.setOnClickListener(v -> sections.loadHabits());
            sections.loadHabits();
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

    private void renderRoutine(ScreenState<List<UserHabitResponse>> value) {
        cards.removeAllViews();
        show(value.displayMessage());
        if (value.status == ScreenState.Status.SUCCESS) {
            if (value.data.isEmpty()) {
                ImageView empty = new ImageView(requireContext());
                empty.setImageResource(R.drawable.figma_routine_empty);
                empty.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(152), dp(152));
                params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                params.topMargin = dp(40);
                cards.addView(empty, params);
                card("Sua rotina está livre", false).setGravity(android.view.Gravity.CENTER);
            }
            SignupOptions catalog = SignupOptions.load(requireContext());
            for (UserHabitResponse habit : value.data) {
                String name = habit.habitName;
                if (name == null)
                    for (SignupOptions.Entry item : catalog.habits)
                        if (Objects.equals(habit.habitId, item.id)) name = item.label;
                List<String> days = new ArrayList<>();
                if (habit.daysOfWeek != null)
                    for (SignupOptions.Entry day : catalog.days)
                        if (habit.daysOfWeek.contains(day.id)) days.add(day.label);
                card((name == null ? "Hábito" : name) + "\n" + (habit.frequency == null ? "—" : habit.frequency) + " vezes/semana\n" + String.join(", ", days), true);
                if (habit.id != null) {
                    action("Editar hábito", () -> editHabit(habit));
                    action("Excluir hábito", () -> new MaterialAlertDialogBuilder(requireContext()).setMessage("Excluir este hábito da sua rotina?")
                        .setNegativeButton("Cancelar", null).setPositiveButton("Excluir", (dialog, which) -> sections.deleteHabit(habit.id)).show());
                }
            }
        }
        action("Adicionar hábito", () -> editHabit(null));
        card("Os hábitos são salvos na sua conta. O agendamento por horário ainda não está" + " disponível.", false);
    }

    private void editHabit(UserHabitResponse existing) {
        if (sections.mutation.getValue().busy()) return;
        SignupOptions catalog = SignupOptions.load(requireContext());
        if (catalog.habits.isEmpty() || catalog.days.isEmpty()) {
            show("Catálogo de hábitos indisponível.");
            return;
        }
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(12), dp(24), 0);
        Spinner habit = new Spinner(requireContext());
        List<String> labels = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < catalog.habits.size(); i++) {
            labels.add(catalog.habits.get(i).label);
            if (existing != null && Objects.equals(existing.habitId, catalog.habits.get(i).id))
                selected = i;
        }
        habit.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
        habit.setSelection(selected);
        habit.setContentDescription("Hábito");
        form.addView(habit);
        EditText frequency = new EditText(requireContext());
        frequency.setHint("Frequência por semana");
        frequency.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (existing != null && existing.frequency != null)
            frequency.setText(existing.frequency.toString());
        form.addView(frequency);
        List<CheckBox> choices = new ArrayList<>();
        for (SignupOptions.Entry day : catalog.days) {
            CheckBox checkbox = new CheckBox(requireContext());
            checkbox.setText(day.label);
            checkbox.setChecked(existing != null && existing.daysOfWeek != null && existing.daysOfWeek.contains(day.id));
            form.addView(checkbox);
            choices.add(checkbox);
        }
        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(form);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(existing == null ? "Adicionar hábito" : "Editar hábito")
            .setView(scroll).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            int times;
            try {
                times = Integer.parseInt(frequency.getText().toString().trim());
                if (times <= 0) throw new NumberFormatException();
            } catch (NumberFormatException invalid) {
                frequency.setError("Informe uma frequência positiva.");
                return;
            }
            List<Integer> days = new ArrayList<>();
            for (int i = 0; i < choices.size(); i++)
                if (choices.get(i).isChecked())
                    days.add(catalog.days.get(i).id);
            if (days.isEmpty()) {
                choices.get(0).setError("Selecione pelo menos um dia.");
                return;
            }
            sections.saveHabit(existing == null ? null : existing.id, catalog.habits.get(habit.getSelectedItemPosition()).id, times, days);
            dialog.dismiss();
        }));
        editor = dialog;
        dialog.show();
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
        if (editor != null) {
            editor.dismiss();
            editor = null;
        }
        cards = null;
        status = null;
        super.onDestroyView();
    }
}
