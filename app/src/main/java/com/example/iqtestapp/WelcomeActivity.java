package com.example.iqtestapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 1) Find the Next button
        Button nextButton = findViewById(R.id.btn_next);

        // 2) Set a click‐listener on it
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 3) Create an Intent to start InfoActivity
                Intent intent = new Intent(WelcomeActivity.this, InfoActivity.class);
                startActivity(intent);
                // 4) Optionally finish() if you don't want to return to welcome
                // finish();
            }
        });
    }
}
