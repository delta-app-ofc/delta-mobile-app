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
    private final android.text.method.KeyListener editableKeyListener;
    private Boolean editing;
    private boolean editableHighlightEnabled;
    private boolean outlinedBox;
    private final android.graphics.Rect inputBounds = new android.graphics.Rect();
    private final android.graphics.RectF borderBounds = new android.graphics.RectF();
    private final android.graphics.Path outerBorder = new android.graphics.Path(), innerBorder = new android.graphics.Path();
    private final android.graphics.Paint labelPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.drawable.Drawable gradientBorder;

    public ProfileFormField(Context context, int label, int type) {
        super(context);
        this.type = type;
        gradientBorder = ContextCompat.getDrawable(context, R.drawable.leak_blue_card).mutate();
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
        editableKeyListener = input.getKeyListener();
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
            input.setKeyListener(editableKeyListener);
            input.setFocusable(true);
            input.setFocusableInTouchMode(true);
        } else {
            input.setKeyListener(null);
            input.setTextIsSelectable(true);
            input.clearFocus();
        }
        input.setCursorVisible(editable);
        input.setShowSoftInputOnFocus(editable);
        updateEditableBackground();
    }

    public void setEditableHighlightEnabled(boolean enabled) {
        editableHighlightEnabled = enabled;
        if (enabled && !outlinedBox) useOutlinedBox();
        updateEditableBackground();
    }

    public void useOutlinedBox() {
        if (outlinedBox) return;
        outlinedBox = true;
        input.setSupportBackgroundTintList(null);
        input.setBackgroundTintList(null);
        input.setBackground(null);
        setBoxBackgroundMode(BOX_BACKGROUND_NONE);
        setBoxBackgroundMode(BOX_BACKGROUND_OUTLINE);
        setHintTextAppearance(R.style.Profile_FieldLabel);
        setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.profile_muted)));
        setHintAnimationEnabled(false);
        setBoxStrokeColorStateList(ColorStateList.valueOf(0xFF949494));
        input.setMinHeight(dp(58));
        updateEditableBackground();
    }

    private void updateEditableBackground() {
        if (!outlinedBox) return;
        boolean highlighted = editableHighlightEnabled && Boolean.TRUE.equals(editing) && android.text.TextUtils.isEmpty(getError());
        setBoxStrokeWidth(highlighted ? 0 : dp(1));
        setBoxStrokeWidthFocused(highlighted ? 0 : dp(1));
        invalidate();
    }

    @Override
    protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!outlinedBox || !editableHighlightEnabled || !Boolean.TRUE.equals(editing) || !android.text.TextUtils.isEmpty(getError())) return;
        input.getDrawingRect(inputBounds);
        offsetDescendantRectToMyCoords(input, inputBounds);
        borderBounds.set(inputBounds);
        outerBorder.reset();
        outerBorder.addRoundRect(borderBounds, dp(14), dp(14), android.graphics.Path.Direction.CW);
        borderBounds.inset(dp(2), dp(2));
        innerBorder.reset();
        innerBorder.addRoundRect(borderBounds, dp(12), dp(12), android.graphics.Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(outerBorder);
        canvas.clipOutPath(innerBorder);
        if (!android.text.TextUtils.isEmpty(getHint())) {
            labelPaint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            labelPaint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
            float width = Math.min(labelPaint.measureText(getHint().toString()), inputBounds.width() - input.getCompoundPaddingLeft() - input.getCompoundPaddingRight());
            float left = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? inputBounds.right - input.getCompoundPaddingRight() - width : inputBounds.left + input.getCompoundPaddingLeft();
            canvas.clipOutRect(left - dp(4), inputBounds.top - dp(12), left + width + dp(4), inputBounds.top + dp(12));
        }
        gradientBorder.setBounds(inputBounds);
        gradientBorder.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public void setError(CharSequence error) {
        super.setError(error);
        if (input != null) updateEditableBackground();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
