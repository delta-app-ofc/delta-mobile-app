package com.aula.delta_mobile_app.view;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

public final class RoutineDayAdapter extends RecyclerView.Adapter<RoutineDayAdapter.Holder> {
    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialButton button;

        Holder(MaterialButton button) {
            super(button);
            this.button = button;
        }
    }

    private final Consumer<LocalDate> select;
    private final LocalDate first, last;
    private LocalDate selected, today, start;
    private int availableWidth;

    public RoutineDayAdapter(Consumer<LocalDate> select, LocalDate first, LocalDate last) {
        this.select = select;
        this.first = first;
        this.last = last;
        setHasStableIds(true);
    }

    public void submit(LocalDate selected, LocalDate today) {
        if (selected.equals(this.selected) && today.equals(this.today)) return;
        boolean initial = start == null;
        this.selected = selected;
        this.today = today;
        start = selected.minusDays(selected.getDayOfWeek().getValue() - 1);
        if (initial) notifyItemRangeInserted(0, getItemCount());
        else notifyItemRangeChanged(0, getItemCount());
    }

    public void resize(int width) {
        if (availableWidth == width) return;
        availableWidth = width;
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public int getItemCount() {
        return start == null ? 0 : 7;
    }

    @Override
    public long getItemId(int position) {
        return start.plusDays(position).toEpochDay();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder((MaterialButton) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MaterialButton button = holder.button;
        LocalDate date = start.plusDays(position);
        Locale locale = button.getResources().getConfiguration().getLocales().get(0);
        boolean active = date.equals(selected);
        boolean enabled = !date.isBefore(first) && !date.isAfter(last);
        int minimum = Math.round(button.getResources().getDimension(R.dimen.routine_touch_size) * Math.max(1f, button.getResources().getConfiguration().fontScale));
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.width = Math.max(minimum, availableWidth / 7);
        button.setLayoutParams(params);
        String weekday = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String number = NumberFormat.getIntegerInstance(locale).format(date.getDayOfMonth());
        button.setText(button.getContext().getString(active ? R.string.routine_day_selected : R.string.routine_day, weekday, number));
        button.setSelected(active);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.4f);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(button.getContext(), active ? R.color.routine_accent : R.color.routine_surface)));
        button.setTextColor(ContextCompat.getColor(button.getContext(), active ? R.color.routine_text : R.color.routine_muted));
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(button.getContext(), R.color.routine_text)));
        button.setStrokeWidth(active ? button.getResources().getDimensionPixelSize(R.dimen.routine_selected_stroke) : 0);
        String description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        if (date.equals(today)) description = button.getContext().getString(R.string.routine_today_description, description);
        button.setContentDescription(description);
        ViewCompat.setStateDescription(button, button.getContext().getString(active ? R.string.routine_selected : R.string.routine_not_selected));
        button.setOnClickListener(view -> select.accept(date));
    }
}
