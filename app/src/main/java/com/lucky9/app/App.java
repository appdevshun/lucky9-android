package com.lucky9.app;

import android.app.Application;

import com.lucky9.app.data.AppDatabase;
import com.lucky9.app.repo.WalletRepository;

public class App extends Application {

    private AppDatabase database;
    private WalletRepository walletRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
        walletRepository = new WalletRepository(database);
        // Ensure the wallet row exists with starting balance.
        walletRepository.ensureSeed();
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public WalletRepository getWalletRepository() {
        return walletRepository;
    }
}
