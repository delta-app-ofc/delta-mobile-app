package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.MonthlyGoalProgress;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class HomeGoalProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private BigDecimal percent;
    private boolean exceeded;
    private String value = "—";
    private String caption;

    public HomeGoalProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setValues(null, null);
    }

    public void setValues(BigDecimal goal, BigDecimal consumed) {
        percent = MonthlyGoalProgress.percentage(goal, consumed);
        exceeded = percent != null && consumed.compareTo(goal) > 0;
        value = percent == null ? "—" : NumberFormat.getIntegerInstance(new Locale("pt", "BR")).format(percent) + "%";
        caption = getResources().getString(percent != null ? R.string.home_goal_consumed_caption : goal == null || goal.signum() <= 0 ? R.string.home_goal_progress_missing_goal : R.string.home_goal_progress_missing_consumption);
        setContentDescription(percent == null ? caption.replace('\n', ' ') : getResources().getString(R.string.home_goal_progress_description, value));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float diameter = Math.min(getWidth(), getHeight()) - 16 * density;
        if (diameter <= 0) return;
        float stroke = Math.min(12 * density, diameter / 12);
        float radius = (diameter - stroke) / 2;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFF2E3032);
        canvas.drawOval(bounds, paint);
        if (percent != null && percent.signum() > 0) {
            int[] colors = exceeded ? new int[] {0xFFF2AAAA, 0xFFE56C6C, 0xFFCA303F} : new int[] {0xFFADC3EF, 0xFF819FDD, 0xFF4D7CD1};
            paint.setShader(new LinearGradient(bounds.left, cy, bounds.right, cy, colors, null, Shader.TileMode.CLAMP));
            canvas.drawArc(bounds, -90, -percent.min(BigDecimal.valueOf(100)).floatValue() * 3.6f, false, paint);
            paint.setShader(null);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        paint.setColor(exceeded ? 0xFFEF6B73 : 0xFF3978ED);
        paint.setTextSize(40 * scaled);
        float available = radius * 1.6f;
        if (paint.measureText(value) > available) paint.setTextSize(paint.getTextSize() * available / paint.measureText(value));
        canvas.drawText(value, cx, cy - 2 * density, paint);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        paint.setColor(0xFFE9ECF3);
        paint.setTextSize(13 * scaled);
        String[] lines = caption.split("\n");
        for (String line : lines) if (paint.measureText(line) > available) paint.setTextSize(paint.getTextSize() * available / paint.measureText(line));
        float y = cy + 20 * density;
        for (String line : lines) {
            canvas.drawText(line, cx, y, paint);
            y += paint.getFontSpacing();
        }
    }
}
