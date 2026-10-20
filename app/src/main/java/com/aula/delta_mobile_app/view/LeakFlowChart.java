package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.LeakMonitoringData;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class LeakFlowChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path area = new Path(), line = new Path();
    private LeakMonitoringData.Event event;

    public LeakFlowChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEvent(LeakMonitoringData.Event value) {
        event = value;
        StringBuilder description = new StringBuilder(getResources().getString(R.string.leak_night_title));
        if (event != null)
            for (LeakMonitoringData.Sample sample : event.samples)
                description.append(", ").append(label(sample.timestamp)).append(": ").append(sample.litersPerMinute == null ? "sem medição" : sample.litersPerMinute + " L/min");
        setContentDescription(event == null || event.samples.isEmpty() ? getResources().getString(R.string.leak_chart_empty) : description.toString());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 2 * d, right = getWidth() - 2 * d, top = 18 * d, bottom = getHeight() - 28 * d;
        if (right <= left || bottom <= top) return;
        long start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = start + 6 * 60 * 60 * 1000L;
        List<LeakMonitoringData.Sample> samples = event == null ? Collections.emptyList() : event.samples;
        if (event != null && event.windowEnd > event.windowStart) {
            start = event.windowStart;
            end = event.windowEnd;
        } else if (!samples.isEmpty()) {
            start = samples.get(0).timestamp;
            end = Math.max(start + 60 * 60 * 1000L, samples.get(samples.size() - 1).timestamp);
        }
        double maximum = 1;
        for (LeakMonitoringData.Sample sample : samples)
            if (sample.timestamp >= start && sample.timestamp <= end && sample.litersPerMinute != null)
                maximum = Math.max(maximum, sample.litersPerMinute.doubleValue());
        maximum *= 1.1;
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(0xFFBBBBBB);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("L/min", left, 12 * d, paint);
        List<float[]> segment = new ArrayList<>();
        for (LeakMonitoringData.Sample sample : samples) {
            if (sample.timestamp < start || sample.timestamp > end) continue;
            if (sample.litersPerMinute == null) {
                drawSegment(canvas, segment, bottom, d);
                segment.clear();
                continue;
            }
            float x = left + (right - left) * (float) ((double) (sample.timestamp - start) / (end - start));
            float y = bottom
            - (bottom - top) * (float) (sample.litersPerMinute.doubleValue() / maximum);
            segment.add(new float[] {x, y});
        }
        drawSegment(canvas, segment, bottom, d);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF7B8390);
        paint.setStrokeWidth(d);
        canvas.drawLine(left, bottom, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF3F3F3);
        paint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
        for (int i = 0; i < 3; i++) {
            paint.setTextAlign(i == 0 ? Paint.Align.LEFT : i == 2 ? Paint.Align.RIGHT : Paint.Align.CENTER);
            canvas.drawText(label(start + (end - start) * i / 2), left + (right - left) * i / 2, bottom + 22 * d, paint);
        }
    }

    private void drawSegment(Canvas canvas, List<float[]> points, float bottom, float d) {
        if (points.isEmpty()) return;
        line.rewind();
        area.rewind();
        area.moveTo(points.get(0)[0], bottom);
        for (int i = 0; i < points.size(); i++) {
            float[] point = points.get(i);
            if (i == 0) line.moveTo(point[0], point[1]);
            else line.lineTo(point[0], point[1]);
            area.lineTo(point[0], point[1]);
        }
        area.lineTo(points.get(points.size() - 1)[0], bottom);
        area.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB34C4C4C);
        canvas.drawPath(area, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF748092);
        paint.setStrokeWidth(1.8f * d);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawPath(line, paint);
        if (points.size() == 1) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(points.get(0)[0], points.get(0)[1], 2 * d, paint);
        }
    }

    private String label(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("mm")).equals("00") ? Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH'h'")) : Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
