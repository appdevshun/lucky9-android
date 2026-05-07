package com.lucky9.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.lucky9.app.engine.Card;

/**
 * A small playing-card view that paints the rank + suit centered.
 * If {@link #setCard(Card, boolean)} face-down=true, paints a navy card back.
 */
public class CardFaceView extends View {

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint suitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rankPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Card card;
    private boolean faceDown = true;

    public CardFaceView(Context c) { super(c); init(); }
    public CardFaceView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public CardFaceView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        cardPaint.setColor(Color.parseColor("#FFFFF8E7"));
        cardPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.parseColor("#888888"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        suitPaint.setStyle(Paint.Style.FILL);
        suitPaint.setTextAlign(Paint.Align.CENTER);
        rankPaint.setStyle(Paint.Style.FILL);
        rankPaint.setTextAlign(Paint.Align.CENTER);
        rankPaint.setFakeBoldText(true);
        backPaint.setColor(Color.parseColor("#FF1B3F6C"));
        backPaint.setStyle(Paint.Style.FILL);
        backStrokePaint.setColor(Color.parseColor("#FFF4C542"));
        backStrokePaint.setStyle(Paint.Style.STROKE);
        backStrokePaint.setStrokeWidth(2f);
    }

    public void setCard(@Nullable Card card, boolean faceDown) {
        this.card = card;
        this.faceDown = faceDown;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = Math.min(w, h) * 0.12f;
        RectF rect = new RectF(1f, 1f, w - 1f, h - 1f);

        if (faceDown || card == null) {
            canvas.drawRoundRect(rect, r, r, backPaint);
            canvas.drawRoundRect(rect, r, r, backStrokePaint);
            float inset = Math.min(w, h) * 0.12f;
            RectF inner = new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
            canvas.drawRoundRect(inner, r * 0.6f, r * 0.6f, backStrokePaint);
            return;
        }

        canvas.drawRoundRect(rect, r, r, cardPaint);
        canvas.drawRoundRect(rect, r, r, borderPaint);

        int color = card.suit().isRed() ? Color.parseColor("#FFC62828") : Color.parseColor("#FF1B1B1B");
        rankPaint.setColor(color);
        suitPaint.setColor(color);

        rankPaint.setTextSize(h * 0.40f);
        suitPaint.setTextSize(h * 0.34f);

        // Centered rank + suit stacked.
        canvas.drawText(card.rank().label(), w / 2f, h * 0.45f, rankPaint);
        canvas.drawText(card.suit().glyph(), w / 2f, h * 0.85f, suitPaint);
    }
}
