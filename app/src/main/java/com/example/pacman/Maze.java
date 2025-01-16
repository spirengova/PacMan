package com.example.pacman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Maze {
    private int[][] mazeData;
    private int[][] originalMazeData;
    private Paint wallPaint;
    private Paint dotPaint;
    private Paint powerPelletPaint;
    private float cellSize;
    
    // 0: empty, 1: wall, 2: dot, 3: power pellet
    public Maze(float scaleFactor) {
        cellSize = scaleFactor;
        
        wallPaint = new Paint();
        wallPaint.setColor(Color.BLUE);
        
        dotPaint = new Paint();
        dotPaint.setColor(Color.WHITE);
        
        powerPelletPaint = new Paint();
        powerPelletPaint.setColor(Color.WHITE);
        
        // Maze layout matching the original Pac-Man
        mazeData = new int[][] {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,1},
            {1,3,1,1,2,1,1,1,2,1,2,1,1,1,2,1,1,3,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,2,1,2,1,1,1,1,1,2,1,2,1,1,2,1},
            {1,2,2,2,2,1,2,2,2,1,2,2,2,1,2,2,2,2,1},
            {1,1,1,1,2,1,1,1,0,1,0,1,1,1,2,1,1,1,1},
            {0,0,0,1,2,1,0,0,0,0,0,0,0,1,2,1,0,0,0},
            {1,1,1,1,2,1,0,1,1,0,1,1,0,1,2,1,1,1,1},
            {0,0,0,0,2,0,0,1,0,0,0,1,0,0,2,0,0,0,0},
            {1,1,1,1,2,1,0,1,1,1,1,1,0,1,2,1,1,1,1},
            {0,0,0,1,2,1,0,0,0,0,0,0,0,1,2,1,0,0,0},
            {1,1,1,1,2,1,0,1,1,1,1,1,0,1,2,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,2,1,1,1,2,1,2,1,1,1,2,1,1,2,1},
            {1,3,2,1,2,2,2,2,2,2,2,2,2,2,2,1,2,3,1},
            {1,1,2,1,2,1,2,1,1,1,1,1,2,1,2,1,2,1,1},
            {1,2,2,2,2,1,2,2,2,1,2,2,2,1,2,2,2,2,1},
            {1,2,1,1,1,1,1,1,2,1,2,1,1,1,1,1,1,2,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        };
        
        // Vytvor kópiu pôvodného stavu bludiska
        originalMazeData = new int[mazeData.length][mazeData[0].length];
        for (int i = 0; i < mazeData.length; i++) {
            originalMazeData[i] = mazeData[i].clone();
        }
    }

    public void draw(Canvas canvas) {
        for (int row = 0; row < mazeData.length; row++) {
            for (int col = 0; col < mazeData[0].length; col++) {
                float left = col * cellSize;
                float top = row * cellSize;
                
                switch (mazeData[row][col]) {
                    case 1: // Wall
                        canvas.drawRect(left, top, 
                                     left + cellSize, 
                                     top + cellSize, 
                                     wallPaint);
                        break;
                    case 2: // Dot
                        canvas.drawCircle(left + cellSize/2,
                                       top + cellSize/2,
                                       cellSize/10, dotPaint);
                        break;
                    case 3: // Power Pellet
                        canvas.drawCircle(left + cellSize/2,
                                       top + cellSize/2,
                                       cellSize/4, powerPelletPaint);
                        break;
                }
            }
        }
    }

    public boolean isWall(float x, float y) {
        int col = (int)(x / cellSize);
        int row = (int)(y / cellSize);
        
        if (row < 0 || row >= mazeData.length || 
            col < 0 || col >= mazeData[0].length) {
            return true;
        }
        
        return mazeData[row][col] == 1;
    }

    public int getDot(float x, float y) {
        int col = (int)(x / cellSize);
        int row = (int)(y / cellSize);
        
        if (row < 0 || row >= mazeData.length || 
            col < 0 || col >= mazeData[0].length) {
            return 0;
        }
        
        int value = mazeData[row][col];
        if (value == 2 || value == 3) {
            mazeData[row][col] = 0;
            return value;
        }
        return 0;
    }

    public float getCellSize() {
        return cellSize;
    }

    public int getRows() {
        return mazeData.length;
    }

    public int getColumns() {
        return mazeData[0].length;
    }

    public int getDotType(int row, int col) {
        if (row < 0 || row >= mazeData.length || 
            col < 0 || col >= mazeData[0].length) {
            return 0;
        }
        return mazeData[row][col];
    }

    public void restoreDotState(int[][] state) {
        for (int i = 0; i < mazeData.length; i++) {
            for (int j = 0; j < mazeData[0].length; j++) {
                mazeData[i][j] = state[i][j];
            }
        }
    }

    public void updateScale(float newScaleFactor) {
        cellSize = newScaleFactor;
    }

    public void resetDots() {
        // Obnov všetky bodky do pôvodného stavu
        for (int i = 0; i < mazeData.length; i++) {
            mazeData[i] = originalMazeData[i].clone();
        }
    }

    public boolean areAllDotsCollected() {
        for (int row = 0; row < mazeData.length; row++) {
            for (int col = 0; col < mazeData[0].length; col++) {
                if (mazeData[row][col] == 2 || mazeData[row][col] == 3) {
                    return false; // There are still dots or power pellets
                }
            }
        }
        return true; // All dots and power pellets are collected
    }
} 