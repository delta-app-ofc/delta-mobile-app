package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineData;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RoutineAgendaAdapter extends RecyclerView.Adapter<RoutineAgendaAdapter.Holder> {
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView time, name, category, estimate;

        Holder(View view) {
            super(view);
            time = view.findViewById(R.id.routine_habit_time);
            name = view.findViewById(R.id.routine_habit_name);
            category = view.findViewById(R.id.routine_habit_category);
            estimate = view.findViewById(R.id.routine_habit_estimate);
            ViewCompat.setScreenReaderFocusable(view, true);
        }
    }

    private List<RoutineData.Execution> executions = Collections.emptyList();

    public void submit(RoutineData.Day day) {
        List<RoutineData.Execution> previous = executions;
        List<RoutineData.Execution> next = day == null ? Collections.emptyList() : day.executions;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return previous.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                RoutineData.Execution old = previous.get(oldPosition), value = next.get(newPosition);
                return old.habit.id == value.habit.id && old.startTime.equals(value.startTime);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).habit == next.get(newPosition).habit;
            }
        });
        executions = next;
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine_habit, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RoutineData.Execution execution = executions.get(position);
        Context context = holder.itemView.getContext();
        holder.time.setText(execution.startTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        holder.name.setText(name(context, execution.habit));
        String category = execution.habit.category;
        holder.category.setText(category);
        holder.category.setVisibility(category == null || category.trim().isEmpty() ? View.GONE : View.VISIBLE);
        holder.estimate.setText(context.getString(R.string.routine_estimate, liters(context, execution.liters)));
    }

    @Override
    public int getItemCount() {
        return executions.size();
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
