package com.example.pacman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class PacMan {
    private float x, y;
    private float speed;
    private int direction; // 0:right, 1:down, 2:left, 3:up
    private Paint paint;
    private RectF bounds;
    private float size;
    private float maxX; // maximálna X súradnica (šírka bludiska)
    private float speedMultiplier = 1.0f;

    public PacMan(float startX, float startY, float size) {
        x = startX;
        y = startY;
        this.size = size;
        speed = size / 10; // Scale speed based on size
        direction = 0;
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setAntiAlias(true);
        bounds = new RectF();
        updateBounds();
    }

    private void updateBounds() {
        float padding = size * 0.1f; // 10% padding
        bounds.set(x - size + padding, y - size + padding, x + size - padding, y + size - padding);
    }

    public void update(Maze maze) {
        float nextX = x;
        float nextY = y;
        
        switch (direction) {
            case 0: nextX += speed * speedMultiplier; break; // Right
            case 1: nextY += speed * speedMultiplier; break; // Down
            case 2: nextX -= speed * speedMultiplier; break; // Left
            case 3: nextY -= speed * speedMultiplier; break; // Up
        }

        // Check if the next position is a wall
        if (!maze.isWall(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }

        // Teleportovanie na opačnú stranu obrazovky
        if (x > maxX) {
            x = 0;
        } else if (x < 0) {
            x = maxX;
        }

        updateBounds();
    }

    public void draw(Canvas canvas) {
        canvas.drawArc(bounds, 30, 300, true, paint);
    }

    public void setDirection(int newDirection) {
        direction = newDirection;
    }

    public int getDirection() {
        return direction;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public RectF getBounds() { return bounds; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        updateBounds();
    }

    public void updateScale(float newScaleFactor) {
        // Prepočítaj pozíciu podľa nového scale factoru
        float ratioX = x / size;
        float ratioY = y / size;
        
        size = newScaleFactor / 2;  // Polovičná veľkosť bunky
        speed = size / 10;  // Aktualizuj rýchlosť
        
        // Nastav novú pozíciu so zachovaním pomeru
        x = ratioX * size;
        y = ratioY * size;
        
        updateBounds();
    }

    public void setMaxX(float maxX) {
        this.maxX = maxX;
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    public void resetColor() {
        paint.setColor(Color.YELLOW);
    }

    public void update() {

    }
} 