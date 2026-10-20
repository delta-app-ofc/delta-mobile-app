package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;

import java.util.*;

public class HomeTemperatureChart extends View {
    public static final class Point {
        public final double celsius, liters;

        public Point(double celsius, double liters) {
            this.celsius = celsius;
            this.liters = liters;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curve = new Path();
    private final List<Point> points = new ArrayList<>();

    public HomeTemperatureChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPoints(List<Point> values) {
        points.clear();
        if (values != null)
            for (Point point : values)
                if (point != null && Double.isFinite(point.celsius) && Double.isFinite(point.liters) && point.liters >= 0) points.add(point);
        points.sort(Comparator.comparingDouble(point -> point.celsius));
        StringBuilder description = new StringBuilder(getResources().getString(R.string.home_temperature_title));
        for (Point point : points)
            description.append(", ").append(point.celsius).append(" °C: ").append(point.liters).append(" L");
        setContentDescription(points.isEmpty() ? getResources().getString(R.string.home_temperature_empty) : description);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 40 * d, right = getWidth() - 16 * d, top = 8 * d, bottom = getHeight() - 28 * d;
        if (right <= left || bottom <= top) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFCCCCCC);
        paint.setStrokeWidth(d);
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        double min = points.isEmpty() ? 15 : Math.floor(points.get(0).celsius / 5) * 5;
        double max = points.isEmpty() ? 35 : Math.ceil(points.get(points.size() - 1).celsius / 5) * 5;
        if (max <= min) {
            min -= 5;
            max += 5;
        }
        double maxLiters = 400;
        if (!points.isEmpty()) {
            maxLiters = 1;
            for (Point point : points) maxLiters = Math.max(maxLiters, point.liters);
            maxLiters = Math.ceil(maxLiters * 1.1 / 100) * 100;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 3; i++) {
            paint.setTextAlign(i == 0 ? Paint.Align.LEFT : i == 2 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(String.format(Locale.getDefault(), "%.0f°C", min + (max - min) * i / 2), left + (right - left) * i / 2, bottom + 18 * d, paint);
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("L", left - 10 * d, top + 8 * d, paint);
        if (points.isEmpty()) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(0xFF999999);
            canvas.drawText("—", (left + right) / 2, (top + bottom) / 2, paint);
            return;
        }
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", maxLiters), left - 10 * d, top + 24 * d, paint);
        canvas.drawText("0", left - 10 * d, bottom, paint);
        curve.rewind();
        float previousX = 0, previousY = 0;
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            float x = left + (float) ((point.celsius - min) / (max - min)) * (right - left);
            float y = bottom - (float) (point.liters / maxLiters) * (bottom - top);
            if (i == 0) curve.moveTo(x, y);
            else {
                float middle = (previousX + x) / 2;
                curve.cubicTo(middle, previousY, middle, y, x, y);
            }
            previousX = x;
            previousY = y;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * d);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.WHITE);
        canvas.drawPath(curve, paint);
        paint.setStyle(Paint.Style.FILL);
        for (Point point : points) {
            float x = left + (float) ((point.celsius - min) / (max - min)) * (right - left);
            float y = bottom - (float) (point.liters / maxLiters) * (bottom - top);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, 4 * d, paint);
            paint.setColor(0xFF6497F0);
            canvas.drawCircle(x, y, 3 * d, paint);
        }
    }
}
