package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class FloatingMenuBackground extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public FloatingMenuBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float d = getResources().getDisplayMetrics().density;
        float top = 36 * d, radius = 28 * d, center = w / 2f;
        paint.setShader(new LinearGradient(0, top, w, h, 0xFF252C36, 0xFF303333, Shader.TileMode.CLAMP));
        float shoulder = Math.min(68 * d, w * .22f);
        path.reset();
        path.moveTo(radius, top);
        path.lineTo(center - shoulder, top);
        path.cubicTo(center - shoulder * .65f, top, center - shoulder * .45f, top + 20 * d, center, top + 20 * d);
        path.cubicTo(center + shoulder * .45f, top + 20 * d, center + shoulder * .65f, top, center + shoulder, top);
        path.lineTo(w - radius, top);
        path.quadTo(w, top, w, top + radius);
        path.lineTo(w, h - radius);
        path.quadTo(w, h, w - radius, h);
        path.lineTo(radius, h);
        path.quadTo(0, h, 0, h - radius);
        path.lineTo(0, top + radius);
        path.quadTo(0, top, radius, top);
        path.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }
}
