package com.lucky9.app.engine;

import java.math.BigDecimal;

/**
 * Per-round, per-seat snapshot of a participant.
 * Wallets are tracked here independent of the persistent Room wallet so the
 * engine remains pure and unit-testable.
 */
public class PlayerState {
    public final int seatIndex;
    public final String name;
    public final boolean isHuman;
    public BigDecimal wallet;
    public final Hand hand = new Hand();
    public BigDecimal currentBet = BigDecimal.ZERO;
    public int winBars = 0;
    public boolean stood = false;
    public boolean isBanker = false;
    public boolean lockedOut = false;
    public Outcome lastOutcome = null;
    public BigDecimal lastDelta = BigDecimal.ZERO;

    public PlayerState(int seatIndex, String name, boolean isHuman, BigDecimal wallet) {
        this.seatIndex = seatIndex;
        this.name = name;
        this.isHuman = isHuman;
        this.wallet = wallet;
    }

    public void resetForRound() {
        hand.clear();
        currentBet = BigDecimal.ZERO;
        stood = false;
        lastOutcome = null;
        lastDelta = BigDecimal.ZERO;
    }
}
