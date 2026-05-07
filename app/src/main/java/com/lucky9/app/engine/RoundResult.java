package com.lucky9.app.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RoundResult {
    public final int roundNumber;
    public final int bankerSeat;
    public final List<PlayerState> seats;
    public final BigDecimal jackpotAfter;
    public final List<Integer> jackpotWinnersSeatIndices;
    public final BigDecimal jackpotPayoutPerWinner;

    public RoundResult(int roundNumber, int bankerSeat, List<PlayerState> seats,
                       BigDecimal jackpotAfter, List<Integer> jackpotWinners,
                       BigDecimal jackpotPayoutPerWinner) {
        this.roundNumber = roundNumber;
        this.bankerSeat = bankerSeat;
        this.seats = new ArrayList<>(seats);
        this.jackpotAfter = jackpotAfter;
        this.jackpotWinnersSeatIndices = new ArrayList<>(jackpotWinners);
        this.jackpotPayoutPerWinner = jackpotPayoutPerWinner;
    }
}
