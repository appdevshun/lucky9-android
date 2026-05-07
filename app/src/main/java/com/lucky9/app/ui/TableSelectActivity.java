package com.lucky9.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.App;
import com.lucky9.app.R;
import com.lucky9.app.engine.Tier;
import com.lucky9.app.repo.WalletRepository;

import java.math.BigDecimal;

public class TableSelectActivity extends AppCompatActivity {

    public static final String EXTRA_TIER = "tier";
    public static final String EXTRA_SEATS = "seats";

    private TextView balanceLabel;
    private TextView seatsValue;
    private SeekBar seatsSeek;
    private RadioGroup tierGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_select);

        balanceLabel = findViewById(R.id.balance_label);
        seatsValue = findViewById(R.id.seats_value);
        seatsSeek = findViewById(R.id.seats_seek);
        tierGroup = findViewById(R.id.tier_group);
        MaterialButton start = findViewById(R.id.btn_start);

        seatsSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seatsValue.setText(String.valueOf(progress + 2));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seatsValue.setText(String.valueOf(seatsSeek.getProgress() + 2));

        start.setOnClickListener(v -> launch());
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            BigDecimal bal = repo.getSync().balance;
            runOnUiThread(() ->
                    balanceLabel.setText("Balance: " + bal.stripTrailingZeros().toPlainString()));
        });
    }

    private void launch() {
        Tier tier;
        int id = tierGroup.getCheckedRadioButtonId();
        if (id == R.id.tier_50) tier = Tier.T50;
        else if (id == R.id.tier_200) tier = Tier.T200;
        else if (id == R.id.tier_500) tier = Tier.T500;
        else if (id == R.id.tier_1000) tier = Tier.T1000;
        else tier = Tier.T100;

        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            BigDecimal bal = repo.getSync().balance;
            if (bal.compareTo(tier.minimum()) < 0) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.not_enough_balance, Toast.LENGTH_LONG).show());
                return;
            }
            int seats = seatsSeek.getProgress() + 2; // 2..8
            runOnUiThread(() -> {
                Intent i = new Intent(this, GameActivity.class);
                i.putExtra(EXTRA_TIER, tier.name());
                i.putExtra(EXTRA_SEATS, seats);
                startActivity(i);
            });
        });
    }
}
