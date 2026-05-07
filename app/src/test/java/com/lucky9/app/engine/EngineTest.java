package com.lucky9.app.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EngineTest {

    @Test
    public void cardPoints_facesAreZero_acesAreOne() {
        assertEquals(0, new Card(Rank.TEN, Suit.HEARTS).points());
        assertEquals(0, new Card(Rank.JACK, Suit.HEARTS).points());
        assertEquals(0, new Card(Rank.QUEEN, Suit.HEARTS).points());
        assertEquals(0, new Card(Rank.KING, Suit.HEARTS).points());
        assertEquals(1, new Card(Rank.ACE, Suit.HEARTS).points());
        for (int v = 2; v <= 9; v++) {
            assertEquals(v, new Card(Rank.values()[v - 1], Suit.HEARTS).points());
        }
    }

    @Test
    public void hand_total_isModulo10() {
        Hand h = new Hand();
        h.add(new Card(Rank.SEVEN, Suit.HEARTS));
        h.add(new Card(Rank.EIGHT, Suit.SPADES));
        // 7+8 = 15 -> 5
        assertEquals(5, h.total());
        h.add(new Card(Rank.KING, Suit.SPADES));
        // 7+8+0 = 15 -> 5
        assertEquals(5, h.total());
    }

    @Test
    public void naturalNine_requiresExactlyTwoCards() {
        Hand h = new Hand();
        h.add(new Card(Rank.FOUR, Suit.HEARTS));
        h.add(new Card(Rank.FIVE, Suit.SPADES));
        assertTrue(h.isNatural9());
        h.add(new Card(Rank.TEN, Suit.SPADES));
        assertFalse("third card disqualifies natural", h.isNatural9());
    }

    @Test
    public void naturalEight_isNotSpecial() {
        Hand h = new Hand();
        h.add(new Card(Rank.THREE, Suit.HEARTS));
        h.add(new Card(Rank.FIVE, Suit.SPADES));
        assertEquals(8, h.total());
        assertFalse("Natural 8 has no special status per spec", h.isNatural9());
    }

    @Test
    public void tier_formulas_match_spec() {
        Tier t100 = Tier.T100;
        assertEquals(0, new BigDecimal("90").compareTo(t100.reserve()));
        assertEquals(0, new BigDecimal("3.00").compareTo(t100.minBet()));
        assertEquals(0, new BigDecimal("1.500").compareTo(t100.ante()));
    }

    @Test
    public void bankerEligibility_failsWhenCapacityTooLow() {
        Tier t100 = Tier.T100;
        // (50 - 90) / 3 = negative -> ineligible (also wallet < min)
        assertFalse(t100.isBankerEligible(new BigDecimal("50"), 3));
        // (300 - 90)/3 = 70 >= 3 -> eligible
        assertTrue(t100.isBankerEligible(new BigDecimal("300"), 3));
    }

    @Test
    public void engine_settle_player_natural9_wins_oneAndAHalf() {
        // Build a deterministic 2-seat engine: player + bot (banker).
        Random rng = new Random(1L);
        List<PlayerState> seats = new ArrayList<>();
        seats.add(new PlayerState(0, "P", true, new BigDecimal("100")));
        seats.add(new PlayerState(1, "B", false, new BigDecimal("500")));
        Engine engine = new Engine(Tier.T100, seats, rng);
        engine.setBankerSeat(1);

        // Hand-rig the hands rather than relying on shuffle.
        seats.get(0).hand.clear();
        seats.get(1).hand.clear();
        seats.get(0).hand.add(new Card(Rank.FOUR, Suit.HEARTS));
        seats.get(0).hand.add(new Card(Rank.FIVE, Suit.SPADES));
        seats.get(1).hand.add(new Card(Rank.TEN, Suit.HEARTS));
        seats.get(1).hand.add(new Card(Rank.SEVEN, Suit.SPADES));
        seats.get(0).currentBet = new BigDecimal("10");

        BigDecimal pBefore = seats.get(0).wallet;
        BigDecimal bBefore = seats.get(1).wallet;
        engine.settle();

        // Player N9 vs banker total 7 -> player wins 1.5x.
        BigDecimal expectedDelta = new BigDecimal("15.000000").stripTrailingZeros();
        assertEquals(expectedDelta, seats.get(0).wallet.subtract(pBefore).stripTrailingZeros());
        assertEquals(expectedDelta.negate(), seats.get(1).wallet.subtract(bBefore).stripTrailingZeros());
        assertEquals(1, seats.get(0).winBars);
        assertEquals(0, seats.get(1).winBars);
    }

    @Test
    public void engine_tie_givesBothWinBars_noPayment() {
        Random rng = new Random(7L);
        List<PlayerState> seats = new ArrayList<>();
        seats.add(new PlayerState(0, "P", true, new BigDecimal("100")));
        seats.add(new PlayerState(1, "B", false, new BigDecimal("500")));
        Engine engine = new Engine(Tier.T100, seats, rng);
        engine.setBankerSeat(1);

        seats.get(0).hand.clear();
        seats.get(1).hand.clear();
        // both totals 7
        seats.get(0).hand.add(new Card(Rank.THREE, Suit.HEARTS));
        seats.get(0).hand.add(new Card(Rank.FOUR, Suit.SPADES));
        seats.get(1).hand.add(new Card(Rank.TWO, Suit.HEARTS));
        seats.get(1).hand.add(new Card(Rank.FIVE, Suit.SPADES));
        seats.get(0).currentBet = new BigDecimal("10");

        BigDecimal pBefore = seats.get(0).wallet;
        BigDecimal bBefore = seats.get(1).wallet;
        engine.settle();
        assertEquals(0, seats.get(0).wallet.compareTo(pBefore));
        assertEquals(0, seats.get(1).wallet.compareTo(bBefore));
        assertEquals(1, seats.get(0).winBars);
        assertEquals(1, seats.get(1).winBars);
    }

    @Test
    public void engine_bankerNatural9_winsAllExceptN9Ties() {
        Random rng = new Random(3L);
        List<PlayerState> seats = new ArrayList<>();
        seats.add(new PlayerState(0, "P1", true, new BigDecimal("100")));   // total 6
        seats.add(new PlayerState(1, "P2", false, new BigDecimal("100")));  // natural 9
        seats.add(new PlayerState(2, "B",  false, new BigDecimal("500")));  // banker N9
        Engine engine = new Engine(Tier.T100, seats, rng);
        engine.setBankerSeat(2);

        seats.get(0).hand.clear();
        seats.get(1).hand.clear();
        seats.get(2).hand.clear();
        seats.get(0).hand.add(new Card(Rank.TWO, Suit.HEARTS));
        seats.get(0).hand.add(new Card(Rank.FOUR, Suit.SPADES));
        seats.get(1).hand.add(new Card(Rank.FOUR, Suit.HEARTS));
        seats.get(1).hand.add(new Card(Rank.FIVE, Suit.SPADES));
        seats.get(2).hand.add(new Card(Rank.FOUR, Suit.SPADES));
        seats.get(2).hand.add(new Card(Rank.FIVE, Suit.HEARTS));

        seats.get(0).currentBet = new BigDecimal("10");
        seats.get(1).currentBet = new BigDecimal("10");

        engine.settle();
        // P1 loses, P2 ties Natural 9.
        assertEquals(0, seats.get(0).winBars);
        assertEquals(1, seats.get(1).winBars);
        assertEquals(2, seats.get(2).winBars); // beat P1 (1 bar) + tie P2 (1 bar)
    }

    @Test
    public void deck_drawsAll52ThenRefills() {
        Deck d = new Deck(new Random(0L));
        for (int i = 0; i < 52; i++) d.draw();
        assertEquals(0, d.remaining());
        // After exhaustion, draw resets.
        Card next = d.draw();
        assertTrue(next != null);
        assertEquals(51, d.remaining());
    }
}
