package com.lucky9.app.ui;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lucky9.app.R;

public class TutorialActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        TextView text = findViewById(R.id.tutorial_text);
        String html = getString(R.string.tutorial_steps);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            text.setText(Html.fromHtml(html));
        }
    }
}
