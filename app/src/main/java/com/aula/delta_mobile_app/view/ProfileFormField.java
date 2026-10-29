package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.aula.delta_mobile_app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class ProfileFormField extends TextInputLayout {
    public final TextInputEditText input;
    private final int type;
    private Boolean editing;

    public ProfileFormField(Context context, int label, int type) {
        super(context);
        this.type = type;
        setHint(context.getString(label));
        setExpandedHintEnabled(false);
        setBoxBackgroundMode(BOX_BACKGROUND_OUTLINE);
        setBoxBackgroundColor(ContextCompat.getColor(context, R.color.profile_field));
        setBoxStrokeColor(0xFFB0B2B3);
        setDefaultHintTextColor(ColorStateList.valueOf(0xFFB0B2B3));
        setHintTextColor(ColorStateList.valueOf(0xFFB5CAF6));
        float radius = dp(14);
        setBoxCornerRadii(radius, radius, radius, radius);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(24);
        setLayoutParams(params);
        input = new TextInputEditText(context);
        input.setId(android.view.View.generateViewId());
        input.setSaveEnabled(false);
        input.setTextSize(16);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFFB0B2B3);
        input.setMinHeight(dp(54));
        input.setPadding(dp(16), dp(16), dp(16), dp(16));
        input.setInputType(type);
        if ((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) input.setMaxLines(1);
        addView(input, new LayoutParams(-1, -2));
        editable(false);
        value("");
    }

    public void value(String text) {
        String value = text == null ? "" : text;
        if (!input.getText().toString().equals(value)) input.setText(value);
        input.setHint(value.isEmpty() ? getContext().getString(R.string.profile_missing) : null);
    }

    public String value() { return input.getText().toString(); }

    public void editable(boolean editable) {
        if (editing != null && editing == editable) return;
        editing = editable;
        if (editable) {
            input.setTextIsSelectable(false);
            input.setInputType(type);
            input.setFocusable(true);
            input.setFocusableInTouchMode(true);
        } else {
            input.setKeyListener(null);
            input.setTextIsSelectable(true);
            input.clearFocus();
        }
        input.setCursorVisible(editable);
        input.setShowSoftInputOnFocus(editable);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
