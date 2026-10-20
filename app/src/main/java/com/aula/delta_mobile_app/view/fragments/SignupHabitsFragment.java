package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AuthModels.Habit;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.*;

public class SignupHabitsFragment extends AuthFlowFragment {
    private static final int[] OPTIONS = {
        R.id.routine_shower, R.id.routine_yard, R.id.routine_clothes, R.id.routine_plants, R.id.routine_car, R.id.routine_dishes
    };
    private static final String[] KEYS = {"shower", "yard", "clothes", "plants", "car", "dishes"};

    public SignupHabitsFragment() {
        super(R.layout.fragment_signup_habits);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupHeader(view, 2, R.string.auth_step2_title, R.string.auth_step2_subtitle, R.string.auth_step_description);
        for (int i = 0; i < OPTIONS.length; i++) {
            TextView card = view.findViewById(OPTIONS[i]);
            String key = KEYS[i];
            card.setSelected(registration().habit(key) != null);
            card.setOnClickListener(v -> configure(card, key));
            card.setOnLongClickListener(v -> {
                configure(card, key);
                return true;
            });
        }
        view.findViewById(R.id.step_continue).setOnClickListener(v -> navigate(R.id.signupPropertyFragment));
    }

    private void configure(TextView card, String key) {
        if (registration().catalog.habit(key) == null || registration().catalog.days.isEmpty()) {
            Toast.makeText(requireContext(), "Este hábito ainda não está disponível no catálogo da plataforma.", Toast.LENGTH_LONG).show();
            return;
        }
        Habit existing = registration().habit(key);
        android.content.Context context = SelectionHelper.context(requireContext());
        LinearLayout content = SelectionHelper.content(context, card.getText().toString(), "Personalize sua rotina de consumo de água.");
        TextInputLayout frequencyField = new TextInputLayout(context);
        frequencyField.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        int fieldRadius = SelectionHelper.dp(context, 16);
        frequencyField.setBoxCornerRadii(fieldRadius, fieldRadius, fieldRadius, fieldRadius);
        frequencyField.setHint("Vezes por semana");
        frequencyField.setHelperText("Informe quantas vezes você realiza essa atividade.");
        TextInputEditText frequency = new TextInputEditText(context);
        frequency.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (existing != null) frequency.setText(String.valueOf(existing.frequency));
        frequencyField.addView(frequency, new LinearLayout.LayoutParams(-1, -2));
        content.addView(frequencyField, new LinearLayout.LayoutParams(-1, -2));
        TextView daysLabel = SelectionHelper.label(context, "Em quais dias?");
        daysLabel.setPadding(0, SelectionHelper.dp(context, 20), 0, SelectionHelper.dp(context, 8));
        content.addView(daysLabel);
        ChipGroup daysGroup = new ChipGroup(context);
        daysGroup.setChipSpacingHorizontal(SelectionHelper.dp(context, 8));
        daysGroup.setChipSpacingVertical(SelectionHelper.dp(context, 4));
        List<Chip> chips = new ArrayList<>();
        for (com.aula.delta_mobile_app.repository.SignupOptions.Entry day : registration().catalog.days) {
            Chip chip = SelectionHelper.chip(context, day.label.split("-")[0]);
            chip.setTag(day.id);
            chip.setContentDescription(day.label);
            chip.setChecked(existing != null && existing.daysOfWeek.contains(day.id));
            daysGroup.addView(chip);
            chips.add(chip);
        }
        content.addView(daysGroup);
        TextView errorLabel = SelectionHelper.label(context, "");
        errorLabel.setTextColor(0xFFBA1A1A);
        errorLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(errorLabel);
        MaterialButton confirm = new MaterialButton(context);
        confirm.setText(R.string.auth_confirm);
        confirm.setCornerRadius(SelectionHelper.dp(context, 18));
        LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(-1, SelectionHelper.dp(context, 56));
        buttonLayout.topMargin = SelectionHelper.dp(context, 16);
        content.addView(confirm, buttonLayout);
        MaterialButton cancel = new MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        cancel.setText(R.string.auth_cancel);
        content.addView(cancel, new LinearLayout.LayoutParams(-1, -2));
        MaterialButton remove = new MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        if (existing != null) {
            remove.setText("Remover hábito");
            content.addView(remove, new LinearLayout.LayoutParams(-1, -2));
        }
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = SelectionHelper.show(this, context, content);
        confirm.setOnClickListener(v -> {
            int count;
            try {
                count = Integer.parseInt(frequency.getText().toString());
            } catch (NumberFormatException failure) {
                count = 0;
            }
            List<Integer> days = new ArrayList<>();
            for (Chip chip : chips) if (chip.isChecked()) days.add((Integer) chip.getTag());
            String error = registration().setHabit(key, count, days);
            if (error != null) {
                errorLabel.setText(error);
                return;
            }
            card.setSelected(true);
            card.setContentDescription(card.getText() + ", selecionado, " + count + " vezes por semana");
            dialog.dismiss();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        remove.setOnClickListener(v -> {
            registration().removeHabit(key);
            card.setSelected(false);
            card.setContentDescription(card.getText());
            dialog.dismiss();
        });
    }
}
