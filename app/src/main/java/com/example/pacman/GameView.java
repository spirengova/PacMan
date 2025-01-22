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
import android.os.Handler;

public class GameView extends SurfaceView implements Runnable {
    private Thread gameThread;
    private SurfaceHolder holder;
    private volatile boolean playing;
    private Canvas canvas;
    private Paint paint;
    private long fps;
    private final int MILLIS_IN_SECOND = 1000;
    private Context context;

    // Konštanty pre rozmery bludiska
    private static final int MAZE_WIDTH = 19;  // Šírka bludiska v bunkách
    private static final int MAZE_HEIGHT = 21; // Výška bludiska v bunkách
    private static final float MAP_TOP_MARGIN = 100; // Priestor pre tlačidlá

    private Maze maze;
    private PacMan pacman;
    private float touchX, touchY;
    private int screenX, screenY;
    private float scaleFactor;
    private SoundPool soundPool;
    private int eatDotSound;
    private MediaPlayer backgroundMusic;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "PacManPrefs";
    private static final String SOUND_ENABLED = "sound_enabled";
    private int score = 0;
    private Paint scorePaint;
    private static final int DOT_POINTS = 10;
    private static final int POWER_PELLET_POINTS = 30;  // Body za power pellet
    private boolean isGameOver = false;
    private Paint gameOverPaint;
    private boolean isPaused = false;
    private boolean isLevelComplete = false;
    private int currentLevel = 1;  // Pridáme premennú pre level
    private ArrayList<Ghost> ghosts;  // Pole pre všetkých duchov
    private Handler handler;
    private float mapOffsetX;
    private float mapOffsetY;
    private Paint buttonPaint;
    private RectF pauseButtonBounds;
    private RectF exitButtonBounds;
    private static final float BUTTON_MARGIN = 20;
    private static final float BUTTON_HEIGHT = 80;
    private static final float BUTTON_RADIUS = 20f;
    private static final float BUTTON_WIDTH = 200f;
    private static final int BUTTON_COLOR = Color.rgb(33, 150, 243);
    private static final int BUTTON_TEXT_COLOR = Color.WHITE;
    private static final float BUTTON_TEXT_SIZE = 40f;
    private static final float BUTTON_SPACING = 20f;
    private float touchStartX, touchStartY;  // Pridané pre sledovanie swipe
    private static final float SWIPE_THRESHOLD = 50;  // Minimálna vzdialenosť pre swipe

    public GameView(Context context, int screenX, int screenY) {
        super(context);
        this.context = context;
        this.screenX = screenX;
        this.screenY = screenY;

        holder = getHolder();
        paint = new Paint();

        // Výpočet mierky s ohľadom na hornú medzeru
        float scaleX = screenX / (float)    MAZE_WIDTH;
        float scaleY = (screenY - MAP_TOP_MARGIN) / (float) MAZE_HEIGHT;
        scaleFactor = Math.min(scaleX, scaleY) * 0.95f;

        // Inicializácia herných objektov
        maze = new Maze(scaleFactor);
        pacman = new PacMan(9, 15, scaleFactor);  // Začiatočná pozícia Pac-Mana

        // Inicializácia duchov
        initializeGhosts();

        // Inicializácia zvukov
        initializeSounds();

        // Načítaj nastavenia
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Inicializácia textov
        initializeTextPaints();

        // Vypočítaj offset pre centrovanie
        calculateMapOffset();

        // Inicializácia tlačidiel
        initializeButtons();

        handler = new Handler();

        startGame();
    }

    private void initializeGhosts() {
        ghosts = new ArrayList<>();
        // Vytvor troch duchov s rôznymi farbami
        ghosts.add(new Ghost(context, 8, 9, scaleFactor, 0));  // Červený
        ghosts.add(new Ghost(context, 9, 9, scaleFactor, 1));  // Ružový
        ghosts.add(new Ghost(context, 10, 9, scaleFactor, 2)); // Oranžový
    }

    private void initializeSounds() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(5)
                        .setAudioAttributes(audioAttributes)
                        .build();
            } else {
                soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            }

            // Načítaj zvuky
            try {
                eatDotSound = soundPool.load(context, R.raw.eat_dot, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeTextPaints() {
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(50);
        scorePaint.setTextAlign(Paint.Align.LEFT);

        gameOverPaint = new Paint();
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(100);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initializeButtons() {
        buttonPaint = new Paint();
        buttonPaint.setColor(BUTTON_COLOR);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setAntiAlias(true);
        buttonPaint.setTextSize(BUTTON_TEXT_SIZE);
        buttonPaint.setTextAlign(Paint.Align.CENTER);
        buttonPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Oba tlačidlá na pravej strane
        exitButtonBounds = new RectF(
                screenX - BUTTON_MARGIN - BUTTON_WIDTH,
                BUTTON_MARGIN,
                screenX - BUTTON_MARGIN,
                BUTTON_MARGIN + BUTTON_HEIGHT
        );

        pauseButtonBounds = new RectF(
                screenX - 2 * BUTTON_MARGIN - 2 * BUTTON_WIDTH,
                BUTTON_MARGIN,
                screenX - 2 * BUTTON_MARGIN - BUTTON_WIDTH,
                BUTTON_MARGIN + BUTTON_HEIGHT
        );
    }

    private void startGame() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (playing) {
            long startFrameTime = System.currentTimeMillis();

            if (!isPaused) {  // Odstránená kontrola isGameOver, aby sa Pacman mohol hýbať
                update();
            }

            draw();

            long timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame > 0) {
                fps = MILLIS_IN_SECOND / timeThisFrame;
            }
        }
    }

    private void update() {
        if (!isGameOver && !isPaused) {
            pacman.update(maze);

            // Konvertuj ArrayList<Ghost> na Ghost[]
            Ghost[] ghostArray = ghosts.toArray(new Ghost[0]);

            // Aktualizuj duchov s konvertovaným poľom
            for (Ghost ghost : ghosts) {
                ghost.update(maze, pacman, ghostArray);
            }

            // Kontrola kolízie s duchmi
            for (Ghost ghost : ghosts) {
                if (RectF.intersects(pacman.getBounds(), ghost.getBounds())) {
                    handleGhostCollision(ghost);
                }
            }

            // Kontrola kolízie s bodkami
            int dotType = maze.getDot(pacman.getGridX(), pacman.getGridY());
            if (dotType > 0) {
                handleDotCollection(dotType);
            }

            // Kontrola víťazstva
            if (maze.areAllDotsCollected() && !isLevelComplete) {
                isLevelComplete = true;
                levelComplete();
            }
        }
    }

    private void handleDotCollection(int dotType) {
        maze.removeDot(pacman.getGridX(), pacman.getGridY());
        if (dotType == Maze.POWER_PELLET) {
            // Power Pellet - urob duchov zraniteľných
            for (Ghost ghost : ghosts) {
                ghost.makeVulnerable();
            }
            score += POWER_PELLET_POINTS;
        } else {
            score += DOT_POINTS;
        }
        playEatSound();
    }

    private void handleGhostCollision(Ghost ghost) {
        if (!ghost.isVulnerable()) {
            gameOver();
        } else {
            ghost.handleEaten();
            score += 200;
            playGhostEatenSound();
        }
    }

    private void drawScore() {
        if (canvas != null) {
            canvas.drawText("Score: " + score, 50, 50, scorePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Kontrola tlačidiel
                if (pauseButtonBounds.contains(x, y)) {
                    if (isPaused) {
                        resume();
                    } else {
                        pause();
                    }
                    return true;
                }

                if (exitButtonBounds.contains(x, y)) {
                    ((MainActivity)context).finish();
                    return true;
                }

                // Ulož počiatočnú pozíciu pre swipe
                touchStartX = x;
                touchStartY = y;
                return true;

            case MotionEvent.ACTION_UP:
                float deltaX = x - touchStartX;
                float deltaY = y - touchStartY;

                // Zisti, či je to swipe (dostatočná vzdialenosť)
                if (Math.abs(deltaX) > SWIPE_THRESHOLD || Math.abs(deltaY) > SWIPE_THRESHOLD) {
                    // Zisti smer swipu
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        // Horizontálny swipe
                        if (deltaX > 0) {
                            setPacmanDirection(0);  // Vpravo
                        } else {
                            setPacmanDirection(2);  // Vľavo
                        }
                    } else {
                        // Vertikálny swipe
                        if (deltaY > 0) {
                            setPacmanDirection(1);  // Dole
                        } else {
                            setPacmanDirection(3);  // Hore
                        }
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void pause() {
        playing = false;
        isPaused = true;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        playing = true;
        isPaused = false;
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
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
            pacman.setDirection(direction, maze);
        }
    }

    public void saveState(Bundle outState) {
        outState.putInt("pacmanGridX", pacman.getGridX());
        outState.putInt("pacmanGridY", pacman.getGridY());
        outState.putInt("pacmanDirection", pacman.getDirection());
        // ... zvyšok kódu ...
    }

    public void restoreState(Bundle savedState) {
        if (savedState != null) {
            int pacmanGridX = savedState.getInt("pacmanGridX", pacman.getGridX());
            int pacmanGridY = savedState.getInt("pacmanGridY", pacman.getGridY());
            pacman.setPosition(pacmanGridX, pacmanGridY);
            // ... zvyšok kódu ...
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

        updateGameObjectsPositions();
    }

    private void updateGameObjectsPositions() {
        // Aktualizácia pozícií všetkých objektov podľa nového scaleFactor
        if (pacman != null) {
            pacman.updateScale(scaleFactor);
        }

        if (maze != null) {
            maze.updateScale(scaleFactor);
        }

        // Aktualizácia mierky pre všetkých duchov
        for (Ghost ghost : ghosts) {
            ghost.updateScale(scaleFactor);
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

    private void levelComplete() {
        isLevelComplete = true;
        currentLevel++;

        // Uložíme aktuálny level
        getContext().getSharedPreferences("PacManPrefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("current_level", currentLevel)
                .apply();

        // Zobrazíme dialóg o dokončení levelu
        new AlertDialog.Builder(getContext())
                .setTitle("Level Complete!")
                .setMessage("Score: " + score)
                .setPositiveButton("Next Level", (dialog, which) -> {
                    startNextLevel();
                })
                .setCancelable(false)
                .show();
    }

    private void startNextLevel() {
        // Reset pozícií a bodiek, ale zachovaj skóre
        resetPositions();
        maze.resetDots();
        isLevelComplete = false;
    }

    private void resetLevel() {
        // Reset pozícií
        resetPositions();

        // Reset maze (obnoví všetky bodky)
        maze.resetDots();

        // Reset stavov
        isLevelComplete = false;

        // Ostatné resety zostávajú rovnaké
    }

    private void gameOver() {
        isGameOver = true;
        isPaused = true;

        // Ulož high score ak je vyššie ako doterajšie
        int highScore = preferences.getInt("high_score", 0);
        if (score > highScore) {
            preferences.edit().putInt("high_score", score).apply();
        }

        // Zobraz game over správu
        new AlertDialog.Builder(getContext())
                .setTitle("Game Over")
                .setMessage("Your score: " + score)
                .setPositiveButton("Return to Menu", (dialog, which) -> {
                    // Návrat do menu
                    Intent intent = new Intent(getContext(), MenuActivity.class);
                    getContext().startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }

    public void startNewGame() {
        // Vymaž uložené dáta
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Resetuj stav hry
        score = 0;
        currentLevel = 1;
        isGameOver = false;
        isPaused = false;
        isLevelComplete = false;

        // Inicializuj bludisko a postavy
        resetPositions();
        maze.resetDots();
        resetGhosts();

        // Spusti hru
        resume();
    }

    public void continueGame() {
        // Načítaj základné informácie o hre
        score = preferences.getInt("score", 0);
        isGameOver = preferences.getBoolean("isGameOver", false);
        isPaused = preferences.getBoolean("isPaused", false);
        currentLevel = preferences.getInt("currentLevel", 1);

        // Načítaj pozíciu Pacmana
        int pacmanGridX = preferences.getInt("pacmanGridX", maze.getColumns() / 2);
        int pacmanGridY = preferences.getInt("pacmanGridY", maze.getRows() - 2);
        int pacmanDirection = preferences.getInt("pacmanDirection", -1);

        // Vytvor pole pre uložený stav bodiek
        int[][] savedDots = new int[maze.getRows()][maze.getColumns()];

        // Načítaj stav bodiek
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                savedDots[row][col] = preferences.getInt("dotState_" + row + "_" + col, 0);
            }
        }

        // Obnov stav bodiek pomocou novej metódy
        maze.restoreDotState(savedDots);

        // Nastav pozíciu Pacmana
        pacman.setPosition(pacmanGridX, pacmanGridY);
        pacman.setDirection(pacmanDirection, maze);

        // Resetuj duchov na ich počiatočné pozície
        resetGhosts();

        // Spusti hru
        resume();
    }

    public void saveState() {
        SharedPreferences.Editor editor = preferences.edit();

        // Ulož základné informácie o hre
        editor.putInt("score", score);
        editor.putBoolean("isGameOver", isGameOver);
        editor.putBoolean("isPaused", isPaused);
        editor.putInt("currentLevel", currentLevel);

        // Ulož pozíciu Pacmana
        editor.putInt("pacmanGridX", pacman.getGridX());
        editor.putInt("pacmanGridY", pacman.getGridY());
        editor.putInt("pacmanDirection", pacman.getDirection());

        // Ulož stav bodiek a stien
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                editor.putInt("dotState_" + row + "_" + col, maze.getDot(col, row));
                editor.putInt("wallState_" + row + "_" + col, maze.getWall(col, row));
            }
        }

        editor.apply();
    }

    private void resetPositions() {
        pacman.setPosition(maze.getColumns() / 2, maze.getRows() - 2);

        // Reset pozícií všetkých duchov
        for (Ghost ghost : ghosts) {
            ghost.resetPosition();
        }
    }

    private void updateScale() {
        pacman.updateScale(scaleFactor);

        // Aktualizácia mierky pre všetkých duchov
        for (Ghost ghost : ghosts) {
            ghost.updateScale(scaleFactor);
        }
    }

    private void drawGameOver() {
        if (canvas != null) {
            String text = "GAME OVER";
            canvas.drawText(text, screenX/2, screenY/2, gameOverPaint);
        }
    }

    private void calculateMapOffset() {
        float mapWidth = maze.getColumns() * scaleFactor;
        float mapHeight = maze.getRows() * scaleFactor;

        mapOffsetX = (screenX - mapWidth) / 2;
        mapOffsetY = ((screenY - MAP_TOP_MARGIN) - mapHeight) / 2 + MAP_TOP_MARGIN;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenX = width;
        screenY = height;

        // Prepočítaj mierku
        float scaleX = screenX / (float) maze.getColumns();
        float scaleY = screenY / (float) maze.getRows();
        scaleFactor = Math.min(scaleX, scaleY) * 0.95f;  // Nechaj malý okraj

        // Aktualizuj všetky objekty s novou mierkou
        maze.updateScale(scaleFactor);
        pacman.updateScale(scaleFactor);
        for (Ghost ghost : ghosts) {
            ghost.updateScale(scaleFactor);
        }

        // Vypočítaj offset pre centrovanie
        calculateMapOffset();
    }

    private void drawButtons(Canvas canvas) {
        // Vykresli pozadie tlačidiel so zaoblenými rohmi
        canvas.drawRoundRect(pauseButtonBounds, BUTTON_RADIUS, BUTTON_RADIUS, buttonPaint);
        canvas.drawRoundRect(exitButtonBounds, BUTTON_RADIUS, BUTTON_RADIUS, buttonPaint);

        // Nastavenie farby pre text
        buttonPaint.setColor(BUTTON_TEXT_COLOR);

        // Vykresli text tlačidiel
        float textY = BUTTON_MARGIN + BUTTON_HEIGHT/2 + BUTTON_TEXT_SIZE/3;
        canvas.drawText(isPaused ? "RESUME" : "PAUSE",
                pauseButtonBounds.centerX(),
                textY,
                buttonPaint);

        canvas.drawText("EXIT",
                exitButtonBounds.centerX(),
                textY,
                buttonPaint);

        // Reset farby pre ďalšie použitie
        buttonPaint.setColor(BUTTON_COLOR);
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.BLACK);
                    canvas.save();
                    canvas.translate(mapOffsetX, mapOffsetY);

                    maze.draw(canvas);
                    pacman.draw(canvas);
                    for (Ghost ghost : ghosts) {
                        ghost.draw(canvas);
                    }

                    canvas.restore();

                    // Vykresli len skóre a tlačidlá
                    drawScore();
                    drawButtons(canvas);

                    if (isGameOver) {
                        drawGameOver();
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    private void playEatSound() {
        if (preferences.getBoolean(SOUND_ENABLED, true)) {
            try {
                soundPool.play(eatDotSound, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playGhostEatenSound() {
        if (preferences.getBoolean(SOUND_ENABLED, true)) {
            try {
                soundPool.play(eatDotSound, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resetGhosts() {
        if (ghosts != null) {
            for (Ghost ghost : ghosts) {
                ghost.resetPosition();
                ghost.resetState();
            }
        }
    }
}