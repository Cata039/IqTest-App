package com.example.iqtestapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        final EditText nameField   = findViewById(R.id.et_name);
        final EditText ageField    = findViewById(R.id.et_age);
        final RadioGroup genderGrp = findViewById(R.id.rg_gender);
        Button startBtn            = findViewById(R.id.btn_start);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    // 1) Read and validate the inputs
                    String name = nameField.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(InfoActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String ageText = ageField.getText().toString().trim();
                    if (ageText.isEmpty()) {
                        Toast.makeText(InfoActivity.this, "Please enter your age", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int age = Integer.parseInt(ageText);
                    if (age <= 0 || age > 120) {
                        Toast.makeText(InfoActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int checkedId = genderGrp.getCheckedRadioButtonId();
                    String gender;
                    if (checkedId == -1) {
                        Toast.makeText(InfoActivity.this, "Please select your gender", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (checkedId == R.id.rb_male) {
                        gender = "Male";
                    } else if (checkedId == R.id.rb_female) {
                        gender = "Female";
                    } else if (checkedId == R.id.rb_other) {
                        gender = "Other";
                    } else {
                        gender = "Unspecified";
                    }

                    // 2) Pack into Intent extras
                    Intent intent = new Intent(
                            InfoActivity.this,
                            TurncoatInstructionsActivity.class
                    );
                    intent.putExtra("name", name);
                    intent.putExtra("age", age);
                    intent.putExtra("gender", gender);

                    // 3) Launch first game instructions
                    startActivity(intent);
                    finish();
                } catch (NumberFormatException e) {
                    Toast.makeText(InfoActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
