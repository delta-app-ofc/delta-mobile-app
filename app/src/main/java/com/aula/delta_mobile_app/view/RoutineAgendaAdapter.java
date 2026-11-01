package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineData;
import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RoutineAgendaAdapter extends RecyclerView.Adapter<RoutineAgendaAdapter.Holder> {
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView time, name, category, estimate;
        final ImageView icon;
        final MaterialCardView card;

        Holder(View view) {
            super(view);
            time = view.findViewById(R.id.routine_habit_time);
            name = view.findViewById(R.id.routine_habit_name);
            category = view.findViewById(R.id.routine_habit_category);
            estimate = view.findViewById(R.id.routine_habit_estimate);
            icon = view.findViewById(R.id.routine_habit_icon);
            card = view.findViewById(R.id.routine_habit_card);
            ViewCompat.setScreenReaderFocusable(view, true);
        }
    }

    private List<RoutineData.Execution> executions = Collections.emptyList();
    private List<RoutineData.Habit> incomplete = Collections.emptyList();

    public void submit(RoutineData.Day day) {
        List<RoutineData.Execution> previous = executions;
        List<RoutineData.Habit> previousIncomplete = incomplete;
        List<RoutineData.Execution> next = day == null ? Collections.emptyList() : day.executions;
        List<RoutineData.Habit> nextIncomplete = day == null ? Collections.emptyList() : day.incompleteHabits;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return previous.size() + previousIncomplete.size();
            }

            @Override
            public int getNewListSize() {
                return next.size() + nextIncomplete.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                boolean oldScheduled = oldPosition < previous.size(), newScheduled = newPosition < next.size();
                if (oldScheduled != newScheduled) return false;
                if (!oldScheduled) return previousIncomplete.get(oldPosition - previous.size()).id == nextIncomplete.get(newPosition - next.size()).id;
                RoutineData.Execution old = previous.get(oldPosition), value = next.get(newPosition);
                return old.habit.id == value.habit.id && old.startTime.equals(value.startTime);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                if (oldPosition < previous.size() && newPosition < next.size())
                    return previous.get(oldPosition).habit == next.get(newPosition).habit;
                return oldPosition >= previous.size() && newPosition >= next.size()
                    && previousIncomplete.get(oldPosition - previous.size()) == nextIncomplete.get(newPosition - next.size());
            }
        });
        executions = next;
        incomplete = nextIncomplete;
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine_habit, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RoutineData.Execution execution = executionAt(position);
        RoutineData.Habit habit = execution == null ? incomplete.get(position - executions.size()) : execution.habit;
        Context context = holder.itemView.getContext();
        LocalTime displayedTime = execution == null ? habit.legacyStartTime : execution.startTime;
        String timeText = displayedTime == null ? context.getString(R.string.routine_no_time) : displayedTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        holder.time.setText(timeText);
        holder.time.setVisibility(View.VISIBLE);
        holder.name.setText(name(context, habit));
        String category = habit.category;
        holder.category.setText(category);
        holder.category.setVisibility(category == null || category.trim().isEmpty() ? View.GONE : View.VISIBLE);
        holder.estimate.setText(context.getString(R.string.routine_estimate, liters(context, habit.litersPerExecution)));
        applyPalette(context, holder.card, holder.icon, habit.habitId);
    }

    @Override
    public int getItemCount() {
        return executions.size() + incomplete.size();
    }

    public RoutineData.Execution executionAt(int position) {
        return position >= 0 && position < executions.size() ? executions.get(position) : null;
    }

    public RoutineData.Habit habitAt(int position) {
        if (position < 0) return null;
        if (position < executions.size()) return executions.get(position).habit;
        int incompletePosition = position - executions.size();
        return incompletePosition < incomplete.size() ? incomplete.get(incompletePosition) : null;
    }

    public static void applyPalette(Context context, MaterialCardView card, ImageView icon, int habitId) {
        int surface, stroke, accent;
        if (habitId == 1) {
            surface = R.color.routine_timeline_purple_surface;
            stroke = R.color.routine_timeline_purple_stroke;
            accent = R.color.routine_timeline_purple_accent;
        } else if (habitId == 4) {
            surface = R.color.routine_timeline_green_surface;
            stroke = R.color.routine_timeline_green_stroke;
            accent = R.color.routine_timeline_green_accent;
        } else if (habitId == 2 || habitId == 5) {
            surface = R.color.routine_timeline_amber_surface;
            stroke = R.color.routine_timeline_amber_stroke;
            accent = R.color.routine_timeline_amber_accent;
        } else {
            surface = R.color.routine_timeline_blue_surface;
            stroke = R.color.routine_timeline_blue_stroke;
            accent = R.color.routine_timeline_blue_accent;
        }
        card.setCardBackgroundColor(ContextCompat.getColor(context, surface));
        card.setStrokeColor(ContextCompat.getColor(context, stroke));
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(context, accent)));
    }

    public static String name(Context context, RoutineData.Habit habit) {
        if (habit.name != null && !habit.name.trim().isEmpty()) return habit.name;
        int[] labels = {R.string.routine_habit_shower, R.string.routine_habit_yard, R.string.routine_habit_clothes, R.string.routine_habit_plants,
            R.string.routine_habit_car, R.string.routine_habit_dishes};
        return context.getString(habit.habitId >= 1 && habit.habitId <= labels.length ? labels[habit.habitId - 1] : R.string.routine_habit_unknown);
    }

    public static String liters(Context context, BigDecimal value) {
        if (value == null) return context.getString(R.string.routine_unavailable);
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return context.getString(R.string.routine_liters, NumberFormat.getIntegerInstance(locale).format(value));
    }
}
