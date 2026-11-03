package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.aula.delta_mobile_app.R;

public class StatusNoticeView extends AppCompatTextView {
    public StatusNoticeView(Context context) {
        this(context, null);
    }

    public StatusNoticeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.status_notice_background);
        setTextColor(0xFFF3F4F5);
        setTextSize(14);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setPaddingRelative(dp(16), dp(14), dp(16), dp(14));
        setMinimumHeight(dp(56));
        setCompoundDrawablePadding(dp(12));
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.status_notice_icon);
        if (icon != null) {
            icon.setBounds(0, 0, dp(20), dp(20));
            setCompoundDrawablesRelative(icon, null, null, null);
        }
        setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        setVisibility(TextUtils.isEmpty(getText()) ? GONE : VISIBLE);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        setVisibility(TextUtils.isEmpty(text) ? GONE : VISIBLE);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility == VISIBLE && TextUtils.isEmpty(getText()) ? GONE : visibility);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
