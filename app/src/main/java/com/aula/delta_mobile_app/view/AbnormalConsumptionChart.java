package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import com.aula.delta_mobile_app.R;
import com.aula.delta_mobile_app.model.AbnormalConsumptionData;
import com.aula.delta_mobile_app.model.AbnormalConsumptionData.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class AbnormalConsumptionChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private AbnormalConsumptionData data;
    private Period period = Period.WEEK;
    private boolean bars;
    private BigDecimal baseline;
    private LocalDate anomaly;

    public AbnormalConsumptionChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHistory(AbnormalConsumptionData value) {
        data = value;
        bars = false;
        describe();
        invalidate();
    }

    public void setComparison(AbnormalConsumptionData value, Period period) {
        data = value;
        this.period = period;
        bars = true;
        describe();
        invalidate();
    }

    public void setConfirmedDiagnosis(BigDecimal baseline, LocalDate anomaly) {
        this.baseline = baseline != null && baseline.signum() >= 0 && Double.isFinite(baseline.doubleValue()) ? baseline : null;
        this.anomaly = anomaly;
        invalidate();
    }

    private void describe() {
        if (data == null) {
            setContentDescription(getResources().getString(R.string.abnormal_graph_unavailable));
            return;
        }
        StringBuilder description = new StringBuilder();
        if (bars)
            for (Bucket bucket : data.periods(period))
                description.append(bucket.label).append(": ").append(number(bucket.liters)).append(" L. ");
        else
            for (int i = 6; i >= 0; i--) {
            LocalDate day = data.reference.minusDays(i);
            description.append(day).append(": ").append(number(data.day(day))).append(" L. ");
        }
        setContentDescription(description);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 8 * d, right = getWidth() - 8 * d, top = 12 * d, bottom = getHeight() - 26 * d;
        if (right <= left || bottom <= top) return;
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF555B65);
        paint.setStrokeWidth(d);
        canvas.drawLine(left, bottom, right, bottom, paint);
        if (data == null) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(0xFFABB3C2);
            canvas.drawText("—", (left + right) / 2, (top + bottom) / 2, paint);
            return;
        }
        double maximum = 1;
        if (bars) {
            for (Bucket bucket : data.periods(period))
                maximum = Math.max(maximum, bucket.liters.doubleValue());
        } else {
            for (int i = 6; i >= 0; i--) {
                BigDecimal liters = data.day(data.reference.minusDays(i));
                if (liters != null) maximum = Math.max(maximum, liters.doubleValue());
            }
        }
        if (!bars && baseline != null) maximum = Math.max(maximum, baseline.doubleValue());
        maximum *= 1.15;
        if (bars) {
            List<Bucket> values = data.periods(period);
            int[] colors = {
                0xFF9ABCF8, 0xFF5686EB, 0xFF224D9B, anomaly == null ? 0xFF3978ED : 0xFFD84057
            };
            float slot = (right - left) / values.size();
            for (int i = 0; i < values.size(); i++) {
                float x = left + slot * i + slot * .08f;
                float y = bottom
                - (float) (values.get(i).liters.doubleValue() / maximum) * (bottom - top);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colors[i]);
                canvas.drawRoundRect(x, y, x + slot * .84f, bottom, 2 * d, 2 * d, paint);
                paint.setColor(0xFFE9ECF3);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(values.get(i).label, left + slot * (i + .5f), bottom + 20 * d, paint);
            }
        } else {
            if (baseline != null) {
                float y = bottom - (float) (baseline.doubleValue() / maximum) * (bottom - top);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(0xFF999999);
                paint.setStrokeWidth(d);
                paint.setPathEffect(new DashPathEffect(new float[] {4 * d, 4 * d}, 0));
                canvas.drawLine(left, y, right, y, paint);
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(number(baseline) + " L (média)", left, y - 5 * d, paint);
            }
            path.rewind();
            boolean continuous = false;
            for (int i = 0; i < 7; i++) {
                LocalDate day = data.reference.minusDays(6 - i);
                BigDecimal value = data.day(day);
                if (value == null) {
                    continuous = false;
                    continue;
                }
                float x = left + (right - left) * i / 6, y = bottom - (float) (value.doubleValue() / maximum) * (bottom - top);
                if (continuous) path.lineTo(x, y);
                else path.moveTo(x, y);
                continuous = true;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(anomaly == null ? 0xFF719CEC : 0xFFE43F56);
                canvas.drawCircle(x, y, 2 * d, paint);
                if (day.equals(anomaly)) {
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("anomalia →", Math.max(left + 38 * d, Math.min(right - 38 * d, x)), Math.max(top + 12 * d, y - 10 * d), paint);
                }
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(anomaly == null ? 0xFF719CEC : 0xFFE43F56);
            paint.setStrokeWidth(2 * d);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFBCC4D2);
            for (int i = 0; i < 3; i++) {
                paint.setTextAlign(i == 0 ? Paint.Align.LEFT : i == 2 ? Paint.Align.RIGHT : Paint.Align.CENTER);
                canvas.drawText(data.reference.minusDays(6 - i * 3).format(DateTimeFormatter.ofPattern("dd/MM")), left + (right - left) * i / 2, bottom + 20 * d, paint);
            }
        }
    }

    private String number(BigDecimal value) {
        if (value == null) return "—";
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        format.setMaximumFractionDigits(1);
        return format.format(value);
    }
}
