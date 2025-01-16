package com.example.pacman;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Display;
import android.view.WindowManager;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.graphics.RectF;
import java.util.List;
import java.util.ArrayList;
import android.os.Bundle;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.app.AlertDialog;
import android.content.Intent;

public class GameView extends SurfaceView implements Runnable {
    private Thread gameThread;
    private SurfaceHolder holder;
    private volatile boolean playing;
    private Canvas canvas;
    private Paint paint;
    private long fps;
    private final int MILLIS_IN_SECOND = 1000;
    private Context context;
    
    private Maze maze;
    private PacMan pacman;
    private float touchX, touchY;
    private int screenX, screenY;
    private float scaleFactor;
    private SoundPool soundPool;
    private int eatDotSound;
    private int eatGhostSound;
    private int gameStartSound;
    private int deathSound;
    private MediaPlayer backgroundMusic;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "PacManPrefs";
    private static final String SOUND_ENABLED = "sound_enabled";
    private int score = 0;
    private Paint scorePaint;
    private static final int DOT_POINTS = 10;
    private static final int POWER_PELLET_POINTS = 50;
    private static final int GHOST_POINTS = 200;
    private List<Ghost> ghosts;
    private List<Fruit> fruits;
    private boolean isGameOver = false;
    private boolean allCollected = false;
    private static final int GHOST_COUNT = 4;
    private static final int FRUIT_COUNT = 4;
    private Paint gameOverPaint;
    private boolean powerMode = false;
    private long powerModeTime = 0;
    private static final long POWER_MODE_DURATION = 5000; // 5 sekúnd
    private int eatFruitSound;
    private boolean isPaused = false;
    private boolean speedBoost = false;
    private boolean doublePoints = false;
    private long speedBoostTime = 0;
    private long doublePointsTime = 0;
    private static final float SPEED_BOOST_MULTIPLIER = 1.5f;
    private static final int BOOST_DURATION = 5000; // 5 sekúnd pre všetky boosty
    private int lives = 3; // Začiatočný počet životov
    private Paint livesPaint; // Pre vykreslenie počtu životov
    private String boostMessage = "";
    private long boostMessageTime = 0;
    private static final int MESSAGE_DURATION = 5000; // Predĺžené na 5 sekúnd aby sa zobrazoval celý odpočet
    private Paint boostMessagePaint;
    private Paint boostTimerPaint;
    private int currentLevel = 1;
    private static final float GHOST_SPEED_INCREASE = 0.2f; // O koľko sa zvýši rýchlosť duchov v každom leveli
    private boolean isLevelComplete = false;

    public GameView(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        paint = new Paint();
        
        // Inicializácia score paint
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(50);
        scorePaint.setTextAlign(Paint.Align.LEFT);
        
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Inicializácia zvuku
        backgroundMusic = MediaPlayer.create(context, R.raw.sound);
        backgroundMusic.setLooping(true);
        
        if (preferences.getBoolean(SOUND_ENABLED, true)) {
            backgroundMusic.start();
        }
        
        // Get screen dimensions
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenX = size.x;
        screenY = size.y;
        
        // Vypočítame veľkosť bunky tak, aby sa bludisko zmestilo na obrazovku
        float mazeWidth = 19; // počet stĺpcov v bludisku
        float mazeHeight = 21; // počet riadkov v bludisku
        float cellSizeX = screenX / mazeWidth;
        float cellSizeY = screenY / mazeHeight;
        scaleFactor = Math.min(cellSizeX, cellSizeY);
        
        // Initialize game objects with scaled dimensions
        maze = new Maze(scaleFactor);
        
        // Umiestni Pacmana na štartovaciu pozíciu (stred spodnej časti)
        float startX = (maze.getColumns() / 2) * scaleFactor;
        float startY = (maze.getRows() - 2) * scaleFactor;
        pacman = new PacMan(startX, startY, scaleFactor / 2);
        pacman.setMaxX(maze.getColumns() * scaleFactor);

        // V konštruktore inicializujte zvuky
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            
            soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();
        } else {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        }

        // Načítajte zvuky
        eatDotSound = soundPool.load(context, R.raw.eat_dot, 1);
        eatGhostSound = soundPool.load(context, R.raw.eat_ghost, 1);
        gameStartSound = soundPool.load(context, R.raw.sound, 1);
        deathSound = soundPool.load(context, R.raw.death, 1);

        // Inicializácia duchov
        ghosts = new ArrayList<>();
        ghosts.add(new Ghost(context, 5 * scaleFactor, 5 * scaleFactor, scaleFactor, 0)); // Red ghost
        ghosts.add(new Ghost(context, 10 * scaleFactor, 5 * scaleFactor, scaleFactor, 1)); // Pink ghost
        ghosts.add(new Ghost(context, 5 * scaleFactor, 10 * scaleFactor, scaleFactor, 2)); // Blue ghost
        ghosts.add(new Ghost(context, 10 * scaleFactor, 10 * scaleFactor, scaleFactor, 3)); // Orange ghost
        
        // Načítaj zvuk pre zjedenie ovocia
        eatFruitSound = soundPool.load(context, R.raw.eat_fruit, 1);
        
        // Inicializácia ovocia s pozíciami mimo modrých blokov
        fruits = new ArrayList<>();
        float[][] fruitPositions = {
            {3, 3},           // Ľavý horný roh, ďalej od steny
            {maze.getColumns()-4, 3},  // Pravý horný roh, ďalej od steny
            {3, maze.getRows()-4},     // Ľavý dolný roh, ďalej od steny
            {maze.getColumns()-4, maze.getRows()-4}  // Pravý dolný roh, ďalej od steny
        };
        
        for (int i = 0; i < FRUIT_COUNT; i++) {
            float fruitX = fruitPositions[i][0] * scaleFactor;
            float fruitY = fruitPositions[i][1] * scaleFactor;
            fruits.add(new Fruit(context, fruitX, fruitY, scaleFactor, i));
        }

        // Inicializácia game over textu
        gameOverPaint = new Paint();
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(100);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);

        // Inicializácia lives paint
        livesPaint = new Paint();
        livesPaint.setColor(Color.WHITE);
        livesPaint.setTextSize(50);
        livesPaint.setTextAlign(Paint.Align.RIGHT);

        // Inicializácia paint pre boost message
        boostMessagePaint = new Paint();
        boostMessagePaint.setColor(Color.YELLOW);
        boostMessagePaint.setTextSize(40);
        boostMessagePaint.setTextAlign(Paint.Align.CENTER);
        boostMessagePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        // Inicializácia paint pre timer
        boostTimerPaint = new Paint();
        boostTimerPaint.setColor(Color.WHITE);
        boostTimerPaint.setTextSize(30);
        boostTimerPaint.setTextAlign(Paint.Align.CENTER);
        boostTimerPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        // Načítaj aktuálny level
        currentLevel = preferences.getInt("current_level", 1);
        
        // Nastav rýchlosť duchov podľa levelu
        for (Ghost ghost : ghosts) {
            ghost.setSpeedMultiplier(1.0f + (currentLevel - 1) * GHOST_SPEED_INCREASE);
        }
    }

    @Override
    public void run() {
        while (playing) {
            if (!isPaused) {
                long startFrameTime = System.currentTimeMillis();
                update();
                draw();
                long timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = MILLIS_IN_SECOND / timeThisFrame;
                }
            }
        }
    }

    private void update() {
        if (!isGameOver && !isPaused) {
            pacman.update(maze);
            updateGhosts(); // Ensure this is called to update ghost movement
            
            // Kontrola kolízie so stenou
            if (maze.isWall(pacman.getX(), pacman.getY())) {
                pacman.setDirection((pacman.getDirection() + 2) % 4);
                pacman.update(maze); // Revert the move if it hits a wall
                return;
            }

            // Check for victory
            if (maze.areAllDotsCollected()) {
                showVictoryMessage();
                return;
            }

            // Aktualizácia power mode
            if (powerMode && System.currentTimeMillis() - powerModeTime > POWER_MODE_DURATION) {
                powerMode = false;
            }

            // Kontrola portálov
            if (pacman.getX() < 0) {
                pacman.setPosition(maze.getColumns() * scaleFactor, pacman.getY());
            } else if (pacman.getX() > maze.getColumns() * scaleFactor) {
                pacman.setPosition(0, pacman.getY());
            }

            // Kontrola kolízie s duchmi
            for (Ghost ghost : ghosts) {
                ghost.update(maze, pacman);
                if (RectF.intersects(ghost.getBounds(), pacman.getBounds())) {
                    if (powerMode) {
                        // Pacman môže zjesť ducha
                        score += GHOST_POINTS * (doublePoints ? 2 : 1);
                        ghost.resetPosition();
                        if (preferences.getBoolean(SOUND_ENABLED, true)) {
                            soundPool.play(eatGhostSound, 1, 1, 0, 0, 1);
                        }
                    } else {
                        lives--;
                        if (lives <= 0) {
                            gameOver();
                        } else {
                            resetPositions();
                            if (preferences.getBoolean(SOUND_ENABLED, true)) {
                                soundPool.play(deathSound, 1, 1, 0, 0, 1);
                            }
                        }
                        return;
                    }
                }
            }

            // Zbieranie bodov a power pellets
            int dotType = maze.getDot(pacman.getX(), pacman.getY());
            if (dotType == 2) { // Normal dot
                score += DOT_POINTS;
                if (preferences.getBoolean(SOUND_ENABLED, true)) {
                    soundPool.play(eatDotSound, 1, 1, 0, 0, 1);
                }
            } else if (dotType == 3) { // Power pellet
                score += POWER_PELLET_POINTS;
                powerMode = true;
                powerModeTime = System.currentTimeMillis();
                if (preferences.getBoolean(SOUND_ENABLED, true)) {
                    soundPool.play(eatGhostSound, 1, 1, 0, 0, 1);
                }
            }

            // Kontrola kolízie s ovocím
            checkFruitCollision();

            // Kontrola či sú všetky bodky zjedené
            boolean allDotsCollected = true;
            for (int i = 0; i < maze.getRows(); i++) {
                for (int j = 0; j < maze.getColumns(); j++) {
                    if (maze.getDotType(i, j) == 2 || maze.getDotType(i, j) == 3) {
                        allDotsCollected = false;
                        break;
                    }
                }
                if (!allDotsCollected) break;
            }

            if (allDotsCollected && !isLevelComplete) {
                levelComplete();
            }

            // Kontrola trvania boostov
            if (speedBoost && System.currentTimeMillis() - speedBoostTime > BOOST_DURATION) {
                speedBoost = false;
                pacman.setSpeedMultiplier(1.0f);
            }
            if (doublePoints && System.currentTimeMillis() - doublePointsTime > BOOST_DURATION) {
                doublePoints = false;
            }
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            canvas = holder.lockCanvas();
            canvas.drawColor(Color.BLACK);
            
            float offsetX = (screenX - (maze.getColumns() * scaleFactor)) / 2;
            float offsetY = (screenY - (maze.getRows() * scaleFactor)) / 2;
            
            canvas.save();
            canvas.translate(offsetX, offsetY);
            
            // Vykresli bludisko
            maze.draw(canvas);
            
            // Vykresli ovocie
            for (Fruit fruit : fruits) {
                fruit.draw(canvas);
            }
            
            // Vykresli duchov
            for (Ghost ghost : ghosts) {
                if (powerMode) {
                    // Pridaj blikanie alebo zmenu farby duchov počas power mode
                    canvas.save();
                    canvas.scale(0.8f, 0.8f, ghost.getX(), ghost.getY());
                    ghost.draw(canvas);
                    canvas.restore();
                } else {
                    ghost.draw(canvas);
                }
            }
            
            // Vykresli Pacmana - farba sa mení len pri zbieraní ovocia
            pacman.draw(canvas);
            
            canvas.restore();
            
            // Vykresli skóre
            canvas.drawText("Score: " + score, 20, 60, scorePaint);
            canvas.drawText("Lives: " + lives, screenX - 20, 60, livesPaint);
            
            // Ak je hra skončená
            if (isGameOver) {
                String message = allCollected ? "YOU WIN!" : "GAME OVER";
                canvas.drawText(message, (float) screenX /2, (float) screenY /2, gameOverPaint);
                canvas.drawText("Score: " + score, (float) screenX /2, (float) screenY /2 + 100, gameOverPaint);
            }
            
            // Vykresli boost message a timer ak je aktívny
            if (System.currentTimeMillis() - boostMessageTime < MESSAGE_DURATION) {
                float messageY = maze.getRows() * scaleFactor * 0.2f; // 20% od vrchu bludiska
                canvas.drawText(boostMessage, screenX / 2f, messageY, boostMessagePaint);
                
                // Vypočítaj zostávajúci čas pre aktívny boost
                String timeLeft = "";
                if (speedBoost) {
                    long remaining = (BOOST_DURATION - (System.currentTimeMillis() - speedBoostTime)) / 1000;
                    if (remaining > 0) timeLeft = remaining + "s";
                } else if (powerMode) {
                    long remaining = (POWER_MODE_DURATION - (System.currentTimeMillis() - powerModeTime)) / 1000;
                    if (remaining > 0) timeLeft = remaining + "s";
                } else if (doublePoints) {
                    long remaining = (BOOST_DURATION - (System.currentTimeMillis() - doublePointsTime)) / 1000;
                    if (remaining > 0) timeLeft = remaining + "s";
                }
                
                if (!timeLeft.isEmpty()) {
                    canvas.drawText(timeLeft, screenX / 2f, messageY + 40, boostTimerPaint);
                }
            }
            
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - touchX;
                float dy = event.getY() - touchY;
                
                // Determine direction based on swipe
                if (Math.abs(dx) > Math.abs(dy)) {
                    setPacmanDirection(dx > 0 ? 0 : 2); // Right or Left
                } else {
                    setPacmanDirection(dy > 0 ? 1 : 3); // Down or Up
                }
                break;
        }
        return true;
    }

    public void pause() {
        isPaused = true;
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            // Log error
        }
    }

    public void resume() {
        isPaused = false;
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void destroy() {
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void setPacmanDirection(int direction) {
        if (pacman != null) {
            // Skontrolujeme, či v novom smere nie je stena
            float oldX = pacman.getX();
            float oldY = pacman.getY();
            
            pacman.setDirection(direction);
            pacman.update();
            
            // Ak je v novom smere stena, vrátime pôvodnú pozíciu a smer
            if (maze.isWall(pacman.getX(), pacman.getY())) {
                pacman.setDirection((direction + 2) % 4); // Vrátime opačný smer
                pacman.update(); // Vrátime na pôvodnú pozíciu
            }
        }
    }

    public void saveState(Bundle outState) {
        outState.putInt("score", score);
        outState.putBoolean("powerMode", powerMode);
        outState.putLong("powerModeTime", powerModeTime);
        outState.putBoolean("isGameOver", isGameOver);
        outState.putBoolean("allCollected", allCollected);
        
        // Uloženie pozície a smeru Pacmana
        outState.putFloat("pacmanX", pacman.getX());
        outState.putFloat("pacmanY", pacman.getY());
        outState.putInt("pacmanDirection", pacman.getDirection());
        
        // Uloženie pozícií duchov
        float[] ghostPositions = new float[ghosts.size() * 2];
        for (int i = 0; i < ghosts.size(); i++) {
            ghostPositions[i * 2] = ghosts.get(i).getX();
            ghostPositions[i * 2 + 1] = ghosts.get(i).getY();
        }
        outState.putFloatArray("ghostPositions", ghostPositions);
        
        // Uloženie viditeľnosti ovocia
        boolean[] fruitVisibility = new boolean[fruits.size()];
        for (int i = 0; i < fruits.size(); i++) {
            fruitVisibility[i] = fruits.get(i).isVisible();
        }
        outState.putBooleanArray("fruitVisibility", fruitVisibility);
        
        // Uloženie stavu bodiek v bludisku
        int[][] dotState = new int[maze.getRows()][maze.getColumns()];
        for (int i = 0; i < maze.getRows(); i++) {
            for (int j = 0; j < maze.getColumns(); j++) {
                dotState[i][j] = maze.getDotType(i, j);
            }
        }
        outState.putSerializable("dotState", dotState);
        outState.putInt("lives", lives);
    }

    public void restoreState(Bundle savedState) {
        if (savedState != null) {
            score = savedState.getInt("score", 0);
            powerMode = savedState.getBoolean("powerMode", false);
            powerModeTime = savedState.getLong("powerModeTime", 0);
            isGameOver = savedState.getBoolean("isGameOver", false);
            allCollected = savedState.getBoolean("allCollected", false);
            
            // Obnovenie pozície a smeru Pacmana
            float pacmanX = savedState.getFloat("pacmanX", pacman.getX());
            float pacmanY = savedState.getFloat("pacmanY", pacman.getY());
            int pacmanDirection = savedState.getInt("pacmanDirection", 0);
            pacman.setPosition(pacmanX, pacmanY);
            pacman.setDirection(pacmanDirection);
            
            // Obnovenie pozícií duchov
            float[] ghostPositions = savedState.getFloatArray("ghostPositions");
            if (ghostPositions != null) {
                for (int i = 0; i < ghosts.size(); i++) {
                    ghosts.get(i).setPosition(
                        ghostPositions[i * 2],
                        ghostPositions[i * 2 + 1]
                    );
                }
            }
            
            // Obnovenie viditeľnosti ovocia
            boolean[] fruitVisibility = savedState.getBooleanArray("fruitVisibility");
            if (fruitVisibility != null) {
                for (int i = 0; i < fruits.size(); i++) {
                    fruits.get(i).setVisible(fruitVisibility[i]);
                }
            }
            
            // Obnovenie stavu bodiek v bludisku
            int[][] dotState = (int[][]) savedState.getSerializable("dotState");
            if (dotState != null) {
                maze.restoreDotState(dotState);
            }
            lives = savedState.getInt("lives", 3);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        screenX = w;
        screenY = h;
        float mazeWidth = maze.getColumns();
        float mazeHeight = maze.getRows();
        float cellSizeX = screenX / mazeWidth;
        float cellSizeY = screenY / mazeHeight;
        scaleFactor = Math.min(cellSizeX, cellSizeY);
        
        // Aktualizácia maximálnej X súradnice pre Pacmana
        if (pacman != null) {
            pacman.setMaxX(maze.getColumns() * scaleFactor);
        }
        
        updateGameObjectsPositions();
    }

    private void updateGameObjectsPositions() {
        // Aktualizácia pozícií všetkých objektov podľa nového scaleFactor
        if (pacman != null) {
            pacman.updateScale(scaleFactor);
        }
        
        if (ghosts != null) {
            for (Ghost ghost : ghosts) {
                ghost.updateScale(scaleFactor);
            }
        }
        
        if (fruits != null) {
            for (Fruit fruit : fruits) {
                fruit.updateScale(scaleFactor);
            }
        }
        
        if (maze != null) {
            maze.updateScale(scaleFactor);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Nepozastavujeme hru, len prepočítame veľkosti
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenX = size.x;
        screenY = size.y;
        
        float mazeWidth = maze.getColumns();
        float mazeHeight = maze.getRows();
        float cellSizeX = screenX / mazeWidth;
        float cellSizeY = screenY / mazeHeight;
        scaleFactor = Math.min(cellSizeX, cellSizeY);
        
        updateGameObjectsPositions();
    }

    private void checkFruitCollision() {
        for (Fruit fruit : fruits) {
            if (fruit.isVisible() && RectF.intersects(pacman.getBounds(), fruit.getBounds())) {
                // Základné skóre
                int basePoints = fruit.getPoints();
                score += doublePoints ? basePoints * 2 : basePoints;
                
                // Aplikuj boost efekt
                switch (fruit.getBoostType()) {
                    case 0: // Speed boost
                        speedBoost = true;
                        speedBoostTime = System.currentTimeMillis();
                        pacman.setSpeedMultiplier(SPEED_BOOST_MULTIPLIER);
                        break;
                    case 1: // Invincibility (použije existujúci power mode)
                        powerMode = true;
                        powerModeTime = System.currentTimeMillis();
                        break;
                    case 2: // Extra life
                        lives++;
                        break;
                    case 3: // Double points
                        doublePoints = true;
                        doublePointsTime = System.currentTimeMillis();
                        break;
                }
                
                fruit.setVisible(false);
                if (preferences.getBoolean(SOUND_ENABLED, true)) {
                    soundPool.play(eatFruitSound, 1, 1, 0, 0, 1);
                }
                
                // Nastav správu podľa typu boostu
                switch (fruit.getBoostType()) {
                    case 0:
                        boostMessage = "SPEED BOOST!";
                        break;
                    case 1:
                        boostMessage = "INVINCIBILITY!";
                        break;
                    case 2:
                        boostMessage = "EXTRA LIFE!";
                        break;
                    case 3:
                        boostMessage = "DOUBLE POINTS!";
                        break;
                }
                boostMessageTime = System.currentTimeMillis();
            }
        }
    }

    private void resetPositions() {
        // Reset pozície Pacmana na štartovaciu pozíciu
        float startX = (maze.getColumns() / 2) * scaleFactor;
        float startY = (maze.getRows() - 2) * scaleFactor;
        pacman.setPosition(startX, startY);
        
        // Reset pozícií duchov
        for (Ghost ghost : ghosts) {
            ghost.resetPosition();
        }
    }

    private void levelComplete() {
        isLevelComplete = true;
        currentLevel++;
        
        // Ulož progres
        preferences.edit()
            .putInt("current_level", currentLevel)
            .putInt("total_score", score)
            .apply();

        // Zvýš rýchlosť duchov
        for (Ghost ghost : ghosts) {
            ghost.setSpeedMultiplier(1.0f + (currentLevel - 1) * GHOST_SPEED_INCREASE);
        }

        // Reset pozícií a stavov
        resetLevel();
    }

    private void resetLevel() {
        // Reset pozícií
        resetPositions();
        
        // Reset maze (obnoví všetky bodky)
        maze.resetDots();
        
        // Reset stavov
        powerMode = false;
        isLevelComplete = false;
        
        // Ostatné resety zostávajú rovnaké
    }

    private void gameOver() {
        isGameOver = true;
        if (preferences.getBoolean(SOUND_ENABLED, true)) {
            soundPool.play(deathSound, 1, 1, 0, 0, 1);
        }
        
        // Ulož štatistiky
        int totalAttempts = preferences.getInt("total_attempts", 0) + 1;
        preferences.edit()
            .putInt("total_attempts", totalAttempts)
            .putInt("remaining_lives", lives)
            .apply();
    }

    public void startNewGame() {
        // Reset game state
        score = 0;
        lives = 3;
        resetPositions();
        maze.resetDots();
        isGameOver = false;
        isLevelComplete = false;
        // Save that there's no saved game state
        getContext().getSharedPreferences("PacManPrefs", Context.MODE_PRIVATE)
            .edit().putBoolean("hasSavedGame", false).apply();
    }

    public void continueGame() {
        // Restore game state from preferences
        SharedPreferences prefs = getContext().getSharedPreferences("PacManPrefs", Context.MODE_PRIVATE);
        score = prefs.getInt("score", 0);
        lives = prefs.getInt("lives", 3);
        float pacmanX = prefs.getFloat("pacmanX", pacman.getX());
        float pacmanY = prefs.getFloat("pacmanY", pacman.getY());
        pacman.setPosition(pacmanX, pacmanY);

        // Restore ghost positions
        for (int i = 0; i < ghosts.size(); i++) {
            float ghostX = prefs.getFloat("ghostX" + i, ghosts.get(i).getX());
            float ghostY = prefs.getFloat("ghostY" + i, ghosts.get(i).getY());
            ghosts.get(i).setPosition(ghostX, ghostY);
        }

        // Restore collected dots
        int[][] dotState = new int[maze.getRows()][maze.getColumns()];
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                dotState[row][col] = prefs.getInt("dotState_" + row + "_" + col, maze.getDotType(row, col));
            }
        }
        maze.restoreDotState(dotState);
    }

    public void saveState() {
        // Save the current game state to preferences
        SharedPreferences.Editor editor = getContext().getSharedPreferences("PacManPrefs", Context.MODE_PRIVATE).edit();
        editor.putBoolean("hasSavedGame", true);
        editor.putInt("score", score);
        editor.putInt("lives", lives);
        editor.putFloat("pacmanX", pacman.getX());
        editor.putFloat("pacmanY", pacman.getY());

        // Save ghost positions
        for (int i = 0; i < ghosts.size(); i++) {
            editor.putFloat("ghostX" + i, ghosts.get(i).getX());
            editor.putFloat("ghostY" + i, ghosts.get(i).getY());
        }

        // Save collected dots
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                editor.putInt("dotState_" + row + "_" + col, maze.getDotType(row, col));
            }
        }

        editor.apply();
    }

    // Ensure ghost movement logic is correct
    private void updateGhosts() {
        for (Ghost ghost : ghosts) {
            ghost.update(maze, pacman);
        }
    }

    private void showVictoryMessage() {
        // Pause the game
        isPaused = true;

        // Show a victory message
        new AlertDialog.Builder(getContext())
            .setTitle("Congratulations!")
            .setMessage("You have collected all the dots and won the game!")
            .setPositiveButton("Return to Menu", (dialog, which) -> {
                // Return to the main menu
                Intent intent = new Intent(getContext(), MenuActivity.class);
                getContext().startActivity(intent);
            })
            .setCancelable(false)
            .show();
    }
} 