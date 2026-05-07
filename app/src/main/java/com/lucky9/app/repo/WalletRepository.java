package com.lucky9.app.repo;

import androidx.lifecycle.LiveData;

import com.lucky9.app.data.AppDatabase;
import com.lucky9.app.data.TransactionEntity;
import com.lucky9.app.data.WalletEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Synchronous-by-default wallet repository. Activities should run mutations
 * on a background thread (helpers provided) but reads of the cached balance
 * are served from {@link WalletEntity}.
 */
public class WalletRepository {

    public static final String TX_DEPOSIT = "DEPOSIT";
    public static final String TX_WITHDRAW = "WITHDRAW";
    public static final String TX_BET_PLACED = "BET_ANTE";
    public static final String TX_BET_RESOLVED = "ROUND_RESULT";
    public static final String TX_BONUS = "DAILY_BONUS";
    public static final String TX_JACKPOT = "JACKPOT";

    public static final BigDecimal STARTING_BALANCE = new BigDecimal("1000");
    public static final BigDecimal DEFAULT_SIM_DEPOSIT = new BigDecimal("500");
    public static final BigDecimal DEFAULT_SIM_WITHDRAW = new BigDecimal("100");
    public static final BigDecimal DAILY_BONUS = new BigDecimal("250");
    public static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    private final AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public WalletRepository(AppDatabase db) {
        this.db = db;
    }

    public ExecutorService io() {
        return io;
    }

    public void ensureSeed() {
        io.execute(() -> {
            WalletEntity existing = db.walletDao().getSync();
            if (existing == null) {
                db.walletDao().insertIfMissing(new WalletEntity(1, STARTING_BALANCE, 0L));
            }
        });
    }

    public WalletEntity getSync() {
        WalletEntity w = db.walletDao().getSync();
        if (w == null) {
            w = new WalletEntity(1, STARTING_BALANCE, 0L);
            db.walletDao().insertIfMissing(w);
        }
        return w;
    }

    public LiveData<List<TransactionEntity>> transactions() {
        return db.transactionDao().recent();
    }

    /** Apply a delta synchronously, write a transaction row. Returns the new balance. */
    public BigDecimal applyDeltaSync(BigDecimal delta, String type, String note) {
        WalletEntity w = getSync();
        BigDecimal newBal = w.balance.add(delta);
        if (newBal.signum() < 0) newBal = BigDecimal.ZERO;
        w.balance = newBal;
        db.walletDao().update(w);
        db.transactionDao().insert(new TransactionEntity(
                System.currentTimeMillis(), type, delta, newBal, note));
        return newBal;
    }

    public boolean canClaimDailyBonus() {
        WalletEntity w = getSync();
        return System.currentTimeMillis() - w.lastBonusAtMillis >= ONE_DAY_MS;
    }

    public BigDecimal claimDailyBonusSync() {
        WalletEntity w = getSync();
        long now = System.currentTimeMillis();
        if (now - w.lastBonusAtMillis < ONE_DAY_MS) {
            return null;
        }
        w.lastBonusAtMillis = now;
        w.balance = w.balance.add(DAILY_BONUS);
        db.walletDao().update(w);
        db.transactionDao().insert(new TransactionEntity(
                now, TX_BONUS, DAILY_BONUS, w.balance, "Daily bonus"));
        return w.balance;
    }
}
