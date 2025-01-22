package com.example.pacman;

import android.os.Bundle;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Získaj rozmery obrazovky
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        
        // Vytvor GameView s rozmermi obrazovky
        gameView = new GameView(this, size.x, size.y);
        setContentView(gameView);
        
        // Skontroluj, či existuje uložená hra
        SharedPreferences preferences = getSharedPreferences("PacManPrefs", MODE_PRIVATE);
        boolean hasSavedGame = preferences.contains("score");
        
        if (hasSavedGame) {
            // Zobraz dialóg s možnosťami
            new AlertDialog.Builder(this)
                .setTitle("Pac-Man")
                .setMessage("Chcete pokračovať v uloženej hre?")
                .setPositiveButton("Pokračovať", (dialog, which) -> {
                    gameView.continueGame();
                })
                .setNegativeButton("Nová hra", (dialog, which) -> {
                    gameView.startNewGame();
                })
                .setCancelable(false)
                .show();
        } else {
            // Ak nie je uložená hra, začni novú
            gameView.startNewGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
            gameView.saveState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
    }
}
