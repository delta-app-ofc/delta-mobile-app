package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.DashboardMetrics;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DashboardHistoryChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private DashboardMetrics metrics;

    public DashboardHistoryChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMetrics(DashboardMetrics value) {
        metrics = value;
        StringBuilder text = new StringBuilder(getResources().getString(R.string.dash_history));
        if (value != null)
            for (int i = 0; i < 6; i++)
                text.append(", ").append(value.firstMonth.plusMonths(i)).append(": ").append(value.months[i] == null ? "sem dados" : value.months[i] + " m³");
        setContentDescription(text);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float labelSize = 10 * getResources().getDisplayMetrics().scaledDensity;
        float left = 24 * d, right = getWidth() - 24 * d, top = labelSize + 14 * d, bottom = getHeight() - 24 * d;
        if (right <= left || bottom <= top) return;
        double maximum = 0;
        int count = 0, last = -1;
        if (metrics != null)
            for (int i = 0; i < 6; i++)
                if (metrics.months[i] != null) {
            maximum = Math.max(maximum, metrics.months[i].doubleValue());
            count++;
            last = i;
        }
        maximum = Math.max(1, maximum) * 1.1;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFB5B5BD);
        paint.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
        for (int i = 0; i < 6; i++) {
            java.time.YearMonth month = (metrics == null ? java.time.YearMonth.now().minusMonths(5) : metrics.firstMonth).plusMonths(i);
            String label = month.format(DateTimeFormatter.ofPattern("MMM", new Locale("pt", "BR"))).replace(".", "");
            paint.setTextAlign(i == 0 ? Paint.Align.LEFT : i == 5 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(label, left + (right - left) * i / 5, getHeight() - 2 * d, paint);
        }
        if (count == 0) {
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.home_chart_empty), (left + right) / 2, (top + bottom) / 2, paint);
            return;
        }
        path.reset();
        boolean connected = false;
        for (int i = 0; i < 6; i++) {
            if (metrics.months[i] == null) {
                connected = false;
                continue;
            }
            float x = left + (right - left) * i / 5;
            float y = bottom - (float) (metrics.months[i].doubleValue() / maximum) * (bottom - top);
            if (!connected) path.moveTo(x, y);
            else path.lineTo(x, y);
            connected = true;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3.5f * d);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(path, paint);
        NumberFormat numbers = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        numbers.setMaximumFractionDigits(3);
        for (int i = 0; i < 6; i++)
            if (metrics.months[i] != null) {
            float x = left + (right - left) * i / 5, y = bottom
            - (float) (metrics.months[i].doubleValue() / maximum) * (bottom - top);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == last ? 0xFF3868E8 : Color.WHITE);
            canvas.drawCircle(x, y, i == last ? 3.5f * d : 1.8f * d, paint);
            if (i == last) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(d);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, 3.5f * d, paint);
            }
            String value = numbers.format(metrics.months[i]);
            if (metrics.months[i].signum() > 0 && metrics.months[i].compareTo(new java.math.BigDecimal("0.001")) < 0)
                value = "< 0,001";
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(labelSize);

            float available = Math.min((right - left) / 5 - 6 * d, 44 * d);
            float width = paint.measureText(value);
            if (width > available) paint.setTextSize(labelSize * available / width);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(value, x, y - 9 * d, paint);
        }
    }
}
