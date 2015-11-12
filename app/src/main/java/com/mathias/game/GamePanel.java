package com.mathias.game;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.test.suitebuilder.annotation.Smoke;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;

import java.util.ArrayList;

/**
 * Created by Mathias on 2015-11-11.
 */

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<Smokepuff> smoke;
    private long smokeStartTime;

    public GamePanel(Context c) {

        super(c);

        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);

        thread = new MainThread(getHolder(), this);

        //make gamePanel focusable so it can handle events
        setFocusable(true);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        int counter = 0;
        while(retry && counter<1000) {

            counter++;

            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
            }
            catch(InterruptedException e) {

                e.printStackTrace();
            }
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        smoke = new ArrayList<Smokepuff>();

        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction()==MotionEvent.ACTION_DOWN) {

            if(!player.getPlaying()) {

                player.setPlaying(true);

            }

            else {

                player.setUp(true);

            }

            return true;

        }

        if(event.getAction()==MotionEvent.ACTION_UP) {

            player.setUp(false);
            return true;

        }

        return super.onTouchEvent(event);
    }

    public void update() {

        if(player.getPlaying()) {

            bg.update();
            player.update();

            long elapsed = (System.nanoTime() - smokeStartTime)/1000000;
            if(elapsed > 120) {

                smoke.add(new Smokepuff(player.getX(), player.getY()+10));
                smokeStartTime = System.nanoTime();

            }

            //Removing smoke from Heli outside the screen
            for(int i = 0; i<smoke.size();i++) {

                smoke.get(i).update();

                if(smoke.get(i).getX()<-10) {

                    smoke.remove(i);

                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {

        final float scaleFactorX = (float) getWidth()/(WIDTH*1.f);
        final float scaleFactorY = (float) getHeight()/(HEIGHT*1.f);

        if(canvas!=null) {

            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);

            for(Smokepuff sp: smoke) {

                sp.draw(canvas);

            }

            canvas.restoreToCount(savedState);

        }

    }

}


