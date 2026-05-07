package com.lucky9.app.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure, deterministic Lucky 9 game engine. Holds round state for a single
 * table tier and rotates the banker clockwise. Caller is responsible for
 * gathering bet amounts and hit/stand decisions and feeding them in.
 */
public class Engine {

    public static final BigDecimal NATURAL9_PAYOUT = new BigDecimal("1.5");

    private final Tier tier;
    private final List<PlayerState> seats;
    private final Deck deck;
    private final Random rng;
    private int currentBankerSeat;
    private int roundNumber = 0;
    private BigDecimal jackpot = BigDecimal.ZERO;

    public Engine(Tier tier, List<PlayerState> seats, Random rng) {
        if (seats.size() < 2) throw new IllegalArgumentException("need at least 2 seats");
        this.tier = tier;
        this.seats = seats;
        this.rng = rng;
        this.deck = new Deck(rng);
        this.currentBankerSeat = 0;
    }

    public Tier tier() { return tier; }
    public List<PlayerState> seats() { return seats; }
    public int roundNumber() { return roundNumber; }
    public BigDecimal jackpot() { return jackpot; }
    public int bankerSeat() { return currentBankerSeat; }

    public PlayerState banker() { return seats.get(currentBankerSeat); }

    public List<PlayerState> nonBankers() {
        List<PlayerState> out = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            if (i != currentBankerSeat) out.add(seats.get(i));
        }
        return out;
    }

    /** Advance banker clockwise to the next eligible seat. */
    public void rotateBanker() {
        int n = seats.size();
        for (int step = 1; step <= n; step++) {
            int candidate = (currentBankerSeat + step) % n;
            PlayerState p = seats.get(candidate);
            int nonBankerCount = n - 1;
            if (!p.lockedOut && tier.isBankerEligible(p.wallet, nonBankerCount)) {
                currentBankerSeat = candidate;
                refreshBankerFlag();
                return;
            }
        }
        // No eligible banker found — keep current.
        refreshBankerFlag();
    }

    private void refreshBankerFlag() {
        for (int i = 0; i < seats.size(); i++) {
            seats.get(i).isBanker = (i == currentBankerSeat);
        }
    }

    /** Test/multiplayer hook: set the banker seat directly. */
    public void setBankerSeat(int seat) {
        if (seat < 0 || seat >= seats.size()) {
            throw new IllegalArgumentException("seat out of range");
        }
        currentBankerSeat = seat;
        refreshBankerFlag();
    }

    public void initializeFirstBanker() {
        // First eligible seat starting at 0.
        int n = seats.size();
        for (int i = 0; i < n; i++) {
            PlayerState p = seats.get(i);
            if (!p.lockedOut && tier.isBankerEligible(p.wallet, n - 1)) {
                currentBankerSeat = i;
                refreshBankerFlag();
                return;
            }
        }
        currentBankerSeat = 0;
        refreshBankerFlag();
    }

    public BigDecimal maxBetForNonBanker() {
        return tier.maxBet(banker().wallet, seats.size() - 1);
    }

    /** Cap a desired bet within [minBet, maxBet] and the no-all-in safeguard. */
    public BigDecimal clampBet(PlayerState p, BigDecimal desired) {
        BigDecimal min = tier.minBet();
        BigDecimal max = maxBetForNonBanker();
        BigDecimal noAllIn = p.wallet.subtract(tier.minimum());
        if (noAllIn.signum() < 0) noAllIn = BigDecimal.ZERO;
        BigDecimal cap = max.min(noAllIn.compareTo(BigDecimal.ZERO) > 0 ? noAllIn : max);
        if (cap.compareTo(min) < 0) {
            // Can't afford min cleanly — allow min if wallet can cover it after potential loss.
            if (p.wallet.compareTo(min) >= 0) return min;
            return BigDecimal.ZERO;
        }
        BigDecimal bet = desired;
        if (bet.compareTo(min) < 0) bet = min;
        if (bet.compareTo(cap) > 0) bet = cap;
        return bet;
    }

    /** Begin a new round: reset hands, charge antes, deal two cards each. */
    public void startRound(BetCollector bets) {
        roundNumber++;
        deck.reset();
        for (PlayerState p : seats) p.resetForRound();

        // Update lockout: wallet < 20% tier minimum.
        for (PlayerState p : seats) {
            p.lockedOut = p.wallet.compareTo(tier.lockoutThreshold()) < 0;
        }

        // Antes: every non-banker pays ante into the jackpot.
        BigDecimal ante = tier.ante();
        for (PlayerState p : seats) {
            if (p.isBanker || p.lockedOut) continue;
            if (p.wallet.compareTo(ante) >= 0) {
                p.wallet = p.wallet.subtract(ante);
                jackpot = jackpot.add(ante);
            }
        }

        // Bets (only non-bankers). The banker covers all of them up to capacity.
        for (PlayerState p : seats) {
            if (p.isBanker || p.lockedOut) {
                p.currentBet = BigDecimal.ZERO;
                continue;
            }
            BigDecimal raw = bets.betFor(p);
            if (raw == null) raw = tier.minBet();
            p.currentBet = clampBet(p, raw);
        }

        // Deal two cards face-down to all (banker + non-bankers + locked-out skip dealing).
        List<PlayerState> dealOrder = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            int idx = (currentBankerSeat + 1 + i) % seats.size();
            PlayerState p = seats.get(idx);
            if (p.lockedOut && !p.isBanker) continue;
            dealOrder.add(p);
        }
        for (int round = 0; round < 2; round++) {
            for (PlayerState p : dealOrder) {
                p.hand.add(deck.draw());
            }
        }
    }

    /** Player asks for one extra card. */
    public Card hit(PlayerState p) {
        if (p.stood) throw new IllegalStateException("already stood");
        if (p.hand.size() >= 3) throw new IllegalStateException("max 3 cards");
        Card c = deck.draw();
        p.hand.add(c);
        p.stood = true; // only one optional hit per round
        return c;
    }

    public void stand(PlayerState p) {
        p.stood = true;
    }

    /**
     * Settle the round: compare every non-banker hand to the banker's, apply
     * 1:1 / 1.5:1 / loss / tie payouts, increment win bars, and pay jackpot if
     * any seat has reached 5 bars. Returns a snapshot for the UI.
     */
    public RoundResult settle() {
        PlayerState banker = banker();
        boolean bankerN9 = banker.hand.isNatural9();
        int bankerTotal = banker.hand.total();

        for (PlayerState p : seats) {
            if (p.isBanker || p.lockedOut) continue;
            boolean playerN9 = p.hand.isNatural9();
            int pTotal = p.hand.total();

            Outcome out;
            if (playerN9 && bankerN9) {
                out = Outcome.TIE;
            } else if (playerN9) {
                out = Outcome.PLAYER_NATURAL9_WIN;
            } else if (bankerN9) {
                out = Outcome.BANKER_WIN;
            } else if (pTotal == bankerTotal) {
                out = Outcome.TIE;
            } else if (pTotal > bankerTotal) {
                out = Outcome.PLAYER_WIN;
            } else {
                out = Outcome.BANKER_WIN;
            }

            BigDecimal delta;
            switch (out) {
                case PLAYER_WIN:
                    delta = p.currentBet;
                    p.wallet = p.wallet.add(delta);
                    banker.wallet = banker.wallet.subtract(delta);
                    p.winBars++;
                    break;
                case PLAYER_NATURAL9_WIN:
                    delta = p.currentBet.multiply(NATURAL9_PAYOUT)
                            .setScale(6, RoundingMode.DOWN).stripTrailingZeros();
                    p.wallet = p.wallet.add(delta);
                    banker.wallet = banker.wallet.subtract(delta);
                    p.winBars++;
                    break;
                case BANKER_WIN:
                    delta = p.currentBet.negate();
                    p.wallet = p.wallet.add(delta); // subtract bet
                    banker.wallet = banker.wallet.subtract(delta); // gain bet
                    banker.winBars++;
                    break;
                case TIE:
                default:
                    delta = BigDecimal.ZERO;
                    p.winBars++;
                    banker.winBars++;
                    break;
            }
            p.lastOutcome = out;
            p.lastDelta = delta;
        }

        // Jackpot: any seats at >= 5 bars share equally.
        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).winBars >= 5) winners.add(i);
        }
        BigDecimal share = BigDecimal.ZERO;
        if (!winners.isEmpty() && jackpot.signum() > 0) {
            share = jackpot.divide(new BigDecimal(winners.size()),
                    new MathContext(12, RoundingMode.DOWN));
            for (int idx : winners) {
                seats.get(idx).wallet = seats.get(idx).wallet.add(share);
            }
            jackpot = BigDecimal.ZERO;
            for (PlayerState s : seats) s.winBars = 0;
        }

        return new RoundResult(roundNumber, currentBankerSeat, seats, jackpot, winners, share);
    }

    public interface BetCollector {
        BigDecimal betFor(PlayerState p);
    }
}
