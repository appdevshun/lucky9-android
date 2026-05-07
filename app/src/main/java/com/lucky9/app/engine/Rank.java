package com.lucky9.app.engine;

public enum Rank {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(0, "10"),
    JACK(0, "J"),
    QUEEN(0, "Q"),
    KING(0, "K");

    private final int points;
    private final String label;

    Rank(int points, String label) {
        this.points = points;
        this.label = label;
    }

    public int points() {
        return points;
    }

    public String label() {
        return label;
    }
}
