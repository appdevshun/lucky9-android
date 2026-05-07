package com.lucky9.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucky9.app.R;
import com.lucky9.app.data.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    private final List<TransactionEntity> data = new ArrayList<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public void setData(List<TransactionEntity> rows) {
        data.clear();
        if (rows != null) data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TransactionEntity tx = data.get(position);
        holder.type.setText(tx.type);
        holder.time.setText(formatter.format(new Date(tx.timestampMillis)));
        holder.note.setText(tx.note == null ? "" : tx.note);
        String prefix = tx.amount.signum() >= 0 ? "+" : "";
        holder.amount.setText(prefix + tx.amount.stripTrailingZeros().toPlainString());
        holder.amount.setTextColor(tx.amount.signum() >= 0 ? 0xFF22C55E : 0xFFEF4444);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView type, time, note, amount;

        VH(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.tx_type);
            time = itemView.findViewById(R.id.tx_time);
            note = itemView.findViewById(R.id.tx_note);
            amount = itemView.findViewById(R.id.tx_amount);
        }
    }
}
