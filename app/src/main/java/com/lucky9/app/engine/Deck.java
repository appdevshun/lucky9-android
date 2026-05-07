package com.lucky9.app.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    private final List<Card> cards = new ArrayList<>(52);
    private final Random rng;

    public Deck() {
        this(new Random());
    }

    public Deck(Random rng) {
        this.rng = rng;
        reset();
    }

    public final void reset() {
        cards.clear();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                cards.add(new Card(r, s));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards, rng);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            reset();
        }
        return cards.remove(cards.size() - 1);
    }

    public int remaining() {
        return cards.size();
    }
}
