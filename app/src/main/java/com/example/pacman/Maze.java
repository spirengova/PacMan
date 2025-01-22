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
    private int[][] dots;  // 0: žiadna bodka, 1: malá bodka, 2: veľká bodka
    
    // 0: empty, 1: wall, 2: dot, 3: power pellet
    public static final int POWER_PELLET = 2;  // Konštanta pre power pellet
    private static final int MAX_POWER_PELLETS = 2;  // Zmenené zo 4 na 2
    
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

        // Inicializácia bodiek podľa mapy
        dots = new int[mazeData.length][mazeData[0].length];
        for (int y = 0; y < mazeData.length; y++) {
            for (int x = 0; x < mazeData[0].length; x++) {
                if (!isWall(x, y)) {
                    dots[y][x] = 1;  // Základná bodka
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        for (int row = 0; row < mazeData.length; row++) {
            for (int col = 0; col < mazeData[0].length; col++) {
                float left = col * cellSize;
                float top = row * cellSize;
                
                // Kresli steny
                if (mazeData[row][col] == 1) {
                        canvas.drawRect(left, top, 
                                     left + cellSize, 
                                     top + cellSize, 
                                     wallPaint);
                }
                
                // Kresli bodky
                if (dots[row][col] > 0) {
                    float centerX = left + cellSize / 2;
                    float centerY = top + cellSize / 2;
                    
                    if (dots[row][col] == POWER_PELLET) {
                        // Väčší kruh pre power pellet
                        canvas.drawCircle(centerX, centerY, cellSize * 0.3f, powerPelletPaint);
                    } else {
                        // Normálna bodka
                        canvas.drawCircle(centerX, centerY, cellSize * 0.1f, dotPaint);
                    }
                }
            }
        }
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= getColumns() || y < 0 || y >= getRows()) {
            return true; // Ak je mimo hraníc, považuj to za stenu
        }
        return mazeData[y][x] == 1; // 1 znamená stena
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

    public void restoreDotState(int[][] savedDots) {
        // Priamo skopíruj uložený stav bodiek
        for (int y = 0; y < getRows(); y++) {
            for (int x = 0; x < getColumns(); x++) {
                dots[y][x] = savedDots[y][x];
                // Ak je bodka zjedená (0), nastav aj mazeData na 0
                if (savedDots[y][x] == 0 && !isWall(x, y)) {
                    mazeData[y][x] = 0;
                }
            }
        }
    }

    public void updateScale(float newScaleFactor) {
        cellSize = newScaleFactor;
    }

    public void resetDots() {
        // Táto metóda by sa mala volať len pri novej hre
        for (int y = 0; y < getRows(); y++) {
            for (int x = 0; x < getColumns(); x++) {
                if (!isWall(x, y) && originalMazeData[y][x] != 0) {
                    dots[y][x] = 1;  // Základná bodka
                } else {
                    dots[y][x] = 0;  // Žiadna bodka v nedostupných miestach
                }
            }
        }
        addRandomPowerPellets();
    }

    private void addRandomPowerPellets() {
        // Pridaj power pellety do rohov mapy
        dots[1][1] = POWER_PELLET;  // Ľavý horný roh
        dots[1][getColumns()-2] = POWER_PELLET;  // Pravý horný roh
    }

    public boolean areAllDotsCollected() {
        for (int y = 0; y < getRows(); y++) {
            for (int x = 0; x < getColumns(); x++) {
                if (dots[y][x] > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setDot(int x, int y, int value) {
        if (x >= 0 && x < getColumns() && y >= 0 && y < getRows()) {
            dots[y][x] = value;
        }
    }

    public int getDot(int x, int y) {
        if (x >= 0 && x < getColumns() && y >= 0 && y < getRows()) {
            return dots[y][x];
        }
        return 0;
    }

    public void removeDot(int x, int y) {
        if (x >= 0 && x < getColumns() && y >= 0 && y < getRows()) {
            // Odstráň bodku z oboch polí
            dots[y][x] = 0;
            mazeData[y][x] = 0;
        }
    }

    public void setState(int x, int y, int wallState, int dotState) {
        if (x >= 0 && x < getColumns() && y >= 0 && y < getRows()) {
            mazeData[y][x] = wallState;
            dots[y][x] = dotState;
        }
    }

    public int getWall(int x, int y) {
        if (x >= 0 && x < getColumns() && y >= 0 && y < getRows()) {
            return mazeData[y][x];
        }
        return 0;
    }
} 