package com.example.pacman;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "PacManPrefs";
    private static final String SOUND_ENABLED = "sound_enabled";
    private static final String DIFFICULTY_LEVEL = "difficulty_level";

    private SharedPreferences preferences;
    private SeekBar volumeSeekBar;
    private Spinner difficultySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Exit button
        Button exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            // Save any pending changes
            preferences.edit().apply();
            finish();
        });

        // Volume control
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeSeekBar.setProgress(preferences.getBoolean(SOUND_ENABLED, true) ? 1 : 0);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preferences.edit().putBoolean(SOUND_ENABLED, progress > 0).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Difficulty level
        difficultySpinner = findViewById(R.id.difficultySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);

        int savedDifficulty = preferences.getInt(DIFFICULTY_LEVEL, 1); // Default to medium
        difficultySpinner.setSelection(savedDifficulty);

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putInt(DIFFICULTY_LEVEL, position).apply();
                Toast.makeText(SettingsActivity.this, "Difficulty set to " + parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onBackPressed() {
        // Save any pending changes before exiting
        preferences.edit().apply();
        super.onBackPressed();
    }
}