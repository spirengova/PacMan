package com.example.pacman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

public class Fruit {
    private float x, y;
    private Bitmap fruitBitmap;
    private RectF bounds;
    private float size;
    private int type;
    private boolean isVisible;
    private int points;
    private int boostType; // 0: speed, 1: invincibility, 2: extra life, 3: double points
    private static final float FRUIT_SCALE = 1.5f;
    private static final int BOOST_DURATION = 5000; // 5 sekúnd

    public Fruit(Context context, float x, float y, float size, int fruitType) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = fruitType;
        this.isVisible = true;
        bounds = new RectF();

        // Priraď boost type podľa typu ovocia
        switch (fruitType) {
            case 0: // Cherry - speed boost
                boostType = 0;
                points = 200;
                break;
            case 1: // Strawberry - invincibility
                boostType = 1;
                points = 400;
                break;
            case 2: // Orange - extra life
                boostType = 2;
                points = 600;
                break;
            case 3: // Apple - double points
                boostType = 3;
                points = 800;
                break;
        }

        // Load appropriate fruit image
        int resourceId;
        switch (fruitType) {
            case 0:
                resourceId = R.drawable.fruit_cherry;
                break;
            case 1:
                resourceId = R.drawable.fruit_strawberry;
                break;
            case 2:
                resourceId = R.drawable.fruit_orange;
                break;
            default:
                resourceId = R.drawable.fruit_apple;
                break;
        }
        
        fruitBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        int scaledSize = (int)(this.size * FRUIT_SCALE);
        fruitBitmap = Bitmap.createScaledBitmap(fruitBitmap, scaledSize, scaledSize, true);
        updateBounds();
    }

    private void updateBounds() {
        float halfSize = size * FRUIT_SCALE / 2;
        bounds.set(x - halfSize, y - halfSize, x + halfSize, y + halfSize);
    }

    public void draw(Canvas canvas) {
        if (isVisible) {
            float halfSize = size * FRUIT_SCALE / 2;
            canvas.drawBitmap(fruitBitmap, x - halfSize, y - halfSize, null);
        }
    }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }
    public int getPoints() { return points; }
    public RectF getBounds() { return bounds; }

    public void updateScale(float newScaleFactor) {
        float ratioX = x / size;
        float ratioY = y / size;
        
        size = newScaleFactor;
        
        x = ratioX * size;
        y = ratioY * size;
        
        updateBounds();
    }

    public int getBoostType() {
        return boostType;
    }

    public int getBoostDuration() {
        return BOOST_DURATION;
    }
} 