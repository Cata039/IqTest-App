package com.example.iqtestapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class QuickTapInstructionsActivity extends AppCompatActivity {
    private static final String TAG = "QuickTapInstructions";
    private String playerName;
    private int playerAge;
    private String playerGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quicktap_instructions);

        // Get and validate player info
        Intent in = getIntent();
        playerName = in.getStringExtra("name");
        playerAge = in.getIntExtra("age", -1);
        playerGender = in.getStringExtra("gender");

        if (!validatePlayerInfo()) {
            Toast.makeText(this, "Invalid player information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // grab the "Play" button
        Button play = findViewById(R.id.btn_quicktap_play);
        play.setOnClickListener(v -> {
            Intent i = new Intent(this, QuickTapActivity.class);
            i.putExtra("name", playerName);
            i.putExtra("age", playerAge);
            i.putExtra("gender", playerGender);
            startActivity(i);
            finish();
        });
    }

    private boolean validatePlayerInfo() {
        if (playerName == null || playerName.trim().isEmpty()) {
            Log.e(TAG, "Player name is missing or empty");
            return false;
        }
        if (playerAge <= 0) {
            Log.e(TAG, "Invalid player age: " + playerAge);
            return false;
        }
        if (playerGender == null || playerGender.trim().isEmpty()) {
            Log.e(TAG, "Player gender is missing or empty");
            return false;
        }
        return true;
    }
}
