package com.aula.delta_mobile_app.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public final class LoadingView extends View {
    private final Paint trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dropBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trianglePath = new Path(), dropClipPath = new Path(), waterPath = new Path(), highlightPath = new Path();
    private final RectF triangleBounds = new RectF();
    private ValueAnimator animator;
    private float waterLevel, wavePhase;

    public LoadingView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(34, 34, 34));
        trianglePaint.setColor(Color.WHITE);
        dropBasePaint.setColor(Color.rgb(159, 187, 242));
        waterPaint.setColor(Color.rgb(48, 107, 229));
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeCap(Paint.Cap.ROUND);
        highlightPaint.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription("Estamos configurando tudo para você.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(value -> {
            waterLevel = (float) value.getAnimatedValue();
            wavePhase = waterLevel * (float) (Math.PI * 3);
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float width = Math.min(115 * density, getWidth() * 0.35f);
        float height = width * 108 / 115;
        float top = (getHeight() - height) * 0.5f;
        triangleBounds.set((getWidth() - width) / 2, top, (getWidth() + width) / 2, top + height);
        buildDropPath();
        drawVectorTriangle(canvas, 255);
        drawWater(canvas);
        drawHighlight(canvas, 255);
    }

    private void drawVectorTriangle(Canvas canvas, int alpha) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        trianglePath.rewind();
        trianglePath.moveTo(left + width * 0.50f, top);
        trianglePath.cubicTo(left + width * 0.43f, top, left + width * 0.40f, top + height * 0.05f, left + width * 0.35f, top + height * 0.14f);
        trianglePath.lineTo(left + width * 0.02f, top + height * 0.73f);
        trianglePath.cubicTo(left - width * 0.05f, top + height * 0.87f, left + width * 0.06f, top + height, left + width * 0.20f, top + height);
        trianglePath.lineTo(left + width * 0.80f, top + height);
        trianglePath.cubicTo(left + width * 0.95f, top + height, left + width * 1.04f, top + height * 0.87f, left + width * 0.98f, top + height * 0.73f);
        trianglePath.lineTo(left + width * 0.65f, top + height * 0.14f);
        trianglePath.cubicTo(left + width * 0.60f, top + height * 0.05f, left + width * 0.57f, top, left + width * 0.50f, top);
        trianglePath.close();

        trianglePaint.setAlpha(alpha);
        canvas.drawPath(trianglePath, trianglePaint);
        canvas.drawPath(dropClipPath, dropBasePaint);
    }

    private void buildDropPath() {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        dropClipPath.rewind();
        dropClipPath.moveTo(left + width * 0.50f, top + height * 0.22f);
        dropClipPath.cubicTo(left + width * 0.46f, top + height * 0.32f, left + width * 0.27f, top + height * 0.50f, left + width * 0.27f, top + height * 0.64f);
        dropClipPath.cubicTo(left + width * 0.27f, top + height * 0.83f, left + width * 0.39f, top + height * 0.90f, left + width * 0.50f, top + height * 0.90f);
        dropClipPath.cubicTo(left + width * 0.69f, top + height * 0.90f, left + width * 0.75f, top + height * 0.77f, left + width * 0.74f, top + height * 0.64f);
        dropClipPath.cubicTo(left + width * 0.72f, top + height * 0.50f, left + width * 0.55f, top + height * 0.32f, left + width * 0.50f, top + height * 0.22f);
        dropClipPath.close();
    }

    private void drawWater(Canvas canvas) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();
        float dropTop = top + height * 0.22f;
        float dropBottom = top + height * 0.89f;
        float surfaceY = lerp(dropBottom + height * 0.03f, dropTop - height * 0.03f, waterLevel);
        float sloshEnvelope = (float) Math.sin(Math.PI * waterLevel);
        float tilt = (float) Math.sin(wavePhase) * height * 0.13f * sloshEnvelope;
        float meniscus = (float) Math.cos(wavePhase) * height * 0.012f * sloshEnvelope;
        float waterLeft = left + width * 0.23f;
        float waterCenter = left + width * 0.50f;
        float waterRight = left + width * 0.78f;
        float edgeCurl = height * 0.088f * sloshEnvelope;
        float leftSlopeY = surfaceY - tilt;
        float rightSlopeY = surfaceY + tilt;
        float centerSurfaceY = surfaceY + meniscus;

        waterPath.rewind();
        waterPath.moveTo(waterLeft, leftSlopeY - edgeCurl);
        waterPath.cubicTo(waterLeft, leftSlopeY + edgeCurl * 0.76f, left + width * 0.36f, centerSurfaceY, waterCenter, centerSurfaceY);
        waterPath.cubicTo(left + width * 0.65f, centerSurfaceY, waterRight, rightSlopeY + edgeCurl * 0.76f, waterRight, rightSlopeY - edgeCurl);
        waterPath.lineTo(left + width, top + height);
        waterPath.lineTo(left, top + height);
        waterPath.close();

        canvas.save();
        canvas.clipPath(dropClipPath);
        canvas.drawPath(waterPath, waterPaint);
        canvas.restore();
    }

    private void drawHighlight(Canvas canvas, int alpha) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        highlightPaint.setAlpha(alpha);
        highlightPaint.setStrokeWidth(width * 0.058f);
        highlightPath.rewind();
        highlightPath.moveTo(left + width * 0.63f, top + height * 0.64f);
        highlightPath.cubicTo(left + width * 0.63f, top + height * 0.72f, left + width * 0.57f, top + height * 0.78f, left + width * 0.47f, top + height * 0.78f);

        canvas.save();
        canvas.clipPath(dropClipPath);
        canvas.drawPath(highlightPath, highlightPaint);
        canvas.restore();
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }
}
