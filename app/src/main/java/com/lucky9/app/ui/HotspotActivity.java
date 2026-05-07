package com.lucky9.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.R;

/** Wi-Fi Hotspot multiplayer entry. Networking will be wired in a follow-up. */
public class HotspotActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotspot);
        MaterialButton back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());
    }
}
