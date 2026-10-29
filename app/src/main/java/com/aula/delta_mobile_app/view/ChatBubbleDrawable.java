package com.aula.delta_mobile_app.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public final class ChatBubbleDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tail = new Path();
    private final RectF body = new RectF();
    private final boolean user;
    private final float density;
    private final int start, end;

    public ChatBubbleDrawable(boolean user, float density, int start, int end) {
        this.user = user;
        this.density = density;
        this.start = start;
        this.end = end;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        body.set(bounds.left, bounds.top, bounds.right, bounds.bottom - 8 * density);
        paint.setShader(user ? new LinearGradient(body.left, 0, body.right, 0, start, end, Shader.TileMode.CLAMP) : null);
        paint.setColor(start);
        tail.reset();
        if (user) {
            tail.moveTo(body.right - 24 * density, body.bottom - 9 * density);
            tail.quadTo(body.right - 9 * density, body.bottom + 2 * density, body.right - 3 * density, bounds.bottom);
            tail.quadTo(body.right, bounds.bottom + density, body.right, bounds.bottom - 3 * density);
            tail.lineTo(body.right, body.bottom - 16 * density);
        } else {
            tail.moveTo(body.left + 24 * density, body.bottom - 9 * density);
            tail.quadTo(body.left + 9 * density, body.bottom + 2 * density, body.left + 3 * density, bounds.bottom);
            tail.quadTo(body.left, bounds.bottom + density, body.left, bounds.bottom - 3 * density);
            tail.lineTo(body.left, body.bottom - 16 * density);
        }
        tail.close();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(body, 16 * density, 16 * density, paint);
        canvas.drawPath(tail, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
