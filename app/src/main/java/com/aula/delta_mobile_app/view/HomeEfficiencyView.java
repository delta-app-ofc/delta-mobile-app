package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeEfficiencyView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Double score;
    private String classification;

    public HomeEfficiencyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScore(null, null);
    }

    public void setScore(Double value, String label) {
        score = value != null && Double.isFinite(value) && value >= 0 && value <= 100 ? value : null;
        classification = score == null ? null : label;
        setContentDescription(score == null ? getResources().getString(R.string.home_eh_accessibility) : "Indicador EH: " + number(score) + " de 100"
            + (label == null ? "" : ", " + label));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float sp = getResources().getDisplayMetrics().scaledDensity;
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f, radius = size / 2 - 8 * d;
        if (radius <= 0) return;
        RectF bounds = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(14 * d);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setShader(null);
        paint.setColor(0xFF363636);
        canvas.drawOval(bounds, paint);
        if (score != null && score > 0) {
            SweepGradient gradient = new SweepGradient(cx, cy, new int[] {0xFF86A5E6, 0xFF4878D8, 0xFFACC3F1, 0xFF86A5E6}, null);
            Matrix matrix = new Matrix();
            matrix.setRotate(-90, cx, cy);
            gradient.setLocalMatrix(matrix);
            paint.setShader(gradient);
            canvas.drawArc(bounds, -90, (float) (score * 3.6), false, paint);
            paint.setShader(null);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setColor(0xFF3274EA);
        paint.setTextSize(42 * sp);
        canvas.drawText(score == null ? "—" : number(score), cx, cy - 12 * d, paint);
        paint.setColor(0xFF999999);
        paint.setTextSize(16 * sp);
        canvas.drawText("/100", cx, cy + 10 * d, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(16 * sp);
        String label = classification == null || classification.trim().isEmpty() ? "—" : classification;

        float available = radius * 1.55f;
        if (paint.measureText(label) > available)
            paint.setTextSize(paint.getTextSize() * available / paint.measureText(label));
        canvas.drawText(label, cx, cy + 34 * d, paint);
    }

    private String number(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(1);
        return format.format(value);
    }
}
