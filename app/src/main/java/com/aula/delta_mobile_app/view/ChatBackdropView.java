package com.aula.delta_mobile_app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public final class ChatBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient top, bottom;

    public ChatBackdropView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == 0 || h == 0) return;
        top = new RadialGradient(w * 1.03f, h * .12f, w * .72f, new int[] { 0x88517DC9, Color.TRANSPARENT }, null, Shader.TileMode.CLAMP);
        bottom = new RadialGradient(-w * .15f, h * .76f, w * .75f, new int[] { 0xBB7295E0, Color.TRANSPARENT }, null, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setShader(top);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(bottom);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
