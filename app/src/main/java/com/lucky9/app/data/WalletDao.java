package com.lucky9.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface WalletDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIfMissing(WalletEntity wallet);

    @Update
    void update(WalletEntity wallet);

    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    WalletEntity getSync();
}
