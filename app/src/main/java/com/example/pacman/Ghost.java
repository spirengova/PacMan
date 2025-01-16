package com.example.pacman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Ghost {
    private float x, y;
    private float startX, startY; // Store initial positions
    private float speed;
    private int direction;
    private Bitmap ghostBitmap;
    private RectF bounds;
    private float size;
    private int type; // 0: red, 1: pink, 2: blue, 3: orange
    private boolean isChasing = false;
    private float speedMultiplier = 1.0f;

    public Ghost(Context context, float startX, float startY, float size, int ghostType) {
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.size = size;
        this.type = ghostType;
        speed = size / 15;  // Adjust speed as needed
        direction = 0;
        bounds = new RectF();

        // Load appropriate ghost image based on type
        int resourceId;
        switch (ghostType) {
            case 0:
                resourceId = R.drawable.ghost_red;
                break;
            case 1:
                resourceId = R.drawable.ghost_pink;
                break;
            case 2:
                resourceId = R.drawable.ghost_blue;
                break;
            default:
                resourceId = R.drawable.ghost_orange;
                break;
        }
        
        ghostBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        ghostBitmap = Bitmap.createScaledBitmap(ghostBitmap, (int)size, (int)size, true);
        updateBounds();
    }

    public void update(Maze maze, PacMan pacman) {
        // Example logic for ghost movement
        // This should be replaced with your actual movement logic
        float nextX = x;
        float nextY = y;

        // Simple AI: move towards PacMan
        if (pacman.getX() > x) {
            nextX += speed * speedMultiplier;
        } else if (pacman.getX() < x) {
            nextX -= speed * speedMultiplier;
        }

        if (pacman.getY() > y) {
            nextY += speed * speedMultiplier;
        } else if (pacman.getY() < y) {
            nextY -= speed * speedMultiplier;
        }

        // Check for wall collisions
        if (!maze.isWall(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }

        updateBounds();
    }

    private void updateBounds() {
        bounds.set(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(ghostBitmap, x - size / 2, y - size / 2, null);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public RectF getBounds() { return bounds; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        updateBounds();
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = multiplier;
    }

    public void resetPosition() {
        // Reset the ghost to its initial position
        this.x = startX;
        this.y = startY;
        updateBounds();
    }

    public void updateScale(float newScaleFactor) {
        // Update the size and speed based on the new scale factor
        float ratioX = x / size;
        float ratioY = y / size;
        
        size = newScaleFactor;
        speed = size / 15;  // Adjust speed based on new size
        
        x = ratioX * size;
        y = ratioY * size;
        
        ghostBitmap = Bitmap.createScaledBitmap(ghostBitmap, (int)size, (int)size, true);
        updateBounds();
    }
} 