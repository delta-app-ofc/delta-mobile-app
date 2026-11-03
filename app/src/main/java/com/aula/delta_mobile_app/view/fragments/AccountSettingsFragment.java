package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class AccountSettingsFragment extends AuthFlowFragment {
    private final Calendar month = Calendar.getInstance();

    public AccountSettingsFragment() {
        super(R.layout.fragment_account_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setScreenColor(R.color.auth_blue);
        month.set(Calendar.DAY_OF_MONTH, 1);
        if (flow().containsKey("referenceYear")) {
            month.set(Calendar.YEAR, flow().getInt("referenceYear"));
            month.set(Calendar.MONTH, flow().getInt("referenceMonth"));
        }
        Calendar current = Calendar.getInstance();
        if (month.get(Calendar.YEAR) > current.get(Calendar.YEAR) || (month.get(Calendar.YEAR) == current.get(Calendar.YEAR)
            && month.get(Calendar.MONTH) > current.get(Calendar.MONTH))) {
            month.set(Calendar.YEAR, current.get(Calendar.YEAR));
            month.set(Calendar.MONTH, current.get(Calendar.MONTH));
            saveMonth();
        }
        TextView monthText = view.findViewById(R.id.reference_month);
        View nextMonth = view.findViewById(R.id.month_next);
        updateMonth(monthText, nextMonth);
        view.findViewById(R.id.month_previous).setOnClickListener(v -> {
            month.add(Calendar.MONTH, -1);
            updateMonth(monthText, nextMonth);
            saveMonth();
        });
        nextMonth.setOnClickListener(v -> {
            if (!canAdvanceMonth()) return;
            month.add(Calendar.MONTH, 1);
            updateMonth(monthText, nextMonth);
            saveMonth();
        });
        EditText paid = view.findViewById(R.id.total_paid);
        EditText consumption = view.findViewById(R.id.consumption);
        restore(paid, "totalPaid");
        restore(consumption, "consumption");
        com.aula.delta_mobile_app.viewmodel.WaterViewModel water = new androidx.lifecycle.ViewModelProvider(requireActivity())
            .get(com.aula.delta_mobile_app.viewmodel.WaterViewModel.class);
        TextView status = addStatus(view);
        View next = view.findViewById(R.id.config_continue);
        water.bill.observe(getViewLifecycleOwner(), value -> {
            showStatus(status, next, value);
            paid.setEnabled(!value.busy());
            consumption.setEnabled(!value.busy());
            view.findViewById(R.id.month_previous).setEnabled(!value.busy());
            nextMonth.setEnabled(!value.busy() && canAdvanceMonth());
            if (value.status == com.aula.delta_mobile_app.model.ScreenState.Status.SUCCESS) {
                water.consumeBill();
                Toast.makeText(requireContext(), "Conta salva com sucesso.", Toast.LENGTH_LONG).show();
                openAuthenticated();
            }
        });
        view.findViewById(R.id.config_back).setOnClickListener(v -> back());
        view.findViewById(R.id.config_continue).setOnClickListener(v -> {
            save(paid, "totalPaid");
            save(consumption, "consumption");
            saveMonth();
            water.saveBill(java.time.LocalDate.of(month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, 1).toString(), text(paid), text(consumption));
        });
    }

    private boolean canAdvanceMonth() {
        Calendar current = Calendar.getInstance();
        return month.get(Calendar.YEAR) < current.get(Calendar.YEAR) || (month.get(Calendar.YEAR) == current.get(Calendar.YEAR)
            && month.get(Calendar.MONTH) < current.get(Calendar.MONTH));
    }

    private void updateMonth(TextView monthText, View nextMonth) {
        String name = new DateFormatSymbols(new Locale("pt", "BR")).getMonths()[month.get(Calendar.MONTH)];
        monthText.setText(Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.get(Calendar.YEAR));
        boolean canAdvance = canAdvanceMonth();
        nextMonth.setEnabled(canAdvance);
        nextMonth.setAlpha(canAdvance ? 1f : 0.35f);
    }

    private void saveMonth() {
        flow().putInt("referenceYear", month.get(Calendar.YEAR));
        flow().putInt("referenceMonth", month.get(Calendar.MONTH));
    }
}
