package com.example.pacman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Ghost {
    private int gridX, gridY;
    private float pixelX, pixelY;
    private float cellSize;
    private RectF bounds;
    private Bitmap ghostBitmap;
    private int startGridX, startGridY;
    private float speedMultiplier = 1.0f;
    private float speed = 0.075f;  // 75% rýchlosti Pac-Mana (0.1f)
    private float moveProgress = 0.0f;
    private float targetPixelX, targetPixelY;  // Cieľové pozície pre plynulý pohyb
    private static final float SMOOTH_SPEED = 4.0f;  // Rýchlosť plynulého pohybu
    private static final float CHASE_SPEED = 2.0f;  // Znížená rýchlosť prenasledovania pre lepšiu hrateľnosť
    private boolean isVulnerable = false;
    private long vulnerableStartTime = 0;
    private static final long VULNERABLE_DURATION = 4000; // 4 sekundy v milisekundách
    private Bitmap normalBitmap;
    private Bitmap blueBitmap;
    private boolean isVisible = true;  // Nová premenná pre viditeľnosť
    private static final long RESPAWN_DELAY = 10000; // 10 sekúnd v milisekundách
    private long respawnTime = 0;
    private boolean isWaitingForRespawn = false;

    public Ghost(Context context, int startGridX, int startGridY, float cellSize, int ghostType) {
        // Nastavenie počiatočnej pozície podľa typu ducha v strede riadku
        switch(ghostType) {
            case 0: // Červený duch - vľavo
                this.gridX = 8;
                this.gridY = 9;
                this.startGridX = 8;
                this.startGridY = 9;
                break;
            case 1: // Ružový duch - stred
                this.gridX = 9;
                this.gridY = 9;
                this.startGridX = 9;
                this.startGridY = 9;
                break;
            case 2: // Oranžový duch - vpravo
                this.gridX = 10;
                this.gridY = 9;
                this.startGridX = 10;
                this.startGridY = 9;
                break;
        }
        
        this.cellSize = cellSize;
        this.pixelX = this.gridX * cellSize + cellSize / 2;
        this.pixelY = this.gridY * cellSize + cellSize / 2;
        this.targetPixelX = this.pixelX;
        this.targetPixelY = this.pixelY;
        
        bounds = new RectF();
        updateBounds();

        // Načítaj oba obrázky ducha
        int resourceId;
        switch(ghostType) {
            case 0: resourceId = R.drawable.ghost_red; break;
            case 1: resourceId = R.drawable.ghost_pink; break;
            case 2: resourceId = R.drawable.ghost_orange; break;
            default: resourceId = R.drawable.ghost_red;
        }
        normalBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        normalBitmap = Bitmap.createScaledBitmap(normalBitmap, 
            (int)(cellSize * 0.8f), (int)(cellSize * 0.8f), true);
            
        blueBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ghost_blue);
        blueBitmap = Bitmap.createScaledBitmap(blueBitmap, 
            (int)(cellSize * 0.8f), (int)(cellSize * 0.8f), true);
        
        ghostBitmap = normalBitmap;
    }

    public void update(Maze maze, PacMan pacman, Ghost[] otherGhosts) {
        // Kontrola respawnu
        if (isWaitingForRespawn) {
            if (System.currentTimeMillis() >= respawnTime) {
                respawn();
            } else {
                return; // Čakáme na respawn, nič nerobíme
            }
        }

        // Kontrola či sa skončil vulnerable stav
        if (isVulnerable && System.currentTimeMillis() - vulnerableStartTime > VULNERABLE_DURATION) {
            resetState();
        }

        moveProgress += speed * speedMultiplier;
        
        if (Math.abs(pixelX - targetPixelX) < 2 && Math.abs(pixelY - targetPixelY) < 2 && moveProgress >= 1.0f) {
            moveProgress = 0.0f;
            
            // Získaj smer k Pac-Manovi
            int dx = pacman.getGridX() - gridX;
            int dy = pacman.getGridY() - gridY;
            
            // Ak je duch zraniteľný, uteká od Pac-Mana
            if (isVulnerable) {
                dx = -dx;
                dy = -dy;
            }
            
            // Inteligentnejší výber smeru
            int[][] possibleMoves = getPossibleMoves(maze, otherGhosts);
            int[] bestMove = chooseBestMove(possibleMoves, dx, dy, maze);
            
            if (bestMove != null) {
                gridX += bestMove[0];
                gridY += bestMove[1];
                
                // Teleportácia na druhú stranu mapy
                if (gridX < 0) gridX = maze.getColumns() - 1;
                if (gridX >= maze.getColumns()) gridX = 0;
                
                targetPixelX = gridX * cellSize + cellSize / 2;
                targetPixelY = gridY * cellSize + cellSize / 2;
            }
        }
        
        // Plynulý pohyb
        float dx = targetPixelX - pixelX;
        float dy = targetPixelY - pixelY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            pixelX += (dx / distance) * CHASE_SPEED;
            pixelY += (dy / distance) * CHASE_SPEED;
        }
        
        updateBounds();
    }

    private int[][] getPossibleMoves(Maze maze, Ghost[] otherGhosts) {
        int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        java.util.List<int[]> validMoves = new java.util.ArrayList<>();
        
        for (int[] dir : directions) {
            int nextX = gridX + dir[0];
            int nextY = gridY + dir[1];
            
            if (!maze.isWall(nextX, nextY) && !isGhostAtPosition(nextX, nextY, otherGhosts)) {
                validMoves.add(dir);
            }
        }
        
        return validMoves.toArray(new int[validMoves.size()][]);
    }

    private int[] chooseBestMove(int[][] possibleMoves, int dx, int dy, Maze maze) {
        if (possibleMoves.length == 0) return null;
        
        int[] bestMove = possibleMoves[0];
        float bestScore = Float.NEGATIVE_INFINITY;
        
        for (int[] move : possibleMoves) {
            float score = calculateMoveScore(move, dx, dy, maze);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove;
    }

    private float calculateMoveScore(int[] move, int dx, int dy, Maze maze) {
        float score = 0;
        
        // Smerový komponent - preferuj pohyb v smere k/od Pac-Mana
        if (isVulnerable) {
            score -= (move[0] * dx + move[1] * dy) * 2;  // Silnejšie vyhýbanie sa
        } else {
            score += move[0] * dx + move[1] * dy;
        }
        
        // Vyhýbanie sa rohom keď je zraniteľný
        if (isVulnerable) {
            int nextX = gridX + move[0];
            int nextY = gridY + move[1];
            int wallCount = countAdjacentWalls(nextX, nextY, maze);
            score -= wallCount * 2;
        }
        
        return score;
    }

    private int countAdjacentWalls(int x, int y, Maze maze) {
        int count = 0;
        int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] dir : dirs) {
            if (maze.isWall(x + dir[0], y + dir[1])) count++;
        }
        return count;
    }

    public void updateRandomly(Maze maze) {
        moveProgress += speed;
        
        // Ak sme blízko cieľovej pozície a je čas na nový pohyb
        if (Math.abs(pixelX - targetPixelX) < 2 && Math.abs(pixelY - targetPixelY) < 2 && moveProgress >= 1.0f) {
            moveProgress = 0.0f;
            
            int[] directions = {0, 1, 2, 3};
            int randomDirection = directions[(int) (Math.random() * directions.length)];
            
            int nextGridX = gridX;
            int nextGridY = gridY;
            
            switch (randomDirection) {
                case 0: nextGridX++; break;
                case 1: nextGridY++; break;
                case 2: nextGridX--; break;
                case 3: nextGridY--; break;
            }
            
            // Teleportácia na opačnú stranu, ak duch opustí mapu
            if (nextGridX < 0) {
                nextGridX = maze.getColumns() - 1;
            } else if (nextGridX >= maze.getColumns()) {
                nextGridX = 0;
            }
            
            // Ak nie je stena, nastav nový cieľ
            if (!maze.isWall(nextGridX, nextGridY)) {
                gridX = nextGridX;
                gridY = nextGridY;
                targetPixelX = gridX * cellSize + cellSize / 2;
                targetPixelY = gridY * cellSize + cellSize / 2;
            }
        }

        // Plynulý pohyb k cieľovej pozícii
        float dx = targetPixelX - pixelX;
        float dy = targetPixelY - pixelY;
        
        pixelX += dx * 0.1f;  // Plynulejší pohyb
        pixelY += dy * 0.1f;
        
        updateBounds();
    }

    private void updateBounds() {
        float size = cellSize * 0.8f;
        bounds.set(
            pixelX - size/2,
            pixelY - size/2,
            pixelX + size/2,
            pixelY + size/2
        );
    }

    public void draw(Canvas canvas) {
        if (isVisible) {  // Kresli ducha len ak je viditeľný
            canvas.drawBitmap(ghostBitmap, pixelX - cellSize / 2, pixelY - cellSize / 2, null);
        }
    }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public float getPixelX() { return pixelX; }
    public float getPixelY() { return pixelY; }
    public RectF getBounds() { return bounds; }

    public void resetPosition() {
        gridX = startGridX;
        gridY = startGridY;
        pixelX = gridX * cellSize + cellSize / 2;
        pixelY = gridY * cellSize + cellSize / 2;
        targetPixelX = pixelX;  // Pridané - reset cieľovej pozície
        targetPixelY = pixelY;  // Pridané - reset cieľovej pozície
        updateBounds();
    }

    public void updateScale(float newCellSize) {
        this.cellSize = newCellSize;
        this.pixelX = gridX * cellSize + cellSize / 2;
        this.pixelY = gridY * cellSize + cellSize / 2;
        updateBounds();
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void setPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.pixelX = gridX * cellSize + cellSize / 2;
        this.pixelY = gridY * cellSize + cellSize / 2;
        updateBounds();
    }

    private boolean isGhostAtPosition(int x, int y, Ghost[] otherGhosts) {
        for (Ghost ghost : otherGhosts) {
            if (ghost != this && ghost.gridX == x && ghost.gridY == y) {
                return true;
            }
        }
        return false;
    }

    public void makeVulnerable() {
        isVulnerable = true;
        vulnerableStartTime = System.currentTimeMillis();
        ghostBitmap = blueBitmap;
    }

    public boolean isVulnerable() {
        return isVulnerable;
    }

    public void resetState() {
        isVulnerable = false;
        ghostBitmap = normalBitmap;
        isVisible = true;  // Reset viditeľnosti pri resete stavu
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public void handleEaten() {
        isVisible = false;
        isWaitingForRespawn = true;
        respawnTime = System.currentTimeMillis() + RESPAWN_DELAY;
    }

    private void respawn() {
        // Reset pozície
        resetPosition();
        
        // Reset stavu
        resetState();
        
        // Reset pohybových premenných
        moveProgress = 0.0f;
        targetPixelX = pixelX;  // Reset cieľovej pozície
        targetPixelY = pixelY;
        speedMultiplier = 1.0f;
        
        isVisible = true;
        isWaitingForRespawn = false;
    }
} 