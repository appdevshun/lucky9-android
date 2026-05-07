package com.lucky9.app.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.App;
import com.lucky9.app.R;
import com.lucky9.app.repo.WalletRepository;

import java.math.BigDecimal;

public class WalletActivity extends AppCompatActivity {

    private TextView balanceValue;
    private final TransactionAdapter adapter = new TransactionAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        balanceValue = findViewById(R.id.balance_value);
        RecyclerView list = findViewById(R.id.tx_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.transactions().observe(this, txs -> adapter.setData(txs));

        MaterialButton btnDeposit = findViewById(R.id.btn_deposit);
        MaterialButton btnWithdraw = findViewById(R.id.btn_withdraw);
        MaterialButton btnClaim = findViewById(R.id.btn_claim);

        btnDeposit.setOnClickListener(v -> promptAmount(true));
        btnWithdraw.setOnClickListener(v -> promptAmount(false));
        btnClaim.setOnClickListener(v -> claimBonus());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBalance();
    }

    private void refreshBalance() {
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            BigDecimal bal = repo.getSync().balance;
            runOnUiThread(() -> balanceValue.setText(bal.stripTrailingZeros().toPlainString()));
        });
    }

    private void promptAmount(boolean deposit) {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setHint(deposit
                ? WalletRepository.DEFAULT_SIM_DEPOSIT.toPlainString()
                : WalletRepository.DEFAULT_SIM_WITHDRAW.toPlainString());

        new AlertDialog.Builder(this)
                .setTitle(deposit ? R.string.deposit : R.string.withdraw)
                .setMessage(R.string.academic_note)
                .setView(et)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    BigDecimal amount;
                    String raw = et.getText().toString().trim();
                    if (raw.isEmpty()) {
                        amount = deposit ? WalletRepository.DEFAULT_SIM_DEPOSIT : WalletRepository.DEFAULT_SIM_WITHDRAW;
                    } else {
                        try {
                            amount = new BigDecimal(raw);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    if (amount.signum() <= 0) {
                        Toast.makeText(this, "Enter a positive amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    BigDecimal delta = deposit ? amount : amount.negate();
                    WalletRepository repo = ((App) getApplication()).getWalletRepository();
                    repo.io().execute(() -> {
                        BigDecimal newBal = repo.applyDeltaSync(delta,
                                deposit ? WalletRepository.TX_DEPOSIT : WalletRepository.TX_WITHDRAW,
                                "Simulated " + (deposit ? "deposit" : "withdraw"));
                        runOnUiThread(() -> balanceValue.setText(newBal.stripTrailingZeros().toPlainString()));
                    });
                })
                .show();
    }

    private void claimBonus() {
        WalletRepository repo = ((App) getApplication()).getWalletRepository();
        repo.io().execute(() -> {
            BigDecimal newBal = repo.claimDailyBonusSync();
            runOnUiThread(() -> {
                if (newBal == null) {
                    Toast.makeText(this, R.string.bonus_already_claimed, Toast.LENGTH_SHORT).show();
                } else {
                    balanceValue.setText(newBal.stripTrailingZeros().toPlainString());
                    Toast.makeText(this, R.string.bonus_granted, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
