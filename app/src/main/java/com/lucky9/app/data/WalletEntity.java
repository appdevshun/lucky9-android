package com.lucky9.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

@Entity(tableName = "wallet")
public class WalletEntity {
    @PrimaryKey
    public int id;

    @NonNull
    public BigDecimal balance;

    public long lastBonusAtMillis;

    public WalletEntity(int id, @NonNull BigDecimal balance, long lastBonusAtMillis) {
        this.id = id;
        this.balance = balance;
        this.lastBonusAtMillis = lastBonusAtMillis;
    }
}
