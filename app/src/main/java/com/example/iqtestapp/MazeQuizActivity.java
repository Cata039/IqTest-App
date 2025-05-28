package com.example.iqtestapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class MazeQuizActivity extends AppCompatActivity {
    private Chronometer chronometer;
    private ImageView ivFloor, iv3d;
    private TextView tvQuestion;
    private EditText etAnswer;
    private Button btnSubmit;
    private LinearLayout flagsContainer;
    private Handler handler = new Handler();

    // exactly 3 rounds, one image pair per round:
    private final int[] floorImages = {
            R.drawable.round1_maze,
            R.drawable.round2_maze,
            R.drawable.round3_maze
    };
    private final int[] mazeImages = {
            R.drawable.round1_3d,
            R.drawable.round2_3d,
            R.drawable.round3_3d
    };

    // the "answer" overlays to show after each round:
    private final int[] floorAnswerImages = {
            R.drawable.round1_maze_answer,
            R.drawable.round2_maze_answer,
            R.drawable.round3_maze_answer
    };
    private final int[] mazeAnswerImages = {
            R.drawable.round1_3d_answer,
            R.drawable.round2_3d_answer,
            R.drawable.round3_3d_answer
    };

    // correct answer for each round (updated)
    private final String[] answers = {
            "I21",  // round 1
            "S13",  // round 2
            "O16"   // round 3
    };

    private int currentRound = 0;
    private int attemptsLeft = 3;
    private int score = 0;

    // player info + DB
    private String    playerName;
    private int       playerAge;
    private String    playerGender;
    private DBHelper  dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maze_quiz);

        // pull extras
        playerName   = getIntent().getStringExtra("name");
        playerAge    = getIntent().getIntExtra("age", 0);
        playerGender = getIntent().getStringExtra("gender");
        dbHelper     = new DBHelper(this);

        // wire UI
        chronometer     = findViewById(R.id.chronometer_maze);
        ivFloor         = findViewById(R.id.iv_floor_plan);
        iv3d            = findViewById(R.id.iv_3d_maze);
        tvQuestion      = findViewById(R.id.tv_maze_question);
        etAnswer        = findViewById(R.id.et_maze_answer);
        btnSubmit       = findViewById(R.id.btn_maze_submit);
        flagsContainer  = findViewById(R.id.flags_container);

        // start timer
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        loadRound();

        btnSubmit.setOnClickListener(v -> checkAnswer());
    }

    private void loadRound() {
        // 1) show splash
        showRoundSplash(currentRound + 1);

        // 2) clear flags from previous round
        flagsContainer.removeAllViews();

        // 3) reset input & tint & attempts
        etAnswer.setText("");
        etAnswer.setBackgroundTintList(
                ColorStateList.valueOf(Color.WHITE)
        );
        attemptsLeft = 3;

        // 4) set images for current round
        ivFloor.setImageResource(floorImages[currentRound]);
        iv3d  .setImageResource(mazeImages [currentRound]);

        // 5) update question label
        tvQuestion.setText(
                "Round " + (currentRound+1) + " — 3 attempts"
        );
    }

    private void showRoundSplash(int roundNum) {
        // 1) Create a full-screen Dialog
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dlg.setContentView(R.layout.dialog_round_splash_fullscreen);

        // 2) Set the round text
        TextView tv = dlg.findViewById(R.id.tv_round_splash);
        tv.setText("ROUND " + roundNum);

        // 3) Load and start the pulse animation
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        tv.startAnimation(pulse);

        // 4) Show and auto-dismiss after 1s
        dlg.show();
        handler.postDelayed(dlg::dismiss, 3000);
    }


    private void checkAnswer() {
        String guess   = etAnswer.getText().toString()
                .trim().toUpperCase();
        String correct = answers[currentRound];

        if (guess.equals(correct)) {
            score++;
            showFlag(true);
            etAnswer.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#A5D6A7")  // green
                    )
            );
            revealAndNext();
        } else {
            attemptsLeft--;
            showFlag(false);
            etAnswer.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#EF9A9A")  // red
                    )
            );
            if (attemptsLeft > 0) {
                Toast.makeText(this,
                        "Wrong! " + attemptsLeft + " tries left.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                revealAndNext();
            }
        }
    }

    private void showFlag(boolean correct) {
        ImageView flag = new ImageView(this);
        flag.setImageResource(
                correct
                        ? R.drawable.ic_flag_green
                        : R.drawable.ic_flag_red
        );
        int size = (int)(24 * getResources()
                .getDisplayMetrics().density);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(size, size);
        lp.setMarginStart(4);
        // add to flagsContainer—won't push the EditText
        flagsContainer.addView(flag, lp);
    }

    private void revealAndNext() {
        // swap in the annotated answer overlay
        ivFloor.setImageResource(floorAnswerImages[currentRound]);
        iv3d  .setImageResource(mazeAnswerImages[currentRound]);

        // after 5 seconds, advance
        handler.postDelayed(() -> {
            if (++currentRound >= floorImages.length) {
                finishQuiz();
            } else {
                loadRound();
            }
        }, 5000);
    }

    private void finishQuiz() {
        chronometer.stop();
        long secs = (SystemClock.elapsedRealtime()
                - chronometer.getBase()) / 1000;

        // record in DB
        dbHelper.insertResult(
                playerName, playerAge, playerGender,
                "Maze Code", secs,
                score + " / " + floorImages.length
        );

        // show final custom dialog
        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_game_over, null, false);
        TextView tv = dialogView.findViewById(R.id.tv_dialog_message);
        tv.setText(
                playerName + " scored " + score +
                        " out of " + floorImages.length +
                        "\nin " + secs + " seconds"
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();


        dialogView.findViewById(R.id.btn_next)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(this, QuickTapInstructionsActivity.class);
                    intent.putExtra("name", playerName);
                    intent.putExtra("age", playerAge);
                    intent.putExtra("gender", playerGender);
                    startActivity(intent);
                    finish();
                });

        dialog.show();
    }
}
