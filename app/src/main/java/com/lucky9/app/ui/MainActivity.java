package com.lucky9.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.App;
import com.lucky9.app.R;
import com.lucky9.app.repo.WalletRepository;
import com.lucky9.app.util.CrashHandler;

public class MainActivity extends AppCompatActivity {

    private TextView balanceLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        balanceLabel = findViewById(R.id.balance_label);
        bind(R.id.btn_play_bots, () -> startActivity(new Intent(this, TableSelectActivity.class)));
        bind(R.id.btn_hotspot, () -> startActivity(new Intent(this, HotspotActivity.class)));
        bind(R.id.btn_online, () -> startActivity(new Intent(this, OnlineActivity.class)));
        bind(R.id.btn_wallet, () -> startActivity(new Intent(this, WalletActivity.class)));
        bind(R.id.btn_rules, () -> startActivity(new Intent(this, RulesActivity.class)));
        bind(R.id.btn_tutorial, () -> startActivity(new Intent(this, TutorialActivity.class)));
        bind(R.id.btn_settings, () -> startActivity(new Intent(this, SettingsActivity.class)));

        showLastCrashIfAny();
    }

    private void showLastCrashIfAny() {
        String trace = CrashHandler.readLastCrash(this);
        if (trace == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Previous crash detected")
                .setMessage(trace)
                .setPositiveButton("Copy & dismiss", (d, w) -> {
                    android.content.ClipboardManager cm =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", trace));
                        Toast.makeText(this, "Trace copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                    CrashHandler.clear(this);
                })
                .setNegativeButton("Dismiss", (d, w) -> CrashHandler.clear(this))
                .show();
    }

    private void bind(int id, Runnable r) {
        MaterialButton b = findViewById(id);
        b.setOnClickListener(v -> r.run());
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            String text = "Wallet: " + repo.getSync().balance.stripTrailingZeros().toPlainString() + " coins";
            runOnUiThread(() -> balanceLabel.setText(text));
        });
    }
}
