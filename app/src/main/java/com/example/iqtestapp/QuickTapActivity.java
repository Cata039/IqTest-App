package com.example.iqtestapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuickTapActivity extends AppCompatActivity {
    private static final int TOTAL_ROUNDS = 3;

    private Chronometer chronometer;
    private GridLayout  grid;
    private EditText    etAnswer;
    private View        btnSubmit;
    private TextView    tvRoundLabel;

    private String      playerName;
    private int         playerAge;
    private String      playerGender;
    private DBHelper    dbHelper;
    private Handler     handler;

    private int         currentRound = 1;
    private long        totalDuration = 0;
    private int         correctCount = 0;

    private final String[][][] boards = {
            {
                    { "06", "68", "88", "??", "98" },
                    { "??", "91", "11", "90", "10" }
            },
            {
                    { "33", "??", "89", "90", "??" },
                    { "31", "35", "34", "36", "37" }
            },
            {
                    { "72", "??", "84", "??", "86" },
                    { "73", "74", "75", "76", "77" }
            }
    };

    private final Set<String>[] solutions = new Set[]{
            new HashSet<>(Arrays.asList("87", "21")),
            new HashSet<>(Arrays.asList("32", "91")),
            new HashSet<>(Arrays.asList("83", "85"))
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quicktap);

        Intent in = getIntent();
        playerName   = in.getStringExtra("name");
        playerAge    = in.getIntExtra("age", 0);
        playerGender = in.getStringExtra("gender");

        dbHelper = new DBHelper(this);
        handler  = new Handler();

        wireUi();
        startRound();
    }

    private void wireUi() {
        chronometer    = findViewById(R.id.chronometer_quicktap);
        grid           = findViewById(R.id.grid_quicktap);
        etAnswer       = findViewById(R.id.et_quicktap_answer);
        btnSubmit      = findViewById(R.id.btn_quicktap_submit);
        tvRoundLabel   = findViewById(R.id.tv_round_label);

        btnSubmit.setOnClickListener(v -> onSubmit());
    }

    private void startRound() {
        tvRoundLabel.setText("Round " + currentRound + " of " + TOTAL_ROUNDS);

        grid.removeAllViews();
        String[][] boardValues = boards[currentRound - 1];

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 5; c++) {
                TextView cell = new TextView(this);
                cell.setText(boardValues[r][c]);
                cell.setTextColor(0xFFFFFFFF);
                cell.setTextSize(18f);
                cell.setPadding(16,16,16,16);
                cell.setBackgroundResource(R.drawable.tile_dark);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(r,1f),
                        GridLayout.spec(c,1f)
                );
                lp.width = 0;
                lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
                lp.setMargins(4,4,4,4);
                cell.setLayoutParams(lp);
                grid.addView(cell);
            }
        }

        etAnswer.setText("");
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    private void onSubmit() {
        String input = etAnswer.getText().toString().trim().replaceAll("\\s", "");
        String[] parts = input.split(",");
        Set<String> guess = new HashSet<>();
        if (parts.length == 2) guess = new HashSet<>(Arrays.asList(parts));
        Set<String> correctAnswers = solutions[currentRound - 1];
        boolean correct = guess.equals(correctAnswers);

        chronometer.stop();
        long secs = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
        totalDuration += secs;
        if (correct) correctCount++;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_game_over, null);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
        tvMsg.setText(correct
                ? "Correct! You found " + String.join(" & ", correctAnswers) + " in " + secs + "s"
                : "Oops—correct was " + String.join(",", correctAnswers) + ". You took " + secs + "s"
        );

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btn_next)
                .setOnClickListener(v -> {
                    dlg.dismiss();
                    if (currentRound < TOTAL_ROUNDS) {
                        currentRound++;
                        startRound();
                    } else {
                        String summaryResult = "(" + correctCount + "/" + TOTAL_ROUNDS + ")";
                        dbHelper.insertResult(
                                playerName, playerAge, playerGender,
                                "Quick Tap", totalDuration,
                                summaryResult
                        );
                        navigateToResults();
                    }
                });

        dlg.show();
    }

    private void navigateToResults() {
        Intent result = new Intent(
                QuickTapActivity.this,
                ResultActivity.class
        );
        result.putExtra("name",   playerName);
        result.putExtra("age",    playerAge);
        result.putExtra("gender", playerGender);
        result.putExtra("show_latest_only", true);
        result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(result);
        finish();
    }
}