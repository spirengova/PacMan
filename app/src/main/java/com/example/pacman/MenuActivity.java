package com.example.pacman;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {
    private boolean hasSavedGame = false; // Track if there's a saved game state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button playButton = findViewById(R.id.buttonPlay);
        Button settingsButton = findViewById(R.id.buttonSettings);
        Button exitButton = findViewById(R.id.buttonExit);
        Button infoButton = findViewById(R.id.buttonInfo);

        playButton.setOnClickListener(v -> {
            if (hasSavedGame) {
                showGameOptionsDialog();
            } else {
                startNewGame();
            }
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        exitButton.setOnClickListener(v -> finish());

        infoButton.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, InfoActivity.class);
            startActivity(intent);
        });
    }

    private void showGameOptionsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Game Options")
            .setMessage("Do you want to start a new game or continue?")
            .setPositiveButton("New Game", (dialog, which) -> startNewGame())
            .setNegativeButton("Continue", (dialog, which) -> continueGame())
            .show();
    }

    private void startNewGame() {
        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        intent.putExtra("newGame", true);
        startActivity(intent);
    }

    private void continueGame() {
        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        intent.putExtra("newGame", false);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if there's a saved game state
        hasSavedGame = getSharedPreferences("PacManPrefs", MODE_PRIVATE)
            .getBoolean("hasSavedGame", false);
    }
}
