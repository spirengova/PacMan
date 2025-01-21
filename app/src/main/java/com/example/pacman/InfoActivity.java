package com.example.pacman;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Button backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> finish());
    }
}
