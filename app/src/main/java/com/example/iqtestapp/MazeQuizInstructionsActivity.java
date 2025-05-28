package com.example.iqtestapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MazeQuizInstructionsActivity extends AppCompatActivity {
    private static final String TAG = "MazeQuizInstructions";
    private String playerName;
    private int playerAge;
    private String playerGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maze_instructions);

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

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button play = findViewById(R.id.btn_play_maze);
        play.setOnClickListener(v -> {
            Intent intent = new Intent(this, MazeQuizActivity.class);
            intent.putExtra("name", playerName);
            intent.putExtra("age", playerAge);
            intent.putExtra("gender", playerGender);
            startActivity(intent);
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
