package com.lucky9.app.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.lucky9.app.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "lucky9_prefs";
    public static final String KEY_SOUND = "sound";
    public static final String KEY_TAGALOG = "tagalog";
    public static final String KEY_RELAY_URL = "relay_url";
    public static final String DEFAULT_RELAY = "wss://your-relay.example.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        MaterialSwitch swSound = findViewById(R.id.sw_sound);
        MaterialSwitch swTagalog = findViewById(R.id.sw_tagalog);
        EditText relay = findViewById(R.id.relay_url);
        MaterialButton save = findViewById(R.id.btn_save);

        swSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        swTagalog.setChecked(prefs.getBoolean(KEY_TAGALOG, true));
        relay.setText(prefs.getString(KEY_RELAY_URL, DEFAULT_RELAY));

        save.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(KEY_SOUND, swSound.isChecked())
                    .putBoolean(KEY_TAGALOG, swTagalog.isChecked())
                    .putString(KEY_RELAY_URL, relay.getText().toString().trim())
                    .apply();
            Toast.makeText(this, R.string.ok, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
