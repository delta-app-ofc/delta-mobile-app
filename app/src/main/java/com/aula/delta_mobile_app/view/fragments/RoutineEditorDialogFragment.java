package com.aula.delta_mobile_app.view.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.RoutineFormState;
import com.aula.delta_mobile_app.model.RoutineFormValidator;
import com.aula.delta_mobile_app.view.StatusNoticeView;
import com.aula.delta_mobile_app.viewmodel.RoutineViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RoutineEditorDialogFragment extends DialogFragment {
    private RoutineViewModel model;
    private RecyclerView list;
    private View header;
    private MaterialAutoCompleteTextView type;
    private TextInputEditText frequency;
    private FormAdapter adapter;
    private AlertDialog confirmation;
    private final Map<DayOfWeek, Chip> chips = new EnumMap<>(DayOfWeek.class);
    private long token;
    private boolean binding, destroyingView;

    public RoutineEditorDialogFragment() {
        super(R.layout.dialog_routine_editor);
    }

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setStyle(STYLE_NO_TITLE, R.style.Routine_EditorDialog);
        setCancelable(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        destroyingView = false;
        model = new ViewModelProvider(requireParentFragment()).get(RoutineViewModel.class);
        RoutineFormState form = model.state().getValue().form;
        if (form == null) {
            dismissAllowingStateLoss();
            return;
        }
        token = form.token;
        header = view.findViewById(R.id.routine_form_header);
        ((ViewGroup) header.getParent()).removeView(header);
        header.setVisibility(View.VISIBLE);
        header.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
        type = header.findViewById(R.id.routine_form_type);
        frequency = header.findViewById(R.id.routine_form_frequency);
        type.setTag(RoutineFormState.TYPE);
        frequency.setTag(RoutineFormState.FREQUENCY);
        type.setSimpleItems(R.array.routine_form_types);
        type.setOnItemClickListener((parent, item, position, id) -> {
            if (!binding && current() != null) model.setFormHabit(position + 1);
        });
        frequency.addTextChangedListener(watcher(value -> {
            if (!binding && current() != null) model.setFormFrequency(value);
        }));
        frequency.setOnEditorActionListener((field, action, event) -> {
            if (action != EditorInfo.IME_ACTION_NEXT) return false;
            focusFirstDistributionField();
            return true;
        });
        ChipGroup group = header.findViewById(R.id.routine_form_days);
        for (DayOfWeek day : DayOfWeek.values()) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(day.getDisplayName(TextStyle.FULL, locale()));
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (!binding && current() != null) model.selectFormDay(day, checked);
            });
            group.addView(chip);
            chips.put(day, chip);
        }
        list = view.findViewById(R.id.routine_form_fields);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setItemAnimator(null);
        adapter = new FormAdapter();
        list.setAdapter(adapter);
        header.findViewById(R.id.routine_form_close).setOnClickListener(button -> model.requestFormClose(token));
        view.findViewById(R.id.routine_form_validate).setOnClickListener(button -> {
            boolean valid = model.validateForm();
            RoutineFormState current = current();
            if (current == null) return;
            if (valid) {
                hideKeyboard();
                list.scrollToPosition(0);
            } else focus(current.errors.keySet().iterator().next(), -1, -1, true, 8);
        });
        Dialog dialog = requireDialog();
        dialog.setCanceledOnTouchOutside(false);
        if (dialog instanceof ComponentDialog) {
            ((ComponentDialog) dialog).getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    model.requestFormClose(token);
                }
            });
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, (root, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int bottom = Math.max(bars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            root.setPadding(bars.left, bars.top, bars.right, bottom);
            return insets;
        });
        model.state().observe(getViewLifecycleOwner(), state -> render(state.form));
        render(form);
        if (saved != null && saved.getLong("formToken", -1) == token && saved.getString("focusKey") != null)
            focus(saved.getString("focusKey"), saved.getInt("selectionStart", -1), saved.getInt("selectionEnd", -1), saved.getBoolean("imeVisible"), 8);
    }

    private RoutineFormState current() {
        if (destroyingView || getView() == null || model == null) return null;
        RoutineFormState form = model.state().getValue().form;
        return form != null && form.token == token ? form : null;
    }

    private Locale locale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    private void render(RoutineFormState form) {
        if (form == null || form.token != token) {
            dismissAllowingStateLoss();
            return;
        }
        if (adapter == null) return;
        adapter.update(form);
        if (form.confirmDiscard && (confirmation == null || !confirmation.isShowing())) {
            confirmation = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.routine_form_discard_title).setMessage(R.string.routine_form_discard_message)
                .setPositiveButton(R.string.routine_form_discard, (dialog, which) -> model.discardForm(token))
                .setNegativeButton(R.string.routine_form_continue, (dialog, which) -> model.cancelFormDiscard(token))
                .setOnCancelListener(dialog -> model.cancelFormDiscard(token)).show();
        } else if (!form.confirmDiscard && confirmation != null) {
            confirmation.dismiss();
            confirmation = null;
        }
    }

    private void bindHeader(RoutineFormState form) {
        binding = true;
        ((TextView) header.findViewById(R.id.routine_form_title)).setText(form.mode == RoutineFormState.Mode.CREATE ? R.string.routine_form_create : R.string.routine_form_edit);
        String[] labels = getResources().getStringArray(R.array.routine_form_types);
        String label = form.habitId == null ? "" : form.habitId >= 1 && form.habitId <= labels.length ? labels[form.habitId - 1] : getString(R.string.routine_form_unknown_type);
        if (!type.getText().toString().equals(label)) type.setText(label, false);
        if (!frequency.getText().toString().equals(form.frequency)) frequency.setText(form.frequency);
        ((TextInputLayout) header.findViewById(R.id.routine_form_type_layout)).setError(error(form.errors.get(RoutineFormState.TYPE)));
        ((TextInputLayout) header.findViewById(R.id.routine_form_frequency_layout)).setError(error(form.errors.get(RoutineFormState.FREQUENCY)));
        for (DayOfWeek day : DayOfWeek.values()) chips.get(day).setChecked(form.days.get(day).selected);
        groupError(R.id.routine_form_days_error, form.errors.containsKey(RoutineFormState.DAYS) ? getString(R.string.routine_form_days_required) : "");
        groupError(R.id.routine_form_distribution_error, error(form.errors.get(RoutineFormState.DISTRIBUTION)));
        ((StatusNoticeView) header.findViewById(R.id.routine_form_notice)).setText(form.valid() ? getString(R.string.routine_form_valid)
            : form.legacy ? getString(R.string.routine_form_legacy) : "");
        binding = false;
    }

    private void groupError(int id, String message) {
        TextView view = header.findViewById(id);
        view.setText(message);
        view.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String error(RoutineFormValidator.Error error) {
        if (error == null) return null;
        switch (error) {
            case REQUIRED: return getString(R.string.routine_form_required);
            case INVALID_TYPE: return getString(R.string.routine_form_invalid_type);
            case INVALID_NUMBER: return getString(R.string.routine_form_invalid_number);
            case POSITIVE: return getString(R.string.routine_form_positive);
            case TOO_MANY_TIMES: return getString(R.string.routine_form_too_many_times);
            case INVALID_TIME: return getString(R.string.routine_form_invalid_time);
            case DUPLICATE_TIME: return getString(R.string.routine_form_duplicate_time);
            default: return getString(R.string.routine_form_mismatch);
        }
    }

    private static TextWatcher watcher(java.util.function.Consumer<String> changed) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable value) {
                changed.accept(value.toString());
            }
        };
    }

    private static final class Field {
        final DayOfWeek day;
        final int index;
        final String key;

        Field(DayOfWeek day, int index) {
            this.day = day;
            this.index = index;
            key = index < 0 ? RoutineFormState.quantityKey(day) : RoutineFormState.timeKey(day, index);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Field && key.equals(((Field) other).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    private class Holder extends RecyclerView.ViewHolder {
        final TextInputLayout layout;
        final TextInputEditText input;
        Field field;
        boolean writing;

        Holder(View view, boolean row) {
            super(view);
            layout = row ? (TextInputLayout) view : null;
            input = row ? view.findViewById(R.id.routine_execution_input) : null;
            if (input != null) {
                input.addTextChangedListener(watcher(value -> {
                    if (writing || field == null || current() == null) return;
                    if (field.index < 0) model.setFormQuantity(field.day, value);
                    else model.setFormTime(field.day, field.index, value);
                }));
                input.setOnEditorActionListener((target, action, event) -> {
                    if (action != EditorInfo.IME_ACTION_NEXT || field == null) return false;
                    int position = adapter.fields.indexOf(field) + 1;
                    if (position < adapter.fields.size()) focus(adapter.fields.get(position).key, -1, -1, true, 8);
                    else hideKeyboard();
                    return true;
                });
            }
        }

        void bind(Field next, RoutineFormState form) {
            writing = true;
            field = next;
            if (!next.key.equals(input.getTag())) {
                input.setTag(next.key);
                input.setInputType(next.index < 0 ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED
                    : InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
            }
            String day = next.day.getDisplayName(TextStyle.FULL, locale());
            layout.setHint(next.index < 0 ? getString(R.string.routine_form_quantity, day) : getString(R.string.routine_form_time, day, next.index + 1));
            String raw = next.index < 0 ? form.days.get(next.day).quantity : form.days.get(next.day).times.get(next.index);
            if (!input.getText().toString().equals(raw)) input.setText(raw);
            layout.setError(error(form.errors.get(next.key)));
            writing = false;
        }
    }

    private final class FormAdapter extends RecyclerView.Adapter<Holder> {
        List<Field> fields = new ArrayList<>();
        RoutineFormState form;

        FormAdapter() {
            setHasStableIds(true);
        }

        void update(RoutineFormState value) {
            List<Field> next = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                RoutineFormState.DayDraft draft = value.days.get(day);
                if (!draft.selected) continue;
                next.add(new Field(day, -1));
                for (int i = 0; i < draft.activeCount(); i++) next.add(new Field(day, i));
            }
            form = value;
            if (!fields.equals(next)) {
                List<Field> previous = fields;
                DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() { return previous.size() + 1; }
                    @Override
                    public int getNewListSize() { return next.size() + 1; }
                    @Override
                    public boolean areItemsTheSame(int oldPosition, int newPosition) {
                        return oldPosition == 0 || newPosition == 0 ? oldPosition == newPosition : previous.get(oldPosition - 1).equals(next.get(newPosition - 1));
                    }
                    @Override
                    public boolean areContentsTheSame(int oldPosition, int newPosition) { return true; }
                });
                fields = next;
                diff.dispatchUpdatesTo(this);
            }
            notifyItemRangeChanged(0, getItemCount(), Boolean.TRUE);
        }

        @Override
        public int getItemCount() { return fields.size() + 1; }

        @Override
        public int getItemViewType(int position) { return position == 0 ? 0 : 1; }

        @Override
        public long getItemId(int position) {
            if (position == 0) return 0;
            Field field = fields.get(position - 1);
            return field.day.getValue() * 10000L + field.index + 2;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return type == 0 ? new Holder(header, false) : new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine_execution_field, parent, false), true);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            if (form == null) return;
            if (position == 0) bindHeader(form);
            else holder.bind(fields.get(position - 1), form);
        }
    }

    private void focusFirstDistributionField() {
        if (adapter != null && !adapter.fields.isEmpty()) focus(adapter.fields.get(0).key, -1, -1, true, 8);
        else focus(RoutineFormState.DAYS, -1, -1, false, 8);
    }

    private void focus(String key, int start, int end, boolean keyboard, int attempts) {
        if (list == null || adapter == null || current() == null || attempts == 0) return;
        if (RoutineFormState.DISTRIBUTION.equals(key)) {
            focusFirstDistributionField();
            return;
        }
        int position = 0;
        for (int i = 0; i < adapter.fields.size(); i++) if (adapter.fields.get(i).key.equals(key)) position = i + 1;
        final int targetPosition = position;
        list.scrollToPosition(position);
        list.postOnAnimation(() -> {
            if (list == null || current() == null) return;
            Holder holder = (Holder) list.findViewHolderForAdapterPosition(targetPosition);
            if (holder == null) {
                focus(key, start, end, keyboard, attempts - 1);
                return;
            }
            View target = targetPosition > 0 ? holder.input : RoutineFormState.TYPE.equals(key) ? type : RoutineFormState.FREQUENCY.equals(key) ? frequency
                : chips.get(DayOfWeek.MONDAY);
            target.requestFocus();
            target.requestRectangleOnScreen(new Rect(0, 0, target.getWidth(), target.getHeight()), true);
            if (target instanceof EditText) {
                EditText input = (EditText) target;
                int length = input.length();
                input.setSelection(start < 0 ? length : Math.min(start, length), end < 0 ? length : Math.min(end, length));
                if (keyboard && target != type) ((InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard() {
        if (getView() != null) ((InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            int maximum = getResources().getDimensionPixelSize(R.dimen.routine_form_max_width);
            int margin = getResources().getDimensionPixelSize(R.dimen.routine_form_window_margin);
            Rect bounds = requireActivity().getWindowManager().getCurrentWindowMetrics().getBounds();
            window.setLayout(maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.min(maximum, Math.max(1, bounds.width() - margin * 2)),
                maximum == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : Math.max(1, bounds.height() - margin * 2));
            window.setGravity(Gravity.CENTER);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightNavigationBars(false);
        }
        if (getView() != null) ViewCompat.requestApplyInsets(getView());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle saved) {
        super.onSaveInstanceState(saved);
        saved.putLong("formToken", token);
        View focused = getView() == null ? null : getView().findFocus();
        if (focused instanceof EditText && focused.getTag() instanceof String) {
            saved.putString("focusKey", (String) focused.getTag());
            saved.putInt("selectionStart", ((EditText) focused).getSelectionStart());
            saved.putInt("selectionEnd", ((EditText) focused).getSelectionEnd());
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(getView());
            saved.putBoolean("imeVisible", insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()));
        }
    }

    @Override
    public void onDestroyView() {
        destroyingView = true;
        if (confirmation != null) confirmation.dismiss();
        confirmation = null;
        if (list != null) list.setAdapter(null);
        adapter = null;
        list = null;
        header = null;
        type = null;
        frequency = null;
        chips.clear();
        super.onDestroyView();
    }
}
