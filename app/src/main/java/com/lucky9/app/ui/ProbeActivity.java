package com.lucky9.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.lucky9.app.App;
import com.lucky9.app.R;
import com.lucky9.app.data.AppDatabase;
import com.lucky9.app.repo.WalletRepository;
import com.lucky9.app.util.CrashHandler;

/**
 * Bare-bones diagnostic activity. Uses only framework widgets and the
 * default holo dark theme so it cannot trip on Material or AppCompat
 * resource resolution. Each probe step is rendered to the screen so the
 * user can identify where the launch is failing without ADB.
 */
public class ProbeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF073A1B);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 80, 40, 80);
        scroll.addView(box, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView header = makeText("Lucky 9 Probe", 22f, 0xFFF4C542, Typeface.BOLD);
        box.addView(header);

        StringBuilder log = new StringBuilder();
        log.append("Build: ").append(android.os.Build.MANUFACTURER)
                .append(' ').append(android.os.Build.MODEL)
                .append(" / API ").append(android.os.Build.VERSION.SDK_INT).append('\n');

        runProbe(log, "Application class", () -> {
            Object a = getApplication();
            return a == null ? "null Application" : a.getClass().getName();
        });

        runProbe(log, "Application class name", () -> getApplication().getClass().getName());

        runProbe(log, "R.string.app_name", () -> getString(R.string.app_name));

        runProbe(log, "Inflate activity_main", () -> {
            View v = getLayoutInflater().inflate(R.layout.activity_main, null);
            return v == null ? "null view" : v.getClass().getSimpleName();
        });

        runProbe(log, "Inflate activity_settings", () -> {
            View v = getLayoutInflater().inflate(R.layout.activity_settings, null);
            return v == null ? "null view" : v.getClass().getSimpleName();
        });

        runProbe(log, "Room AppDatabase.getInstance", () -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            return db == null ? "null" : "ok";
        });

        runProbe(log, "Wallet read (getSync)", () -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            // Force a real DB hit on a worker thread.
            final String[] result = new String[]{"timeout"};
            Thread t = new Thread(() -> {
                try {
                    Object w = db.walletDao().getSync();
                    result[0] = (w == null ? "no row yet" : "row present");
                } catch (Throwable ex) {
                    result[0] = "ERR " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                }
            });
            t.start();
            t.join(3000);
            return result[0];
        });

        runProbe(log, "Read previous crash", () -> {
            String trace = CrashHandler.readLastCrash(this);
            return trace == null ? "(none)" : ("present, " + trace.length() + " chars");
        });

        TextView pre = new TextView(this);
        pre.setText(log);
        pre.setTextColor(0xFFFFFFFF);
        pre.setTextIsSelectable(true);
        pre.setTypeface(Typeface.MONOSPACE);
        pre.setTextSize(12f);
        pre.setPadding(20, 30, 20, 30);
        pre.setBackgroundColor(0xFF0A1F12);
        box.addView(pre, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button copy = new Button(this);
        copy.setText("Copy probe log");
        copy.setOnClickListener(v -> {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("probe", pre.getText().toString()));
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(copy, marginParams(20));

        Button viewCrash = new Button(this);
        viewCrash.setText("Show last crash trace");
        viewCrash.setOnClickListener(v -> {
            String trace = CrashHandler.readLastCrash(this);
            if (trace == null) {
                Toast.makeText(this, "No crash recorded", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", trace));
            }
            TextView t = new TextView(this);
            t.setText(trace);
            t.setTypeface(Typeface.MONOSPACE);
            t.setTextSize(11f);
            t.setTextColor(0xFFFFC0C0);
            t.setTextIsSelectable(true);
            t.setPadding(20, 20, 20, 20);
            t.setBackgroundColor(0xFF1A0A0A);
            box.addView(t, marginParams(20));
            Toast.makeText(this, "Crash trace also copied to clipboard", Toast.LENGTH_LONG).show();
        });
        box.addView(viewCrash, marginParams(10));

        Button cont = new Button(this);
        cont.setText("Continue to game");
        cont.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, MainActivity.class));
            } catch (Throwable th) {
                Toast.makeText(this, "Crash launching MainActivity: " + th, Toast.LENGTH_LONG).show();
            }
        });
        box.addView(cont, marginParams(20));

        setContentView(scroll);
    }

    private LinearLayout.LayoutParams marginParams(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        float d = getResources().getDisplayMetrics().density;
        lp.topMargin = (int) (topDp * d);
        return lp;
    }

    private TextView makeText(String s, float sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(0, 0, 0, 30);
        return t;
    }

    private interface Probe { String run() throws Exception; }

    private void runProbe(StringBuilder log, String name, Probe p) {
        log.append('[').append(name).append("] ");
        try {
            String res = p.run();
            log.append("OK  ").append(res);
        } catch (Throwable t) {
            log.append("FAIL ").append(t.getClass().getSimpleName())
                    .append(": ").append(t.getMessage());
            Throwable cause = t.getCause();
            if (cause != null && cause != t) {
                log.append(" / cause=").append(cause.getClass().getSimpleName())
                        .append(": ").append(cause.getMessage());
            }
        }
        log.append('\n');
    }
}
