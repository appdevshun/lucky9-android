package com.lucky9.app.engine;

import java.util.Objects;

public final class Card {
    private final Rank rank;
    private final Suit suit;

    public Card(Rank rank, Suit suit) {
        this.rank = Objects.requireNonNull(rank);
        this.suit = Objects.requireNonNull(suit);
    }

    public Rank rank() {
        return rank;
    }

    public Suit suit() {
        return suit;
    }

    public int points() {
        return rank.points();
    }

    @Override
    public String toString() {
        return rank.label() + suit.glyph();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card c = (Card) o;
        return rank == c.rank && suit == c.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }
}
