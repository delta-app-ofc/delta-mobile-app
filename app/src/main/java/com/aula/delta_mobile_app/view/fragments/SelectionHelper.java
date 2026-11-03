package com.aula.delta_mobile_app.view.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.aula.delta_mobile_app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

final class SelectionHelper {
    private SelectionHelper() {}

    static Context context(Context context) {
        return new ContextThemeWrapper(context, R.style.Theme_Delta_Selection);
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static LinearLayout content(Context context, String title, String description) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 24), dp(context, 16), dp(context, 24), dp(context, 24));
        View handle = new View(context);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(0xFFDCE4F3);
        shape.setCornerRadius(dp(context, 4));
        handle.setBackground(shape);
        LinearLayout.LayoutParams handleLayout = new LinearLayout.LayoutParams(dp(context, 32), dp(context, 4));
        handleLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleLayout.bottomMargin = dp(context, 20);
        content.addView(handle, handleLayout);
        TextView heading = label(context, title);
        heading.setTextSize(24);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        androidx.core.view.ViewCompat.setAccessibilityHeading(heading, true);
        content.addView(heading);
        TextView subtitle = label(context, description);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(context, 8), 0, dp(context, 16));
        content.addView(subtitle);
        return content;
    }

    static TextView label(Context context, String text) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(16);
        label.setTextColor(ContextCompat.getColor(context, R.color.auth_navy));
        return label;
    }

    static Chip chip(Context context, String text) {
        Chip chip = new Chip(context);
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setEnsureMinTouchTargetSize(true);
        int[][] states = {new int[] {android.R.attr.state_checked}, new int[] {}};
        chip.setChipBackgroundColor(new ColorStateList(states, new int[] {0xFF3069E7, 0xFFE8EFFF}));
        chip.setTextColor(new ColorStateList(states, new int[] {0xFFFFFFFF, 0xFF1A2358}));
        chip.setChipStrokeWidth(0);
        chip.setCheckedIconTint(ColorStateList.valueOf(0xFFFFFFFF));
        chip.setChipCornerRadius(dp(context, 16));
        return chip;
    }

    static BottomSheetDialog show(Fragment owner, Context context, View content) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        scroll.setFillViewport(true);
        scroll.addView(content);
        dialog.setContentView(scroll);
        dialog.setOnShowListener(ignored -> {
            dialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setSkipCollapsed(true);
        });
        DefaultLifecycleObserver cleanup = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                dialog.dismiss();
            }
        };
        androidx.lifecycle.Lifecycle lifecycle = owner.getViewLifecycleOwner().getLifecycle();
        lifecycle.addObserver(cleanup);
        dialog.setOnDismissListener(ignored -> lifecycle.removeObserver(cleanup));
        dialog.show();
        return dialog;
    }
}
