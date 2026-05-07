package com.lucky9.app.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Table tier definitions. Tier name = minimum wallet to enter.
 * Reserve = 90% of tier minimum.
 * Min bet = 3% of tier minimum.
 * Ante = 50% of min bet.
 * Lockout threshold = 20% of tier minimum.
 *
 * All values are stored as BigDecimal to preserve precision per spec.
 */
public enum Tier {
    T50(50),
    T100(100),
    T200(200),
    T500(500),
    T1000(1000);

    private final BigDecimal min;

    Tier(int minimum) {
        this.min = new BigDecimal(minimum);
    }

    public BigDecimal minimum() {
        return min;
    }

    public BigDecimal reserve() {
        return min.multiply(new BigDecimal("0.90"));
    }

    public BigDecimal minBet() {
        return min.multiply(new BigDecimal("0.03"));
    }

    public BigDecimal ante() {
        return minBet().multiply(new BigDecimal("0.50"));
    }

    public BigDecimal lockoutThreshold() {
        return min.multiply(new BigDecimal("0.20"));
    }

    public String displayName() {
        return "Tier " + min.toPlainString();
    }

    /**
     * Maximum bet a non-banker player may place this round, given the banker's wallet
     * and the count of non-banker players. Capacity is divided equally; leftover stays unused.
     */
    public BigDecimal maxBet(BigDecimal bankerWallet, int nonBankerPlayers) {
        if (nonBankerPlayers <= 0) return BigDecimal.ZERO;
        BigDecimal capacity = bankerWallet.subtract(reserve());
        if (capacity.signum() <= 0) return BigDecimal.ZERO;
        return capacity.divide(new BigDecimal(nonBankerPlayers), new MathContext(12, RoundingMode.DOWN));
    }

    /**
     * Banker eligibility: (wallet - reserve) / players >= min bet.
     * Also requires the wallet to be at least the tier minimum.
     */
    public boolean isBankerEligible(BigDecimal wallet, int nonBankerPlayers) {
        if (wallet.compareTo(min) < 0) return false;
        if (nonBankerPlayers <= 0) return true;
        BigDecimal perPlayer = maxBet(wallet, nonBankerPlayers);
        return perPlayer.compareTo(minBet()) >= 0;
    }
}
