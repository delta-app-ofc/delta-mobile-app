package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.ApiModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeConsumptionChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path line = new Path();
    private List<ApiModels.ConsumptionDailyResponse> points = new ArrayList<>();

    public HomeConsumptionChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setReadings(List<ApiModels.ConsumptionDailyResponse> readings) {
        points = new ArrayList<>(readings);
        StringBuilder description = new StringBuilder(getResources().getString(R.string.home_history));
        for (ApiModels.ConsumptionDailyResponse item : points)
            description.append(", ").append(item.fullDate).append(": ").append(item.totalLiters).append(" L");
        setContentDescription(description);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 42 * d, right = getWidth() - 12 * d, top = 16 * d, bottom = getHeight() - 34 * d;
        if (right <= left || bottom <= top) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(d);
        paint.setColor(0xFF666666);
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFBBBBBB);
        paint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        if (points.isEmpty()) {
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getResources().getString(R.string.home_chart_empty), (left + right) / 2, (top + bottom) / 2, paint);
            return;
        }
        double maximum = 1;
        for (ApiModels.ConsumptionDailyResponse item : points)
            maximum = Math.max(maximum, item.totalLiters.doubleValue());
        maximum *= 1.15;
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "%.0f L", maximum), left - 6 * d, top + 6 * d, paint);
        canvas.drawText("0", left - 6 * d, bottom, paint);
        line.reset();
        long start = java.time.LocalDate.parse(points.get(0).fullDate).toEpochDay();
        long span = Math.max(1, java.time.LocalDate.parse(points.get(points.size() - 1).fullDate).toEpochDay() - start);
        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? (left + right) / 2 : left + (right - left) * (java.time.LocalDate.parse(points.get(i).fullDate).toEpochDay() - start) / span;
            float y = bottom
            - (float) (points.get(i).totalLiters.doubleValue() / maximum) * (bottom - top);
            if (i == 0) line.moveTo(x, y);
            else line.lineTo(x, y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * d);
        paint.setColor(0xFF8EADE9);
        canvas.drawPath(line, paint);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < points.size(); i++) {
            float x = points.size() == 1 ? (left + right) / 2 : left + (right - left) * (java.time.LocalDate.parse(points.get(i).fullDate).toEpochDay() - start) / span;
            float y = bottom
            - (float) (points.get(i).totalLiters.doubleValue() / maximum) * (bottom - top);
            canvas.drawCircle(x, y, 4 * d, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xFFBBBBBB);
        canvas.drawText(label(points.get(0).fullDate), left, bottom + 22 * d, paint);
        if (points.size() > 1) {
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label(points.get(points.size() - 1).fullDate), right, bottom + 22 * d, paint);
        }
    }

    private String label(String date) {
        return java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
    }
}
