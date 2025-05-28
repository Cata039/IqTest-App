package com.example.iqtestapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.Random;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private TextView tvPlayerInfo;
    private TextView tvIqScore;
    private Button btnShowIq;
    private LinearLayout llGameResults;
    private String playerName;
    private int playerAge;
    private String playerGender;
    private DBHelper dbHelper;
    private boolean iqRevealed = false;
    private LinearLayout llPlayerRanking;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize views
        tvPlayerInfo = findViewById(R.id.tv_player_info);
        tvIqScore = findViewById(R.id.tv_iq_score);
        btnShowIq = findViewById(R.id.btn_show_iq);
        llGameResults = findViewById(R.id.ll_game_results);
        llPlayerRanking = findViewById(R.id.ll_player_ranking);


        // Get player info from intent and validate
        if (!validatePlayerInfo()) {
            showErrorAndFinish("Invalid player information");
            return;
        }

        // Initialize database helper
        try {
            dbHelper = new DBHelper(this);
            // Verify database is accessible
            if (!verifyDatabase()) {
                showErrorAndFinish("Database error");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Database initialization error", e);
            showErrorAndFinish("Database initialization failed");
            return;
        }
        // Display player info
        displayPlayerInfo();
        // Display game results
        displayGameResults();
        // Set up IQ reveal button
        btnShowIq.setOnClickListener(v -> revealIqScore());
        //show the rank
        displayPlayerRankings();
    }

    private boolean validatePlayerInfo() {
        try {
            playerName = getIntent().getStringExtra("name");
            playerAge = getIntent().getIntExtra("age", -1);
            playerGender = getIntent().getStringExtra("gender");

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
        } catch (Exception e) {
            Log.e(TAG, "Error validating player info", e);
            return false;
        }
    }
    private boolean verifyDatabase() {
        try {
            dbHelper.getResultsForPlayer(playerName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Database verification failed", e);
            return false;
        }
    }
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
    private void displayPlayerInfo() {
        try {
            String info = String.format("%s, %d years old, %s",
                playerName, playerAge, playerGender);
            tvPlayerInfo.setText(info);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying player info", e);
            tvPlayerInfo.setText("Error displaying player information");
        }
    }
    private void displayGameResults() {
        try {
            List<DBHelper.GameResult> results = dbHelper.getResultsForPlayer(playerName);

            if (results == null || results.isEmpty()) {
                TextView noResults = new TextView(this);
                noResults.setText("No game results found.");
                noResults.setTextColor(Color.WHITE);
                noResults.setTextSize(16f);
                noResults.setPadding(0, 8, 0, 8);
                llGameResults.addView(noResults);
                return;
            }

            // Only show the most recent session (last 3 results)
            int startIndex = Math.max(0, results.size() - 3);
            List<DBHelper.GameResult> latestSession = results.subList(startIndex, results.size());

            for (DBHelper.GameResult result : latestSession) {
                TextView resultView = new TextView(this);
                resultView.setTextColor(Color.WHITE);
                resultView.setTextSize(16f);
                resultView.setPadding(0, 8, 0, 8);

                String resultText = result.result.startsWith("(")
                        ? String.format("%s: %ds %s", result.game, result.duration, result.result)
                        : String.format("%s: %ds (%s)", result.game, result.duration, result.result);

                resultView.setText(resultText);
                llGameResults.addView(resultView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying game results", e);
            TextView errorView = new TextView(this);
            errorView.setText("Error loading game results. Please try again.");
            errorView.setTextColor(Color.WHITE);
            errorView.setTextSize(16f);
            errorView.setPadding(0, 8, 0, 8);
            llGameResults.addView(errorView);
        }
    }

    private void displayPlayerRankings() {
        try {
            List<DBHelper.PlayerInfo> players = dbHelper.getAllRankedPlayers();
            int rank = 1;

            for (DBHelper.PlayerInfo player : players) {
                TextView row = new TextView(this);

                // Medal logic
                String medal = "";
                if (rank == 1) medal = "🥇 ";
                else if (rank == 2) medal = "🥈 ";
                else if (rank == 3) medal = "🥉 ";

                String displayText = String.format("%s%d.  %s (%d yrs):  IQ %d", medal, rank, player.name, player.age, player.iq);

                // Highlight current player
                boolean isCurrentPlayer = player.name.equals(playerName)
                        && player.age == playerAge
                        && player.gender.equalsIgnoreCase(playerGender);

                if (isCurrentPlayer) {
                    row.setTextColor(Color.WHITE);
                    row.setTypeface(getResources().getFont(R.font.kranky), android.graphics.Typeface.BOLD);
                    row.setTextSize(18f);
                } else {
                    row.setTextColor(Color.WHITE);
                    row.setTextSize(16f);
                }

                row.setText(displayText);
                row.setPadding(0, 8, 0, 8);
                llPlayerRanking.addView(row);
                rank++;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying player rankings", e);
        }
    }



    private void revealIqScore() {
        try {
            if (!iqRevealed) {
                int iqScore;

                // Try to parse IQ from visible text if already shown
                String iqText = tvIqScore.getText().toString();
                if (iqText != null && iqText.startsWith("Your IQ:")) {
                    try {
                        iqScore = Integer.parseInt(iqText.split(":")[1].trim());
                    } catch (Exception e) {
                        iqScore = calculateIqScore();
                    }
                } else {
                    iqScore = calculateIqScore();
                }

                // Display IQ
                tvIqScore.setText(String.format("Your IQ: %d", iqScore));
                tvIqScore.setVisibility(View.VISIBLE);

                // Save to DB
                dbHelper.upsertIqScore(playerName, playerAge, playerGender, iqScore);

                // Refresh leaderboard
                llPlayerRanking.removeAllViews();
                displayPlayerRankings();

                btnShowIq.setText("Hide IQ");
                iqRevealed = true;
            } else {
                tvIqScore.setVisibility(View.GONE);
                btnShowIq.setText("Show IQ");
                iqRevealed = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error revealing IQ score", e);
            Toast.makeText(this, "Error revealing IQ", Toast.LENGTH_SHORT).show();
        }
    }



    private int calculateIqScore() {
        try {
            List<DBHelper.GameResult> results = dbHelper.getResultsForPlayer(playerName);
            if (results == null || results.isEmpty()) {
                return 95; // Lower default to reflect no performance
            }

            int iq = 95;
            int winCount = 0;
            int lossCount = 0;
            int mazeCorrect = 0, mazeTotal = 0;

            for (DBHelper.GameResult result : results) {
                boolean isWin = result.result.contains("won");
                boolean isMaze = result.game.contains("Maze") && result.result.contains("/");

                // Maze Code special case (e.g., "2 / 3")
                if (isMaze) {
                    try {
                        String[] parts = result.result.split("/");
                        mazeCorrect = Integer.parseInt(parts[0].replaceAll("[^0-9]", "").trim());
                        mazeTotal = Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
                    } catch (Exception ignored) {
                    }
                }

                if (isWin) {
                    iq += 7;
                    winCount++;
                    // Speed bonus only for wins
                    if (result.duration < 30) {
                        iq += 4;
                    } else if (result.duration < 60) {
                        iq += 2;
                    }
                } else {
                    lossCount++;
                    iq -= 2;
                }
            }

            // Maze Code performance bonus (up to +15)
            if (mazeTotal > 0) {
                iq += (int) (15.0 * mazeCorrect / mazeTotal);
            } else {
                iq -= 5; // Penalize for not answering any maze question
            }

            // If all games are lost and Maze is 0, set minimum IQ
            if (winCount == 0 && mazeCorrect == 0) {
                iq = 85;
            }

            // Clamp IQ to a realistic range
            iq = Math.max(80, Math.min(140, iq));
            return iq;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating IQ score", e);
            return 95; // Safer fallback default
        }
    }
}
