package com.lucky9.app;

import android.app.Application;
import android.util.Log;

import com.lucky9.app.data.AppDatabase;
import com.lucky9.app.repo.WalletRepository;
import com.lucky9.app.util.CrashHandler;

public class App extends Application {

    private static final String TAG = "Lucky9";

    private AppDatabase database;
    private WalletRepository walletRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.install(this);
        try {
            database = AppDatabase.getInstance(this);
            walletRepository = new WalletRepository(database);
            walletRepository.ensureSeed();
        } catch (Throwable t) {
            Log.e(TAG, "App.onCreate failed", t);
        }
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public WalletRepository getWalletRepository() {
        return walletRepository;
    }
}
