package com.lucky9.app.engine;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Lightweight bot decision policy. Hits on totals 0–4, stands on 5+, and
 * always stands on Natural 9 (forced by engine anyway). For betting it picks
 * a value between min and a fraction of max with mild jitter.
 */
public class BotPolicy {

    private final Random rng;

    public BotPolicy(Random rng) {
        this.rng = rng;
    }

    public BigDecimal pickBet(PlayerState p, BigDecimal min, BigDecimal max) {
        if (max.compareTo(min) <= 0) return min;
        BigDecimal range = max.subtract(min);
        double f = 0.25 + rng.nextDouble() * 0.5; // 25–75% of available range
        BigDecimal pick = min.add(range.multiply(BigDecimal.valueOf(f)));
        return pick.max(min).min(max);
    }

    public boolean shouldHit(PlayerState p) {
        if (p.hand.isNatural9()) return false;
        int total = p.hand.total();
        if (p.hand.size() >= 3) return false;
        // Filipino-style heuristic: 0–4 hit, 5 mixed, 6+ stand.
        if (total <= 4) return true;
        if (total == 5) return rng.nextDouble() < 0.4;
        return false;
    }
}
