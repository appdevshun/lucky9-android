package com.lucky9.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Felt-green oval table with gold border, jackpot + round labels in the center. */
public class OvalTableView extends View {

    private final Paint feltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String jackpotText = "Jackpot 0";
    private String roundText = "Round 0";

    public OvalTableView(Context c) { super(c); init(); }
    public OvalTableView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public OvalTableView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.parseColor("#FFF4C542"));
        titlePaint.setColor(Color.parseColor("#FFF4C542"));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setFakeBoldText(true);
        subPaint.setColor(Color.parseColor("#FFFFF8E7"));
        subPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setJackpot(String s) { jackpotText = s; invalidate(); }
    public void setRound(String s) { roundText = s; invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float pad = Math.min(w, h) * 0.05f;
        RectF oval = new RectF(pad, pad + h * 0.10f, w - pad, h - pad - h * 0.10f);
        float cx = oval.centerX();
        float cy = oval.centerY();
        float radius = Math.max(oval.width(), oval.height()) / 2f;

        feltPaint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{Color.parseColor("#FF1B7A3F"), Color.parseColor("#FF0F5C2E"), Color.parseColor("#FF073A1B")},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(oval, feltPaint);
        canvas.drawOval(oval, borderPaint);

        // Inner felt outline
        Paint innerStroke = new Paint(borderPaint);
        innerStroke.setStrokeWidth(2f);
        innerStroke.setColor(Color.parseColor("#88F4C542"));
        RectF innerRing = new RectF(oval.left + 14, oval.top + 14, oval.right - 14, oval.bottom - 14);
        canvas.drawOval(innerRing, innerStroke);

        // Center jackpot + round
        titlePaint.setTextSize(Math.min(w, h) * 0.06f);
        subPaint.setTextSize(Math.min(w, h) * 0.045f);
        canvas.drawText(jackpotText, cx, cy - 4, titlePaint);
        canvas.drawText(roundText, cx, cy + Math.min(w, h) * 0.06f, subPaint);
    }
}
