package com.lucky9.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMillis;

    @NonNull
    public String type;

    @NonNull
    public BigDecimal amount;

    @NonNull
    public BigDecimal balanceAfter;

    public String note;

    public TransactionEntity(long timestampMillis,
                             @NonNull String type,
                             @NonNull BigDecimal amount,
                             @NonNull BigDecimal balanceAfter,
                             String note) {
        this.timestampMillis = timestampMillis;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
    }
}
