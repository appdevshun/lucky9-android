package com.lucky9.app.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    private final List<Card> cards = new ArrayList<>(3);

    public void add(Card c) {
        if (cards.size() >= 3) {
            throw new IllegalStateException("Lucky 9 hand is capped at 3 cards");
        }
        cards.add(c);
    }

    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    public int total() {
        int sum = 0;
        for (Card c : cards) {
            sum += c.points();
        }
        return sum % 10;
    }

    /** Natural 9 = exactly first two cards summing to 9. Natural 8 has no special status. */
    public boolean isNatural9() {
        if (cards.size() != 2) return false;
        return (cards.get(0).points() + cards.get(1).points()) % 10 == 9;
    }

    public void clear() {
        cards.clear();
    }
}
