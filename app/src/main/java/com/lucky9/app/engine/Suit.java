package com.lucky9.app.engine;

public enum Suit {
    HEARTS("\u2665"),
    DIAMONDS("\u2666"),
    CLUBS("\u2663"),
    SPADES("\u2660");

    private final String glyph;

    Suit(String glyph) {
        this.glyph = glyph;
    }

    public String glyph() {
        return glyph;
    }

    public boolean isRed() {
        return this == HEARTS || this == DIAMONDS;
    }
}
