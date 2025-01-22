package com.example.pacman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class PacMan {
    private int gridX, gridY;  // Pozície v mriežke (celé čísla)
    private float pixelX, pixelY;  // Skutočné pozície na obrazovke
    private int direction = -1;  // -1:stojí, 0:vpravo, 1:dole, 2:vľavo, 3:hore
    private float cellSize;  // Veľkosť jednej bunky
    private RectF bounds;
    private Paint paint;
    private boolean isMoving = false;  // Pridané
    private float speed = 0.1f;  // Rýchlosť Pac-Mana, menšia hodnota znamená pomalší pohyb
    private float moveProgress = 0.0f;  // Progres pohybu medzi bunkami
    private int desiredDirection = -1;  // Požadovaný smer
    private float rotation = 0;  // Uhol rotácie v stupňoch
    private float speedMultiplier = 1.0f;

    public PacMan(int startGridX, int startGridY, float cellSize) {
        this.gridX = startGridX;
        this.gridY = startGridY;
        this.cellSize = cellSize;
        
        this.pixelX = gridX * cellSize + cellSize / 2;
        this.pixelY = gridY * cellSize + cellSize / 2;
        
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        bounds = new RectF();
        updateBounds();
    }

    public void setPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.pixelX = gridX * cellSize + cellSize / 2;
        this.pixelY = gridY * cellSize + cellSize / 2;
        updateBounds();
    }

    public void updateScale(float newCellSize) {
        this.cellSize = newCellSize;
        this.pixelX = gridX * cellSize + cellSize / 2;
        this.pixelY = gridY * cellSize + cellSize / 2;
        updateBounds();
    }

    public boolean isMoving() {
        return isMoving;
    }

    public int getDirection() {
        return direction;
    }

    public void update(Maze maze) {
        if (direction == -1 && desiredDirection != -1) {
            // Ak stojíme ale máme požadovaný smer, skús sa pohnúť tým smerom
            direction = desiredDirection;
        }

        if (direction != -1) {
            moveProgress += speed * speedMultiplier;
            if (moveProgress >= 1.0f) {
                moveProgress = 0.0f;

                // Najprv skús požadovaný smer
                if (desiredDirection != -1 && desiredDirection != direction) {
                    int nextX = gridX;
                    int nextY = gridY;
                    
                    switch (desiredDirection) {
                        case 0: nextX++; break;
                        case 1: nextY++; break;
                        case 2: nextX--; break;
                        case 3: nextY--; break;
                    }
                    
                    if (!maze.isWall(nextX, nextY)) {
                        direction = desiredDirection;
                        gridX = nextX;
                        gridY = nextY;
                        updatePosition();
                        return;
                    }
                }

                // Ak nemôžeme ísť požadovaným smerom, pokračuj v aktuálnom
                int nextX = gridX;
                int nextY = gridY;
                
                switch (direction) {
                    case 0: nextX++; break;
                    case 1: nextY++; break;
                    case 2: nextX--; break;
                    case 3: nextY--; break;
                }

                // Teleportácia na opačnú stranu
                if (nextX < 0) nextX = maze.getColumns() - 1;
                else if (nextX >= maze.getColumns()) nextX = 0;

                if (!maze.isWall(nextX, nextY)) {
                    gridX = nextX;
                    gridY = nextY;
                    updatePosition();
                } else {
                    direction = -1; // Zastav sa pri stene
                }
            }
        }
        updateBounds();
    }

    private void updatePosition() {
        pixelX = gridX * cellSize + cellSize / 2;
        pixelY = gridY * cellSize + cellSize / 2;
    }

    public void setDirection(int newDirection, Maze maze) {
        // Vždy nastav požadovaný smer
        this.desiredDirection = newDirection;
        
        // Ak Pacman stojí alebo sa môže otočiť do nového smeru, zmeň smer okamžite
        if (direction == -1 || canMoveInDirection(newDirection, maze)) {
            this.direction = newDirection;
            isMoving = true;
        }
    }

    private boolean canMoveInDirection(int dir, Maze maze) {
        int nextX = gridX;
        int nextY = gridY;
        
        switch (dir) {
            case 0: nextX++; break;  // Vpravo
            case 1: nextY++; break;  // Dole
            case 2: nextX--; break;  // Vľavo
            case 3: nextY--; break;  // Hore
        }
        
        // Kontrola teleportácie na druhú stranu
        if (nextX < 0) nextX = maze.getColumns() - 1;
        else if (nextX >= maze.getColumns()) nextX = 0;
        
        return !maze.isWall(nextX, nextY);
    }

    public void draw(Canvas canvas) {
        if (canvas != null) {
            // Ulož aktuálny stav canvasu
            canvas.save();
            
            // Presuň počiatok do stredu Pac-Mana
            canvas.translate(pixelX, pixelY);
            
            // Nastav rotáciu podľa smeru
            switch (direction) {
                case 0:  // Vpravo - základná pozícia (0°)
                    rotation = 0;
                    break;
                case 1:  // Dole (90°)
                    rotation = 90;
                    break;
                case 2:  // Vľavo (180°)
                    rotation = 180;
                    break;
                case 3:  // Hore (270°)
                    rotation = 270;
                    break;
            }
            
            // Aplikuj rotáciu
            canvas.rotate(rotation);
            
            // Vykresli Pac-Mana v strede s rotáciou
            float size = cellSize * 0.8f;
            RectF rotatedBounds = new RectF(
                -size/2, -size/2,
                size/2, size/2
            );
            canvas.drawArc(rotatedBounds, 30, 300, true, paint);
            
            // Obnov pôvodný stav canvasu
            canvas.restore();
        }
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

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public float getPixelX() { return pixelX; }
    public float getPixelY() { return pixelY; }

    public RectF getBounds() {
        return bounds;
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = multiplier;
    }
} 