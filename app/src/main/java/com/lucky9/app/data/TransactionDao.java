package com.lucky9.app.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(TransactionEntity tx);

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC LIMIT 200")
    LiveData<List<TransactionEntity>> recent();

    @Query("SELECT COUNT(*) FROM transactions")
    int count();
}
