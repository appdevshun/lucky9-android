package com.lucky9.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** 5-segment win-bar drawn programmatically. */
public class WinBarView extends View {

    private static final int SEGMENTS = 5;
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int filled = 0;

    public WinBarView(Context c) { super(c); init(); }
    public WinBarView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public WinBarView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        emptyPaint.setColor(0x33FFFFFF);
        emptyPaint.setStyle(Paint.Style.FILL);
        filledPaint.setColor(0xFFF4C542);
        filledPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);
    }

    public void setFilled(int n) {
        if (n < 0) n = 0;
        if (n > SEGMENTS) n = SEGMENTS;
        if (n != filled) {
            filled = n;
            invalidate();
        }
    }

    public int getFilled() {
        return filled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float gap = Math.max(2f, w * 0.01f);
        float segW = (w - gap * (SEGMENTS - 1)) / (float) SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            float x = i * (segW + gap);
            RectF r = new RectF(x, 0f, x + segW, h);
            canvas.drawRoundRect(r, 2f, 2f, i < filled ? filledPaint : emptyPaint);
            canvas.drawRoundRect(r, 2f, 2f, borderPaint);
        }
    }
}
