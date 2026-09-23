package com.aula.delta_mobile_app.splash_screen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public final class DeltaSplashView extends View {
    private static final int WATER_COLOR = Color.rgb(48, 107, 229);
    private static final String SLOGAN = "Sua gestão inteligente";
    private static final long FILL_DURATION_MS = 2700L;
    private static final long LOCKUP_DURATION_MS = 400L;
    private static final long HOLD_DURATION_MS = 1600L;
    private static final long EXIT_DURATION_MS = 350L;

    private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dropBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sloganPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint.FontMetrics sloganMetrics = new Paint.FontMetrics();
    private final Path lettersPath = new Path();
    private final Path trianglePath = new Path();
    private final Path dropClipPath = new Path();
    private final Path waterPath = new Path();
    private final Path highlightPath = new Path();
    private final RectF lettersBounds = new RectF();
    private final RectF triangleBounds = new RectF();

    private AnimatorSet animatorSet;
    private float waterLevel;
    private float wavePhase;
    private float lockupProgress;
    private float exitProgress;
    private boolean started;

    public DeltaSplashView(Context context) {
        this(context, null);
    }

    public DeltaSplashView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        logoPaint.setColor(WATER_COLOR);
        logoPaint.setStyle(Paint.Style.FILL);
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
        dropBasePaint.setColor(Color.rgb(159, 187, 242));
        dropBasePaint.setStyle(Paint.Style.FILL);
        waterPaint.setColor(WATER_COLOR);
        waterPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeCap(Paint.Cap.ROUND);
        highlightPaint.setStrokeJoin(Paint.Join.ROUND);
        sloganPaint.setColor(Color.WHITE);
        sloganPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        sloganPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void start(Runnable onFinished) {
        if (started) return;
        started = true;

        ValueAnimator fill = ValueAnimator.ofFloat(0f, 1f);
        fill.setDuration(FILL_DURATION_MS);
        fill.setInterpolator(new AccelerateDecelerateInterpolator());
        fill.addUpdateListener(animation -> {
            waterLevel = (float) animation.getAnimatedValue();
            // Three diminishing half-swings: left, right, left, then level.
            wavePhase = waterLevel * (float) (Math.PI * 3.0);
            invalidate();
        });

        ValueAnimator lockup = ValueAnimator.ofFloat(0f, 1f);
        lockup.setDuration(LOCKUP_DURATION_MS);
        lockup.setInterpolator(new DecelerateInterpolator());
        lockup.addUpdateListener(animation -> {
            lockupProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        ValueAnimator hold = ValueAnimator.ofFloat(0f, 1f);
        hold.setDuration(HOLD_DURATION_MS);
        hold.setInterpolator(new LinearInterpolator());

        ValueAnimator exit = ValueAnimator.ofFloat(0f, 1f);
        exit.setDuration(EXIT_DURATION_MS);
        exit.setInterpolator(new DecelerateInterpolator());
        exit.addUpdateListener(animation -> {
            exitProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        animatorSet = new AnimatorSet();
        animatorSet.playSequentially(fill, lockup, hold, exit);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (started) onFinished.run();
            }
        });
        animatorSet.start();
    }

    public void stop() {
        started = false;
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        float density = getResources().getDisplayMetrics().density;
        float largeWidth = Math.min(getWidth() * 0.66f, 292f * density);
        float finalWidth = Math.min(getWidth() * 0.56f, 248f * density);
        float logoWidth = lerp(largeWidth, finalWidth, lockupProgress);
        float logoHeight = logoWidth * 84f / 324f;
        float centerY = getHeight() * 0.50f;
        float logoTop = centerY - logoHeight * 0.5f - lockupProgress * 14f * density;
        float logoLeft = (getWidth() - logoWidth) * 0.5f;

        int contentAlpha = Math.round(255f * (1f - exitProgress));
        logoPaint.setAlpha(contentAlpha);
        dropBasePaint.setAlpha(contentAlpha);
        waterPaint.setAlpha(contentAlpha);

        float lettersWidth = logoWidth * 235f / 324f;
        lettersBounds.set(logoLeft, logoTop + logoHeight * 7f / 84f,
                logoLeft + lettersWidth, logoTop + logoHeight);
        triangleBounds.set(logoLeft + lettersWidth, logoTop,
                logoLeft + logoWidth, logoTop + logoHeight);

        drawVectorLetters(canvas);
        buildDropPath();
        drawVectorTriangle(canvas, contentAlpha);
        drawWater(canvas);
        drawHighlight(canvas, contentAlpha);

        if (lockupProgress > 0f) {
            float sloganWidth = logoWidth;
            float sloganTop = logoTop + logoHeight + 14f * density;
            sloganPaint.setTextSize(logoWidth * 0.105f);
            sloganPaint.setTextScaleX(1f);
            sloganPaint.setTextScaleX(sloganWidth / sloganPaint.measureText(SLOGAN));
            sloganPaint.setAlpha(Math.round(255f * lockupProgress * (1f - exitProgress)));
            sloganPaint.getFontMetrics(sloganMetrics);
            canvas.drawText(SLOGAN, getWidth() * 0.5f,
                    sloganTop - sloganMetrics.top, sloganPaint);
        }
    }

    private void drawVectorLetters(Canvas canvas) {
        float left = lettersBounds.left;
        float top = lettersBounds.top;
        float sx = lettersBounds.width() / 235f;
        float sy = lettersBounds.height() / 77f;
        float radius = 4f * Math.min(sx, sy);

        lettersPath.rewind();
        lettersPath.setFillType(Path.FillType.EVEN_ODD);

        lettersPath.moveTo(left, top);
        lettersPath.lineTo(left + 29f * sx, top);
        lettersPath.cubicTo(left + 48f * sx, top, left + 59f * sx, top + 14f * sy,
                left + 59f * sx, top + 38.5f * sy);
        lettersPath.cubicTo(left + 59f * sx, top + 63f * sy, left + 48f * sx,
                top + 77f * sy, left + 29f * sx, top + 77f * sy);
        lettersPath.lineTo(left, top + 77f * sy);
        lettersPath.close();
        lettersPath.moveTo(left + 21f * sx, top + 21f * sy);
        lettersPath.lineTo(left + 28f * sx, top + 21f * sy);
        lettersPath.cubicTo(left + 38f * sx, top + 21f * sy, left + 43f * sx,
                top + 27f * sy, left + 43f * sx, top + 38.5f * sy);
        lettersPath.cubicTo(left + 43f * sx, top + 50f * sy, left + 38f * sx,
                top + 56f * sy, left + 28f * sx, top + 56f * sy);
        lettersPath.lineTo(left + 21f * sx, top + 56f * sy);
        lettersPath.close();

        canvas.drawPath(lettersPath, logoPaint);
        lettersPath.rewind();
        lettersPath.setFillType(Path.FillType.WINDING);

        addRoundedBar(64f, 0f, 85f, 77f, left, top, sx, sy, radius);
        addRoundedBar(64f, 0f, 125f, 21f, left, top, sx, sy, radius);
        addRoundedBar(64f, 28f, 120f, 49f, left, top, sx, sy, radius);
        addRoundedBar(64f, 56f, 125f, 77f, left, top, sx, sy, radius);
        addRoundedBar(129f, 0f, 150f, 77f, left, top, sx, sy, radius);
        addRoundedBar(129f, 56f, 184f, 77f, left, top, sx, sy, radius);
        addRoundedBar(169f, 0f, 235f, 21f, left, top, sx, sy, radius);
        addRoundedBar(191.5f, 0f, 212.5f, 77f, left, top, sx, sy, radius);

        canvas.drawPath(lettersPath, logoPaint);
    }

    private void addRoundedBar(float l, float t, float r, float b, float offsetX,
                               float offsetY, float sx, float sy, float radius) {
        lettersPath.addRoundRect(offsetX + l * sx, offsetY + t * sy,
                offsetX + r * sx, offsetY + b * sy, radius, radius, Path.Direction.CW);
    }

    private void drawVectorTriangle(Canvas canvas, int alpha) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        trianglePath.rewind();
        trianglePath.moveTo(left + width * 0.50f, top);
        trianglePath.cubicTo(left + width * 0.43f, top, left + width * 0.40f,
                top + height * 0.05f, left + width * 0.35f, top + height * 0.14f);
        trianglePath.lineTo(left + width * 0.02f, top + height * 0.73f);
        trianglePath.cubicTo(left - width * 0.05f, top + height * 0.87f,
                left + width * 0.06f, top + height, left + width * 0.20f, top + height);
        trianglePath.lineTo(left + width * 0.80f, top + height);
        trianglePath.cubicTo(left + width * 0.95f, top + height,
                left + width * 1.04f, top + height * 0.87f,
                left + width * 0.98f, top + height * 0.73f);
        trianglePath.lineTo(left + width * 0.65f, top + height * 0.14f);
        trianglePath.cubicTo(left + width * 0.60f, top + height * 0.05f,
                left + width * 0.57f, top, left + width * 0.50f, top);
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
        dropClipPath.cubicTo(left + width * 0.46f, top + height * 0.32f,
                left + width * 0.27f, top + height * 0.50f,
                left + width * 0.27f, top + height * 0.64f);
        dropClipPath.cubicTo(left + width * 0.27f, top + height * 0.83f,
                left + width * 0.39f, top + height * 0.90f,
                left + width * 0.50f, top + height * 0.90f);
        dropClipPath.cubicTo(left + width * 0.69f, top + height * 0.90f,
                left + width * 0.75f, top + height * 0.77f,
                left + width * 0.74f, top + height * 0.64f);
        dropClipPath.cubicTo(left + width * 0.72f, top + height * 0.50f,
                left + width * 0.55f, top + height * 0.32f,
                left + width * 0.50f, top + height * 0.22f);
        dropClipPath.close();
    }

    private void drawWater(Canvas canvas) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        float dropTop = top + height * 0.22f;
        float dropBottom = top + height * 0.89f;
        float surfaceY = lerp(dropBottom + height * 0.03f, dropTop - height * 0.03f,
                waterLevel);
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
        float leftSurfaceY = leftSlopeY - edgeCurl;
        float rightSurfaceY = rightSlopeY - edgeCurl;

        waterPath.rewind();
        waterPath.moveTo(waterLeft, leftSurfaceY);
        waterPath.cubicTo(
                waterLeft, leftSlopeY + edgeCurl * 0.76f,
                left + width * 0.36f, centerSurfaceY,
                waterCenter, centerSurfaceY);
        waterPath.cubicTo(
                left + width * 0.65f, centerSurfaceY,
                waterRight, rightSlopeY + edgeCurl * 0.76f,
                waterRight, rightSurfaceY);
        waterPath.lineTo(left + width, top + height);
        waterPath.lineTo(left, top + height);
        waterPath.close();

        canvas.save();
        canvas.clipPath(dropClipPath);
        canvas.drawPath(waterPath, waterPaint);
        canvas.restore();
    }

    /** Keeps the logo's light trail above the water throughout the whole fill. */
    private void drawHighlight(Canvas canvas, int alpha) {
        float left = triangleBounds.left;
        float top = triangleBounds.top;
        float width = triangleBounds.width();
        float height = triangleBounds.height();

        highlightPaint.setAlpha(alpha);
        highlightPaint.setStrokeWidth(width * 0.058f);
        highlightPath.rewind();
        highlightPath.moveTo(left + width * 0.63f, top + height * 0.64f);
        highlightPath.cubicTo(
                left + width * 0.63f, top + height * 0.72f,
                left + width * 0.57f, top + height * 0.78f,
                left + width * 0.47f, top + height * 0.78f);

        canvas.save();
        canvas.clipPath(dropClipPath);
        canvas.drawPath(highlightPath, highlightPaint);
        canvas.restore();
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
