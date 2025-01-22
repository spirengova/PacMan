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
import java.util.Iterator;

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
    private List<Fruit> fruits;
    private static final int MAX_FRUITS = 2;
    private static final long FRUIT_SPAWN_INTERVAL = 10000; // 10 sekúnd
    private long lastFruitSpawnTime;
    private boolean hasSpeedBoost = false;
    private boolean hasInvincibility = false;
    private boolean hasDoublePoints = false;
    private long boostEndTime = 0;
    private Paint boostMessagePaint;
    private String currentBoostMessage = "";
    private long boostMessageEndTime = 0;
    private static final long MESSAGE_DURATION = 2000; // 2 sekundy
    private int lives = 3; // Počiatočný počet životov

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

        // Načítaj nastavenia
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Inicializácia textov
        initializeTextPaints();

        // Vypočítaj offset pre centrovanie
        calculateMapOffset();

        // Inicializácia tlačidiel
        initializeButtons();

        handler = new Handler();

        fruits = new ArrayList<>();
        lastFruitSpawnTime = System.currentTimeMillis();

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
            // Inicializácia hudby na pozadí
            backgroundMusic = MediaPlayer.create(context, R.raw.sound);
            backgroundMusic.setLooping(true);
            if (preferences.getBoolean(SOUND_ENABLED, true)) {
                backgroundMusic.start();
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

        boostMessagePaint = new Paint();
        boostMessagePaint.setColor(Color.GREEN);
        boostMessagePaint.setTextSize(40);
        boostMessagePaint.setTextAlign(Paint.Align.CENTER);
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
        lives = 3; // Reset životov pri novej hre
        score = 0; // Reset skóre
        isGameOver = false;
        isPaused = false;
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

            // Update fruits
            updateFruits();
            
            // Kontrola kolízie s ovocím
            checkFruitCollisions();
            
            // Kontrola boostov
            checkBoosts();
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
    }

    private void handleGhostCollision(Ghost ghost) {
        if (!ghost.isVulnerable() && !hasInvincibility) {
            lives--;
            if (lives <= 0) {
                isGameOver = true;
                isPaused = true;
            } else {
                resetPositions();
            }
        } else {
            ghost.handleEaten();
            score += hasDoublePoints ? 400 : 200;
        }
    }

    private void drawScore() {
        String scoreText = "Score: " + score;
        canvas.drawText(scoreText, 20, 50, scorePaint);
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
            String gameOverText = "GAME OVER";
            String scoreText = "Final Score: " + score;
            
            // Vykresli GAME OVER
            canvas.drawText(gameOverText, screenX/2, screenY/2 - 60, gameOverPaint);
            
            // Vykresli skóre pod GAME OVER
            canvas.drawText(scoreText, screenX/2, screenY/2 + 60, gameOverPaint);
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
                    
                    // Draw fruits
                    for (Fruit fruit : fruits) {
                        fruit.draw(canvas);
                    }
                    
                    pacman.draw(canvas);
                    for (Ghost ghost : ghosts) {
                        ghost.draw(canvas);
                    }

                    canvas.restore();

                    drawScore();
                    drawLives();
                    drawButtons(canvas);
                    
                    // Vykresli správu o booste ak je aktívna
                    if (System.currentTimeMillis() < boostMessageEndTime) {
                        canvas.drawText(currentBoostMessage, screenX/2, screenY - 100, boostMessagePaint);
                    }

                    if (isGameOver) {
                        drawGameOver();
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
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

    private void updateFruits() {
        long currentTime = System.currentTimeMillis();
        
        // Spawn new fruit if needed
        if (currentTime - lastFruitSpawnTime > FRUIT_SPAWN_INTERVAL && fruits.size() < MAX_FRUITS) {
            spawnNewFruit();
            lastFruitSpawnTime = currentTime;
        }
    }

    private void spawnNewFruit() {
        int attempts = 0;
        int maxAttempts = 20;  // Zvýšený počet pokusov
        
        while (attempts < maxAttempts) {
            int x = 1 + (int)(Math.random() * (maze.getColumns() - 2));
            int y = 1 + (int)(Math.random() * (maze.getRows() - 2));
            
            // Kontrola či je políčko dostupné pre Pacmana
            if (!maze.isWall(x, y) && isAccessibleFromPacman(x, y)) {
                int fruitType = (int)(Math.random() * 4);
                float pixelX = x * scaleFactor + scaleFactor/2;
                float pixelY = y * scaleFactor + scaleFactor/2;
                
                fruits.add(new Fruit(context, pixelX, pixelY, scaleFactor, fruitType));
                break;
            }
            attempts++;
        }
    }

    private boolean isAccessibleFromPacman(int x, int y) {
        // Jednoduchý flood fill algoritmus pre kontrolu dostupnosti
        boolean[][] visited = new boolean[maze.getColumns()][maze.getRows()];
        return checkAccessibility(pacman.getGridX(), pacman.getGridY(), x, y, visited);
    }

    private boolean checkAccessibility(int startX, int startY, int targetX, int targetY, boolean[][] visited) {
        // Ak sme mimo bludiska alebo na stene alebo už navštívené, vráť false
        if (startX < 0 || startX >= maze.getColumns() || 
            startY < 0 || startY >= maze.getRows() ||
            maze.isWall(startX, startY) || visited[startX][startY]) {
            return false;
        }

        // Ak sme našli cieľ, vráť true
        if (startX == targetX && startY == targetY) {
            return true;
        }

        // Označ ako navštívené
        visited[startX][startY] = true;

        // Skontroluj všetky smery
        return checkAccessibility(startX + 1, startY, targetX, targetY, visited) ||
               checkAccessibility(startX - 1, startY, targetX, targetY, visited) ||
               checkAccessibility(startX, startY + 1, targetX, targetY, visited) ||
               checkAccessibility(startX, startY - 1, targetX, targetY, visited);
    }

    private void checkFruitCollisions() {
        Iterator<Fruit> iterator = fruits.iterator();
        while (iterator.hasNext()) {
            Fruit fruit = iterator.next();
            if (RectF.intersects(pacman.getBounds(), fruit.getBounds())) {
                // Aplikuj boost podľa typu ovocia
                applyFruitBoost(fruit);
                score += fruit.getPoints();
                iterator.remove();
            }
        }
    }

    private void applyFruitBoost(Fruit fruit) {
        long currentTime = System.currentTimeMillis();
        boostEndTime = currentTime + fruit.getBoostDuration();
        boostMessageEndTime = currentTime + MESSAGE_DURATION;
        
        switch (fruit.getBoostType()) {
            case 0: // Speed boost
                hasSpeedBoost = true;
                pacman.setSpeedMultiplier(1.5f);
                currentBoostMessage = "Speed Boost!";
                break;
            case 1: // Invincibility
                hasInvincibility = true;
                currentBoostMessage = "Invincibility!";
                break;
            case 2: // Extra life
                lives++;
                currentBoostMessage = "Extra Life!";
                break;
            case 3: // Double points
                hasDoublePoints = true;
                currentBoostMessage = "Double Points!";
                break;
        }
    }

    private void checkBoosts() {
        long currentTime = System.currentTimeMillis();
        if (currentTime > boostEndTime) {
            // Reset all boosts
            if (hasSpeedBoost) {
                hasSpeedBoost = false;
                pacman.setSpeedMultiplier(1.0f);
            }
            hasInvincibility = false;
            hasDoublePoints = false;
        }
    }

    private void drawLives() {
        String livesText = "Lives: " + lives;
        canvas.drawText(livesText, 20, 100, scorePaint); // Posunuté nižšie pod skóre
    }

    public void toggleBackgroundMusic() {
        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
                preferences.edit().putBoolean(SOUND_ENABLED, false).apply();
            } else {
                backgroundMusic.start();
                preferences.edit().putBoolean(SOUND_ENABLED, true).apply();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (backgroundMusic != null && preferences.getBoolean(SOUND_ENABLED, true)) {
            backgroundMusic.start();
        }
    }

    public void cleanup() {
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }
}