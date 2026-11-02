package com.aula.delta_mobile_app.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AuthModels.Habit;
import com.aula.delta_mobile_app.model.AuthModels.HabitSchedule;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.LocalTime;
import java.util.*;

public class SignupHabitsFragment extends AuthFlowFragment {
    private static final String TIME_PICKER = "signup_habit_time";
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
        LinearLayout content = SelectionHelper.content(context, card.getText().toString(), getString(R.string.auth_habit_description));
        TextView daysLabel = SelectionHelper.label(context, getString(R.string.auth_habit_when));
        daysLabel.setPadding(0, SelectionHelper.dp(context, 20), 0, SelectionHelper.dp(context, 8));
        content.addView(daysLabel);
        TextView daysHelp = SelectionHelper.label(context, getString(R.string.auth_habit_days_help));
        daysHelp.setTextSize(14);
        content.addView(daysHelp);
        ChipGroup daysGroup = new ChipGroup(context);
        daysGroup.setChipSpacingHorizontal(SelectionHelper.dp(context, 8));
        daysGroup.setChipSpacingVertical(SelectionHelper.dp(context, 4));
        Map<Integer, List<String>> schedule = new LinkedHashMap<>();
        if (existing != null) {
            for (Integer day : existing.daysOfWeek) schedule.put(day, new ArrayList<>());
            if (existing.schedule != null)
                for (HabitSchedule item : existing.schedule)
                    if (item != null && item.dayOfWeek != null && item.startTimes != null)
                        schedule.put(item.dayOfWeek, new ArrayList<>(item.startTimes));
        }
        for (com.aula.delta_mobile_app.repository.SignupOptions.Entry day : registration().catalog.days) {
            Chip chip = SelectionHelper.chip(context, day.label.split("-")[0]);
            chip.setTag(day.id);
            chip.setContentDescription(day.label);
            chip.setChecked(schedule.containsKey(day.id));
            daysGroup.addView(chip);
        }
        content.addView(daysGroup);
        LinearLayout scheduleContent = new LinearLayout(context);
        scheduleContent.setOrientation(LinearLayout.VERTICAL);
        content.addView(scheduleContent, new LinearLayout.LayoutParams(-1, -2));
        TextView summary = SelectionHelper.label(context, "");
        summary.setPadding(0, SelectionHelper.dp(context, 12), 0, 0);
        content.addView(summary);
        TextView errorLabel = SelectionHelper.label(context, "");
        errorLabel.setTextColor(0xFFBA1A1A);
        errorLabel.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(errorLabel);
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            scheduleContent.removeAllViews();
            for (com.aula.delta_mobile_app.repository.SignupOptions.Entry day : registration().catalog.days) {
                List<String> times = schedule.get(day.id);
                if (times == null) continue;
                TextView title = SelectionHelper.label(context, day.label);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setPadding(0, SelectionHelper.dp(context, 16), 0, SelectionHelper.dp(context, 4));
                scheduleContent.addView(title);
                ChipGroup timeGroup = new ChipGroup(context);
                timeGroup.setChipSpacingHorizontal(SelectionHelper.dp(context, 8));
                for (int i = 0; i < times.size(); i++) {
                    int index = i;
                    Chip time = SelectionHelper.chip(context, times.get(i));
                    time.setCheckable(false);
                    time.setCloseIconVisible(true);
                    time.setOnClickListener(view -> openTimePicker(day.id, index, schedule, render[0]));
                    time.setOnCloseIconClickListener(view -> {
                        times.remove(index);
                        render[0].run();
                    });
                    timeGroup.addView(time);
                }
                scheduleContent.addView(timeGroup);
                MaterialButton add = new MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle);
                add.setText(R.string.auth_habit_add_time);
                add.setOnClickListener(view -> openTimePicker(day.id, -1, schedule, render[0]));
                scheduleContent.addView(add, new LinearLayout.LayoutParams(-2, SelectionHelper.dp(context, 48)));
            }
            int executions = 0;
            for (List<String> times : schedule.values()) executions += times.size();
            summary.setText(getResources().getQuantityString(R.plurals.auth_habit_summary, executions, executions));
        };
        for (int i = 0; i < daysGroup.getChildCount(); i++) {
            Chip chip = (Chip) daysGroup.getChildAt(i);
            chip.setOnCheckedChangeListener((button, checked) -> {
                Integer day = (Integer) button.getTag();
                if (checked) schedule.putIfAbsent(day, new ArrayList<>());
                else schedule.remove(day);
                errorLabel.setText("");
                render[0].run();
            });
        }
        render[0].run();
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
            String error = registration().setHabit(key, schedule);
            if (error != null) {
                errorLabel.setText(error);
                return;
            }
            int count = registration().habit(key).frequency;
            card.setSelected(true);
            card.setContentDescription(getString(R.string.auth_habit_selected_description, card.getText(), count));
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

    private void openTimePicker(int day, int index, Map<Integer, List<String>> schedule, Runnable render) {
        if (getChildFragmentManager().isStateSaved() || getChildFragmentManager().findFragmentByTag(TIME_PICKER) != null) return;
        List<String> times = schedule.get(day);
        if (times == null) return;
        LocalTime initial = LocalTime.now();
        if (index >= 0 && index < times.size()) {
            try {
                initial = LocalTime.parse(times.get(index));
            } catch (RuntimeException ignored) {
            }
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(initial.getHour()).setMinute(initial.getMinute())
            .setTitleText(R.string.auth_habit_add_time).build();
        picker.addOnPositiveButtonClickListener(view -> {
            String value = String.format(Locale.ROOT, "%02d:%02d", picker.getHour(), picker.getMinute());
            if (times.contains(value) && (index < 0 || !value.equals(times.get(index)))) {
                Toast.makeText(requireContext(), R.string.auth_habit_duplicate_time, Toast.LENGTH_LONG).show();
                return;
            }
            if (index < 0) times.add(value);
            else if (index < times.size()) times.set(index, value);
            render.run();
        });
        picker.show(getChildFragmentManager(), TIME_PICKER);
    }
}
