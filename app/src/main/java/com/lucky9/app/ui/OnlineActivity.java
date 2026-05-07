package com.lucky9.app.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.lucky9.app.R;

/** Online multiplayer entry. Wire to Node.js relay in a follow-up. */
public class OnlineActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

        EditText roomCode = findViewById(R.id.room_code);
        MaterialButton create = findViewById(R.id.btn_create);
        MaterialButton join = findViewById(R.id.btn_join);
        MaterialButton back = findViewById(R.id.btn_back);

        create.setOnClickListener(v -> Toast.makeText(this, "Relay scaffold ready — connect a server in Settings", Toast.LENGTH_LONG).show());
        join.setOnClickListener(v -> {
            String code = roomCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter a room code", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Relay scaffold ready — connect a server in Settings", Toast.LENGTH_LONG).show();
        });
        back.setOnClickListener(v -> finish());
    }
}
