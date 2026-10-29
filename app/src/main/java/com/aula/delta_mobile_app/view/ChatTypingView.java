package com.aula.delta_mobile_app.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class ChatTypingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float phase;

    public ChatTypingView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 3; i++) {
            float emphasis = (float) ((Math.cos((phase - i / 3f) * Math.PI * 2) + 1) / 2);
            paint.setColor(0xFFE8EEFF);
            paint.setAlpha((int) (85 + 170 * emphasis));
            canvas.drawCircle((5 + i * 12) * density, getHeight() / 2f, (2 + emphasis * 2) * density, paint);
        }
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); updateAnimation(); }
    @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
    @Override protected void onVisibilityChanged(View changedView, int visibility) { super.onVisibilityChanged(changedView, visibility); updateAnimation(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); updateAnimation(); }

    private void updateAnimation() {
        if (!isAttachedToWindow() || !isShown() || getWindowVisibility() != VISIBLE || !ValueAnimator.areAnimatorsEnabled()) {
            stopAnimation();
            return;
        }
        if (animator != null) return;
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1050);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> { phase = (float) value.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) { animator.cancel(); animator.removeAllUpdateListeners(); animator = null; }
        phase = 0;
        invalidate();
    }
}
