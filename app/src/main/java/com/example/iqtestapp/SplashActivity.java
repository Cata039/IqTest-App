package com.example.iqtestapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ProgressBar;


public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 3000;

    private ProgressBar progressBar;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progress_bar);
        simulateProgressBar();


        TextView tv = findViewById(R.id.tv_splash_title);
        // load & start fade-in
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        tv.startAnimation(fadeIn);
    }

    private void simulateProgressBar() {
        final int delay = 30; // ms
        final int[] progress = {0};

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress[0]++;
                progressBar.setProgress(progress[0]);

                if (progress[0] < 100) {
                    handler.postDelayed(this, delay);
                } else {
                    // When full, launch WelcomeActivity
                    startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                    finish();
                }
            }
        }, delay);
    }


}
