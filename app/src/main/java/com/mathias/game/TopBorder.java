package com.mathias.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

/**
 * Created by Mathias on 2015-11-12.
 */
public class TopBorder extends GameObject {

    private Bitmap image;

    public TopBorder(Bitmap res, int x, int y, int h) {

        height = h;
        width = 20;

        this.y = y;

        dx = GamePanel.MOVESPEED;
        image = Bitmap.createBitmap(res, 0, 0, width, height);

    }

    public void update() {

        x += dx;

    }

    public void draw(Canvas canvas) {

        try {

            canvas.drawBitmap(image, x, y, null);

        }

        catch(Exception e) {


        }
    }
}
