package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;

import java.util.*;

public class HomeTemperatureChart extends View {
    public static final class Point {
        public final int hour;
        public final double celsius, liters;

        public Point(int hour, double liters, double celsius) {
            this.hour = hour;
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
                if (point != null && Double.isFinite(point.celsius) && (Double.isNaN(point.liters) || Double.isFinite(point.liters) && point.liters >= 0)) points.add(point);
        points.sort(Comparator.comparingInt(point -> point.hour));
        StringBuilder description = new StringBuilder(getResources().getString(R.string.home_temperature_title));
        for (Point point : points) {
            description.append(", ").append(String.format(Locale.getDefault(), "%02dh", point.hour)).append(": ").append(point.celsius).append(" °C");
            if (Double.isFinite(point.liters)) description.append(", estimativa ").append(point.liters).append(" L");
        }
        setContentDescription(points.isEmpty() ? getResources().getString(R.string.home_temperature_empty) : description);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 42 * d, right = getWidth() - 42 * d, top = 48 * d, bottom = getHeight() - 32 * d;
        if (right <= left || bottom <= top) return;
        double maxLiters = 100;
        if (!points.isEmpty()) {
            for (Point point : points) if (Double.isFinite(point.liters)) maxLiters = Math.max(maxLiters, point.liters);
            maxLiters = Math.ceil(maxLiters / 100) * 100;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(0xFFFFFFFF);
        int[] hours = {0, 4, 8, 12, 16, 20, 24};
        for (int hour : hours) {
            float x = left + hour / 24f * (right - left);
            paint.setTextAlign(hour == 0 ? Paint.Align.LEFT : hour == 24 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(hour == 24 ? "24h" : String.format(Locale.getDefault(), "%02dh", hour), x, bottom + 18 * d, paint);
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("L/h", left - 7 * d, top - 28 * d, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("°C", right + 7 * d, top - 28 * d, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        if (points.isEmpty()) {
            paint.setColor(0xFF999999);
            canvas.drawText("—", (left + right) / 2, (top + bottom) / 2, paint);
            return;
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(9 * getResources().getDisplayMetrics().scaledDensity);
        paint.setStrokeWidth(d);
        for (int liters = 0; liters <= maxLiters; liters += 100) {
            float y = bottom - liters / (float) maxLiters * (bottom - top);
            paint.setColor(0x665E6875);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(left, y, right, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(String.valueOf(liters), left - 7 * d, y - (metrics.ascent + metrics.descent) / 2, paint);
        }
        if (maxLiters >= 50) {
            float y = bottom - 50 / (float) maxLiters * (bottom - top);
            paint.setColor(0xCCFFFFFF);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(left, y, right, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("50", left - 7 * d, y - (paint.getFontMetrics().ascent + paint.getFontMetrics().descent) / 2, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xFF91B5FF);
        for (int degrees = 5; degrees <= 35; degrees += 5) {
            float y = bottom - degrees / 35f * (bottom - top);
            canvas.drawText(String.valueOf(degrees), right + 7 * d, y, paint);
            if (degrees < 35) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setPathEffect(new DashPathEffect(new float[] {3 * d, 4 * d}, 0));
                canvas.drawLine(left, y, right, y, paint);
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFCCCCCC);
        paint.setStrokeWidth(d);
        canvas.drawLine(left, top, left, bottom, paint);
        canvas.drawLine(left, bottom, right, bottom, paint);
        curve.rewind();
        Path temperatureCurve = new Path();
        float previousX = 0;
        float previousTemperatureY = 0;
        boolean hasPreviousLiters = false;
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            float x = left + point.hour / 24f * (right - left);
            float temperatureY = bottom - (float) (Math.max(0, Math.min(35, point.celsius)) / 35) * (bottom - top);
            if (i == 0) {
                temperatureCurve.moveTo(x, temperatureY);
            } else {
                float middle = (previousX + x) / 2;
                temperatureCurve.cubicTo(middle, previousTemperatureY, middle, temperatureY, x, temperatureY);
            }
            if (Double.isFinite(point.liters)) {
                float y = bottom - (float) (Math.min(point.liters, maxLiters) / maxLiters) * (bottom - top);
                if (!hasPreviousLiters) curve.moveTo(x, y);
                else curve.lineTo(x, y);
                hasPreviousLiters = true;
            }
            previousX = x;
            previousTemperatureY = temperatureY;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * d);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.WHITE);
        canvas.drawPath(curve, paint);
        paint.setColor(0xFF91B5FF);
        paint.setPathEffect(new DashPathEffect(new float[] {5 * d, 4 * d}, 0));
        canvas.drawPath(temperatureCurve, paint);
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
        for (Point point : points) {
            float x = left + point.hour / 24f * (right - left);
            if (Double.isFinite(point.liters)) {
                float y = bottom - (float) (Math.min(point.liters, maxLiters) / maxLiters) * (bottom - top);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, 4 * d, paint);
                paint.setColor(0xFF6497F0);
                canvas.drawCircle(x, y, 3 * d, paint);
            }
            float temperatureY = bottom - (float) (Math.max(0, Math.min(35, point.celsius)) / 35) * (bottom - top);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, temperatureY, 4 * d, paint);
            paint.setColor(0xFF91B5FF);
            canvas.drawCircle(x, temperatureY, 3 * d, paint);
        }
    }
}
