package com.lucky9.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.App;
import com.lucky9.app.R;
import com.lucky9.app.engine.BotPolicy;
import com.lucky9.app.engine.Card;
import com.lucky9.app.engine.Engine;
import com.lucky9.app.engine.Outcome;
import com.lucky9.app.engine.PlayerState;
import com.lucky9.app.engine.RoundResult;
import com.lucky9.app.engine.Tier;
import com.lucky9.app.repo.WalletRepository;
import com.lucky9.app.ui.views.CardFaceView;
import com.lucky9.app.ui.views.OvalTableView;
import com.lucky9.app.ui.views.WinBarView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Bot match: oval table, banker rotation, full Lucky 9 round loop. */
public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    private static final long BET_PHASE_MS = 5000L;
    private static final long PRE_DEAL_MS = 2000L;
    private static final long DEAL_PER_CARD_MS = 250L;
    private static final long HIT_PHASE_MS = 5000L;
    private static final long REVEAL_PER_PLAYER_MS = 1000L;
    private static final long SETTLEMENT_MS = 2200L;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();
    private BotPolicy botPolicy;

    private OvalTableView ovalTable;
    private TextView tierLabel;
    private TextView roundLabel;
    private TextView jackpotLabel;
    private TextView statusLabel;
    private View betPanel;
    private SeekBar betSeek;
    private TextView betAmount;
    private MaterialButton btnBet;
    private View hitStandPanel;
    private MaterialButton btnHit;
    private MaterialButton btnStand;
    private ConstraintLayout root;
    private ImageButton btnBack;

    private final List<SeatViewHolder> seatViews = new ArrayList<>();
    private Engine engine;
    private Tier tier;
    private int humanSeat = 0;
    private BigDecimal lastBalanceSnapshot;

    private enum Phase {BET, DEAL, HIT, REVEAL, SETTLE}

    private Phase phase = Phase.BET;
    private boolean roundInProgress = false;
    private boolean activityFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ovalTable = findViewById(R.id.oval_table);
        tierLabel = findViewById(R.id.tier_label);
        roundLabel = findViewById(R.id.round_label);
        jackpotLabel = findViewById(R.id.jackpot_label);
        statusLabel = findViewById(R.id.status_label);
        betPanel = findViewById(R.id.bet_panel);
        betSeek = findViewById(R.id.bet_seek);
        betAmount = findViewById(R.id.bet_amount);
        btnBet = findViewById(R.id.btn_bet);
        hitStandPanel = findViewById(R.id.hit_stand_panel);
        btnHit = findViewById(R.id.btn_hit);
        btnStand = findViewById(R.id.btn_stand);
        // Seat panels are added as children of the ConstraintLayout that holds the oval table.
        root = (ConstraintLayout) ovalTable.getParent();
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        botPolicy = new BotPolicy(rng);

        Bundle extras = getIntent().getExtras();
        String tierName = extras != null ? extras.getString(TableSelectActivity.EXTRA_TIER, Tier.T100.name()) : Tier.T100.name();
        int seats = extras != null ? extras.getInt(TableSelectActivity.EXTRA_SEATS, 4) : 4;
        try {
            tier = Tier.valueOf(tierName);
        } catch (Exception e) {
            tier = Tier.T100;
        }
        tierLabel.setText(tier.displayName());

        bootstrapEngine(seats);
        ovalTable.post(this::layoutSeatViews);
        ui.postDelayed(this::startRound, 600);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityFinishing = true;
        ui.removeCallbacksAndMessages(null);
    }

    private void bootstrapEngine(int seatCount) {
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        BigDecimal humanBalance = repo.getSync().balance;
        lastBalanceSnapshot = humanBalance;

        List<PlayerState> seats = new ArrayList<>();
        // Human seat at 0 (we will visually anchor it to the bottom).
        seats.add(new PlayerState(0, getString(R.string.you), true, humanBalance));
        BigDecimal botBudget = tier.minimum().multiply(new BigDecimal("3"));
        for (int i = 1; i < seatCount; i++) {
            seats.add(new PlayerState(i, "Bot " + i, false, botBudget));
        }
        humanSeat = 0;
        engine = new Engine(tier, seats, rng);
        engine.initializeFirstBanker();
    }

    /** Place seat panels around the oval, anchoring the human at the bottom-center. */
    private void layoutSeatViews() {
        if (seatViews.isEmpty()) {
            for (PlayerState p : engine.seats()) {
                View v = getLayoutInflater().inflate(R.layout.view_player_seat, root, false);
                root.addView(v);
                SeatViewHolder h = new SeatViewHolder(v, p);
                seatViews.add(h);
            }
        }

        int n = engine.seats().size();
        float W = ovalTable.getWidth();
        float H = ovalTable.getHeight();
        float cx = ovalTable.getX() + W / 2f;
        float cy = ovalTable.getY() + H / 2f;
        float rx = W * 0.45f;
        float ry = H * 0.42f;

        for (int i = 0; i < n; i++) {
            // Distribute seats around the ellipse with the human at angle 90deg (bottom).
            double angle = Math.PI / 2 + (2 * Math.PI * i) / n;
            float x = cx + rx * (float) Math.cos(angle);
            float y = cy + ry * (float) Math.sin(angle);
            View v = seatViews.get(i).root;
            v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int vw = v.getMeasuredWidth();
            int vh = v.getMeasuredHeight();
            v.setX(x - vw / 2f);
            v.setY(y - vh / 2f);
        }
        refreshSeats();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !seatViews.isEmpty()) {
            layoutSeatViews();
        }
    }

    private void refreshSeats() {
        roundLabel.setText(getString(R.string.round) + " " + engine.roundNumber());
        jackpotLabel.setText(getString(R.string.jackpot) + " " + engine.jackpot().stripTrailingZeros().toPlainString());
        ovalTable.setJackpot(getString(R.string.jackpot) + " " + engine.jackpot().stripTrailingZeros().toPlainString());
        ovalTable.setRound(getString(R.string.round) + " " + engine.roundNumber());
        for (SeatViewHolder h : seatViews) h.bind(phase);
    }

    private void startRound() {
        if (activityFinishing) return;
        if (humanLockedOut()) {
            statusLabel.setText(R.string.lockout);
            return;
        }
        // Reset hand UI between rounds.
        for (SeatViewHolder h : seatViews) h.clearCards();
        roundInProgress = true;
        phase = Phase.BET;
        if (engine.roundNumber() > 0) {
            engine.rotateBanker();
        }
        refreshSeats();
        runBetPhase();
    }

    // --------- BET ----------
    private BigDecimal humanBet = null;

    private void runBetPhase() {
        if (humanIsBanker()) {
            statusLabel.setText("You are banker — others bet");
            betPanel.setVisibility(View.GONE);
        } else {
            BigDecimal min = tier.minBet();
            BigDecimal max = engine.maxBetForNonBanker();
            if (max.compareTo(min) < 0) max = min; // safeguard
            int range = max.subtract(min).multiply(new BigDecimal("100")).intValue();
            betSeek.setMax(Math.max(0, range));
            betSeek.setProgress(0);
            humanBet = min;
            BigDecimal finalMin = min;
            BigDecimal finalMax = max;
            betAmount.setText(min.stripTrailingZeros().toPlainString());
            betSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    BigDecimal amt = finalMin.add(new BigDecimal(progress).divide(new BigDecimal("100")));
                    if (amt.compareTo(finalMax) > 0) amt = finalMax;
                    humanBet = amt;
                    betAmount.setText(amt.stripTrailingZeros().toPlainString());
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            betPanel.setVisibility(View.VISIBLE);
            statusLabel.setText("Place your bet (auto " + finalMin.stripTrailingZeros().toPlainString() + " in 5s)");
            btnBet.setOnClickListener(v -> {
                betPanel.setVisibility(View.GONE);
                ui.removeCallbacksAndMessages(null);
                proceedToDeal();
            });
        }
        ui.postDelayed(() -> {
            if (phase != Phase.BET) return;
            betPanel.setVisibility(View.GONE);
            proceedToDeal();
        }, BET_PHASE_MS);
    }

    private void proceedToDeal() {
        phase = Phase.DEAL;
        statusLabel.setText("Dealing…");
        // Build bet collector.
        Engine.BetCollector collector = p -> {
            if (p.isHuman) {
                BigDecimal val = humanBet != null ? humanBet : tier.minBet();
                return val;
            }
            BigDecimal min = tier.minBet();
            BigDecimal max = engine.maxBetForNonBanker();
            if (max.compareTo(min) < 0) max = min;
            return botPolicy.pickBet(p, min, max);
        };
        engine.startRound(collector);

        // Save bet/ante transactions.
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            PlayerState me = engine.seats().get(humanSeat);
            BigDecimal newBal = me.wallet;
            BigDecimal delta = newBal.subtract(lastBalanceSnapshot);
            if (delta.signum() != 0) {
                repo.applyDeltaSync(delta, WalletRepository.TX_BET_PLACED,
                        "Round " + engine.roundNumber() + " ante+bet");
            }
            lastBalanceSnapshot = newBal;
        });

        refreshSeats();
        ui.postDelayed(this::revealNaturalsThenHits, PRE_DEAL_MS);
    }

    // --------- DEAL & NATURAL 9 reveal ----------
    private void revealNaturalsThenHits() {
        // Reveal Natural 9 hands immediately.
        boolean anyNatural = false;
        PlayerState banker = engine.banker();
        if (banker.hand.isNatural9()) {
            anyNatural = true;
        }
        for (SeatViewHolder h : seatViews) {
            boolean reveal = (h.player == banker && banker.hand.isNatural9()) ||
                    (!h.player.isBanker && h.player.hand.isNatural9());
            h.showCards(reveal);
            if (reveal && h.player.hand.isNatural9()) {
                h.flashNatural9();
                anyNatural = true;
            }
        }
        if (banker.hand.isNatural9()) {
            // Skip hit phase entirely.
            statusLabel.setText("Banker Natural 9!");
            ui.postDelayed(this::doRevealAndSettle, 1200);
            return;
        }
        // Auto-stand any player with Natural 9.
        for (PlayerState p : engine.seats()) {
            if (!p.isBanker && p.hand.isNatural9()) p.stood = true;
        }
        phase = Phase.HIT;
        runHitsForAllNonBankers();
    }

    private final List<Integer> hitQueue = new ArrayList<>();

    private void runHitsForAllNonBankers() {
        hitQueue.clear();
        int n = engine.seats().size();
        int start = (engine.bankerSeat() + 1) % n;
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            PlayerState p = engine.seats().get(idx);
            if (p.isBanker || p.lockedOut) continue;
            if (p.hand.isNatural9() || p.stood) continue;
            hitQueue.add(idx);
        }
        nextHitTurn();
    }

    private void nextHitTurn() {
        if (hitQueue.isEmpty()) {
            playBankerTurnThenSettle();
            return;
        }
        int idx = hitQueue.remove(0);
        PlayerState p = engine.seats().get(idx);
        for (SeatViewHolder h : seatViews) h.setActive(h.player == p);
        if (p.isHuman) {
            promptHumanHit(p);
        } else {
            statusLabel.setText(p.name + " thinking…");
            ui.postDelayed(() -> {
                if (botPolicy.shouldHit(p)) {
                    Card drawn = engine.hit(p);
                    seatForPlayer(p).addCard(drawn, true);
                    Log.d(TAG, p.name + " hits " + drawn);
                } else {
                    engine.stand(p);
                }
                ui.postDelayed(this::nextHitTurn, 500);
            }, 700);
        }
    }

    private void promptHumanHit(PlayerState p) {
        statusLabel.setText("Your turn — HIT or STAND (5s)");
        hitStandPanel.setVisibility(View.VISIBLE);
        btnHit.setEnabled(true);
        btnHit.setOnClickListener(v -> {
            hitStandPanel.setVisibility(View.GONE);
            ui.removeCallbacksAndMessages(null);
            Card c = engine.hit(p);
            seatForPlayer(p).addCard(c, true);
            ui.postDelayed(this::nextHitTurn, 600);
        });
        btnStand.setOnClickListener(v -> {
            hitStandPanel.setVisibility(View.GONE);
            ui.removeCallbacksAndMessages(null);
            engine.stand(p);
            ui.postDelayed(this::nextHitTurn, 200);
        });
        ui.postDelayed(() -> {
            if (phase != Phase.HIT) return;
            hitStandPanel.setVisibility(View.GONE);
            engine.stand(p);
            statusLabel.setText("Auto-stand");
            ui.postDelayed(this::nextHitTurn, 300);
        }, HIT_PHASE_MS);
    }

    private void playBankerTurnThenSettle() {
        for (SeatViewHolder h : seatViews) h.setActive(false);
        PlayerState banker = engine.banker();
        if (!banker.hand.isNatural9()) {
            // Banker plays: bot only (if human is banker, prompt them).
            if (banker.isHuman) {
                hitStandPanel.setVisibility(View.VISIBLE);
                statusLabel.setText("You are banker — HIT or STAND");
                btnHit.setEnabled(banker.hand.size() < 3);
                btnHit.setOnClickListener(v -> {
                    hitStandPanel.setVisibility(View.GONE);
                    Card c = engine.hit(banker);
                    seatForPlayer(banker).addCard(c, true);
                    ui.postDelayed(this::doRevealAndSettle, 600);
                });
                btnStand.setOnClickListener(v -> {
                    hitStandPanel.setVisibility(View.GONE);
                    engine.stand(banker);
                    ui.postDelayed(this::doRevealAndSettle, 200);
                });
                ui.postDelayed(() -> {
                    if (phase == Phase.HIT) {
                        hitStandPanel.setVisibility(View.GONE);
                        engine.stand(banker);
                        ui.postDelayed(this::doRevealAndSettle, 200);
                    }
                }, HIT_PHASE_MS);
                return;
            }
            statusLabel.setText("Banker thinking…");
            ui.postDelayed(() -> {
                if (botPolicy.shouldHit(banker)) {
                    Card drawn = engine.hit(banker);
                    seatForPlayer(banker).addCard(drawn, true);
                } else {
                    engine.stand(banker);
                }
                ui.postDelayed(this::doRevealAndSettle, 600);
            }, 800);
        } else {
            doRevealAndSettle();
        }
    }

    // --------- REVEAL + SETTLEMENT ----------
    private void doRevealAndSettle() {
        phase = Phase.REVEAL;
        statusLabel.setText("Reveal…");
        // Reveal in seat order, banker last.
        int bankerSeat = engine.bankerSeat();
        List<Integer> order = new ArrayList<>();
        int n = engine.seats().size();
        for (int i = 0; i < n; i++) {
            int idx = (bankerSeat + 1 + i) % n;
            if (idx != bankerSeat) order.add(idx);
        }
        order.add(bankerSeat);

        long delay = 0;
        for (int idx : order) {
            final int finalIdx = idx;
            ui.postDelayed(() -> {
                SeatViewHolder h = seatViews.get(finalIdx);
                h.showCards(true);
                h.showTotal();
            }, delay);
            delay += REVEAL_PER_PLAYER_MS;
        }
        ui.postDelayed(this::settleAndUpdateWallet, delay + 200);
    }

    private void settleAndUpdateWallet() {
        phase = Phase.SETTLE;
        RoundResult result = engine.settle();
        for (SeatViewHolder h : seatViews) h.showOutcome();
        refreshSeats();

        // Sync the human balance with the persistent wallet.
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        PlayerState me = engine.seats().get(humanSeat);
        BigDecimal newBal = me.wallet;
        BigDecimal delta = newBal.subtract(lastBalanceSnapshot);
        repo.io().execute(() -> {
            if (delta.signum() != 0) {
                repo.applyDeltaSync(delta, WalletRepository.TX_BET_RESOLVED,
                        "Round " + result.roundNumber + " settlement");
            }
            if (!result.jackpotWinnersSeatIndices.isEmpty()
                    && result.jackpotWinnersSeatIndices.contains(humanSeat)) {
                runOnUiThread(() ->
                        Toast.makeText(this, "JACKPOT! +" + result.jackpotPayoutPerWinner.stripTrailingZeros().toPlainString(),
                                Toast.LENGTH_LONG).show());
            }
        });
        lastBalanceSnapshot = newBal;

        ui.postDelayed(() -> {
            roundInProgress = false;
            startRound();
        }, SETTLEMENT_MS);
    }

    private SeatViewHolder seatForPlayer(PlayerState p) {
        for (SeatViewHolder h : seatViews) if (h.player == p) return h;
        return seatViews.get(0);
    }

    private boolean humanIsBanker() {
        return engine.banker().isHuman;
    }

    private boolean humanLockedOut() {
        return engine.seats().get(humanSeat).lockedOut;
    }

    // ----- Inner: per-seat view binding -----
    private class SeatViewHolder {
        final View root;
        final PlayerState player;
        final ImageView crown;
        final FrameLayout avatarHolder;
        final TextView avatarInitials;
        final TextView nameLabel;
        final TextView balanceLabel;
        final TextView betLabel;
        final WinBarView winbar;
        final LinearLayout cardsRow;
        final TextView totalLabel;
        final TextView resultLabel;

        SeatViewHolder(View v, PlayerState p) {
            this.root = v;
            this.player = p;
            crown = v.findViewById(R.id.crown);
            avatarHolder = v.findViewById(R.id.avatar_holder);
            avatarInitials = v.findViewById(R.id.avatar_initials);
            nameLabel = v.findViewById(R.id.name_label);
            balanceLabel = v.findViewById(R.id.balance_label);
            betLabel = v.findViewById(R.id.bet_label);
            winbar = v.findViewById(R.id.winbar);
            cardsRow = v.findViewById(R.id.cards_row);
            totalLabel = v.findViewById(R.id.total_label);
            resultLabel = v.findViewById(R.id.result_label);
            avatarInitials.setText(initialsFor(p.name));
            nameLabel.setText(p.name);
        }

        void bind(Phase phase) {
            balanceLabel.setText(player.wallet.stripTrailingZeros().toPlainString());
            crown.setVisibility(player.isBanker ? View.VISIBLE : View.INVISIBLE);
            winbar.setFilled(player.winBars);
            if (player.currentBet.signum() > 0) {
                betLabel.setText("Bet " + player.currentBet.stripTrailingZeros().toPlainString());
                betLabel.setVisibility(View.VISIBLE);
            } else {
                betLabel.setVisibility(View.INVISIBLE);
            }
            if (phase != Phase.SETTLE && phase != Phase.REVEAL) {
                resultLabel.setText("");
            }
            avatarHolder.setBackgroundResource(R.drawable.bg_avatar);
        }

        void clearCards() {
            cardsRow.removeAllViews();
            totalLabel.setText("");
            resultLabel.setText("");
        }

        void addCard(Card c, boolean faceUp) {
            CardFaceView cv = new CardFaceView(GameActivity.this);
            int w = (int) (getResources().getDimension(R.dimen.card_w));
            int h = (int) (getResources().getDimension(R.dimen.card_h));
            ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams((int)(w * 0.8), (int)(h * 0.8));
            cv.setLayoutParams(lp);
            cv.setCard(c, !faceUp);
            cardsRow.addView(cv);
        }

        void showCards(boolean show) {
            // Initial display: re-render from the engine's hand each time so that
            // newly-dealt cards are reflected (face-down by default).
            cardsRow.removeAllViews();
            for (Card c : player.hand.cards()) {
                CardFaceView cv = new CardFaceView(GameActivity.this);
                int w = (int) (getResources().getDimension(R.dimen.card_w));
                int h = (int) (getResources().getDimension(R.dimen.card_h));
                cv.setLayoutParams(new LinearLayout.LayoutParams((int)(w * 0.8), (int)(h * 0.8)));
                cv.setCard(c, !show);
                cardsRow.addView(cv);
            }
        }

        void setActive(boolean active) {
            avatarHolder.setBackgroundResource(active ? R.drawable.bg_avatar_active : R.drawable.bg_avatar);
        }

        void showTotal() {
            int t = player.hand.total();
            totalLabel.setText(player.hand.isNatural9() ? "N9!" : String.valueOf(t));
        }

        void flashNatural9() {
            totalLabel.setText("N9!");
        }

        void showOutcome() {
            if (player.lastOutcome == null) {
                resultLabel.setText("");
                return;
            }
            switch (player.lastOutcome) {
                case PLAYER_WIN:
                case PLAYER_NATURAL9_WIN:
                    resultLabel.setText("WIN +" + player.lastDelta.stripTrailingZeros().toPlainString());
                    resultLabel.setTextColor(0xFF22C55E);
                    break;
                case BANKER_WIN:
                    resultLabel.setText(player.isBanker ? "WIN" : ("LOSE " + player.lastDelta.stripTrailingZeros().toPlainString()));
                    resultLabel.setTextColor(player.isBanker ? 0xFF22C55E : 0xFFEF4444);
                    break;
                case TIE:
                default:
                    resultLabel.setText("TIE");
                    resultLabel.setTextColor(0xFFF59E0B);
                    break;
            }
        }
    }

    private static String initialsFor(@NonNull String name) {
        if (name == null || name.isEmpty()) return "?";
        if (name.length() <= 2) return name.toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
