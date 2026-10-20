package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aula.delta_mobile_app.R;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignupProfileFragment extends AuthFlowFragment {
    public SignupProfileFragment() {
        super(R.layout.fragment_signup_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 0, R.string.auth_step0_title, R.string.auth_step0_subtitle, R.string.auth_step0_description, v -> backToLogin());
        EditText name = view.findViewById(R.id.full_name);
        EditText date = view.findViewById(R.id.birth_date);
        EditText phone = view.findViewById(R.id.phone);
        restore(name, "name");
        restore(date, "birthDate");
        restore(phone, "phone");
        date.setOnClickListener(v -> {
            if (getChildFragmentManager().findFragmentByTag("birth_date_picker") != null)
                return;
            long latestBirthDate = com.aula.delta_mobile_app.viewmodel.SignupViewModel.latestAllowedBirthDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                .toEpochMilli();
            Long selected = null;
            try {
                selected = java.time.LocalDate.parse(text(date), java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")).atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant().toEpochMilli();
            } catch (RuntimeException ignored) {
            }
            long openAt = selected != null && selected <= latestBirthDate ? selected : latestBirthDate;
            CalendarConstraints constraints = new CalendarConstraints.Builder().setEnd(latestBirthDate).setOpenAt(openAt).setValidator(
                DateValidatorPointBackward.before(latestBirthDate)).build();
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker().setTitleText("Data de nascimento")
                .setTheme(R.style.ThemeOverlay_Delta_Calendar).setCalendarConstraints(constraints).setPositiveButtonText(R.string.auth_confirm)
                .setNegativeButtonText(R.string.auth_cancel);
            if (selected != null && selected <= latestBirthDate)
                builder.setSelection(selected);
            MaterialDatePicker<Long> picker = builder.build();
            bindDatePicker(picker, date);
            picker.show(getChildFragmentManager(), "birth_date_picker");
        });
        androidx.fragment.app.Fragment restoredPicker = getChildFragmentManager().findFragmentByTag("birth_date_picker");
        if (restoredPicker instanceof MaterialDatePicker)
            bindDatePicker((MaterialDatePicker<Long>) restoredPicker, date);
        TextView next = view.findViewById(R.id.step_continue);
        watchButton(next, name, date);
        next.setOnClickListener(v -> {
            String validation = registration().personalError();
            if (validation != null) {
                name.setError(validation);
                return;
            }
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(text(date), position);
            if (parsed == null || position.getIndex() != text(date).length() || !parsed.before(new Date())) {
                date.setError(getString(R.string.auth_invalid_date));
                date.requestFocus();
                return;
            }
            save(name, "name");
            save(date, "birthDate");
            save(phone, "phone");
            navigate(R.id.signupAccountFragment);
        });
    }

    private void bindDatePicker(MaterialDatePicker<Long> picker, EditText date) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(value -> date.setText(java.time.Instant.ofEpochMilli(value).atZone(java.time.ZoneOffset.UTC).toLocalDate().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))));
    }
}
