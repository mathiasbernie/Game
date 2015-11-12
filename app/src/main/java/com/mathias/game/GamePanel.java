package com.mathias.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.test.suitebuilder.annotation.Smoke;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;

import java.util.ArrayList;
import java.util.Random;

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
    private ArrayList<Missile> missiles;
    private long missileStartTime;
    private Random rand = new Random();
    private ArrayList<TopBorder> topborder;
    private ArrayList<BottomBorder> bottomborder;
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;
    //increase to slow down difficulty progression, decrease to speed up difficulty progression
    private int progressDenom = 20;

    public GamePanel(Context c) {

        super(c);

        //add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);

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
                thread = null;
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
        smokeStartTime = System.nanoTime();
        missiles = new ArrayList<Missile>();
        missileStartTime = System.nanoTime();
        topborder = new ArrayList<TopBorder>();
        bottomborder = new ArrayList<BottomBorder>();

        thread = new MainThread(getHolder(), this);
        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getAction()==MotionEvent.ACTION_DOWN) {

            if(!player.getPlaying()) {

                player.setPlaying(true);
                player.setUp(true);

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

            //calculate the threshold of height the border can have based on the score
            //mad and min border heart are updated, and the border switched direction when
            //either max or min is met.

            maxBorderHeight = 30+player.getScore()/progressDenom;

            //cap max border height so that border can only take up a total of 1/2 the screen
            if(maxBorderHeight > HEIGHT/4) {

                maxBorderHeight = HEIGHT/4;
                minBorderHeight = 5 + player.getScore()/progressDenom;

                //check top border collision
                for(int i = 0; i < topborder.size(); i++) {

                    if(collision(topborder.get(i), player)) {

                        player.setPlaying(false);

                    }

                }

                //check bottom border collision
                for (int i = 0; i < bottomborder.size(); i++) {

                    if(collision(bottomborder.get(i), player)) {

                        player.setPlaying(false);

                    }

                }

            }

            //update top border
            this.updateTopBorder();

            //update bottom border
            this.updateBottomBorder();

            //add missiles on timer - Higher player score equals shorter delay
            long missileElapsed = (System.nanoTime()-missileStartTime)/1000000;
            if(missileElapsed>(2000 - player.getScore()/4)) {

                //first missile always goes down the middle
                if(missiles.size()==0) {

                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH+10, HEIGHT/2, 45, 15, player.getScore(), 13));

                }

                else {

                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH+10, (int) (rand.nextDouble()*(HEIGHT - (maxBorderHeight * 2)) + maxBorderHeight), 45, 15, player.getScore(), 13));

                }

                //reset timer
                missileStartTime = System.nanoTime();

            }

            //loop through every missile and check for collision between gameobjects
            for(int i = 0; i<missiles.size(); i++) {

                missiles.get(i).update();
                if(collision(missiles.get(i),player)) {

                    missiles.remove(i);
                    player.setPlaying(false);
                    break;

                }

                //remove missile if out of screen
                if(missiles.get(i).getX()<-100) {

                    missiles.remove(i);
                    break;

                }

            }

            //add smoke puffs on timer
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

        else {

            newGameCreated = false;

            if(!newGameCreated) {

                newGame();

            }
        }
    }

    public boolean collision(GameObject a, GameObject b) {

        if(Rect.intersects(a.getRectangle(),b.getRectangle())) {

            return true;

        }

        return false;

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

            //draw smokepuffs
            for(Smokepuff sp: smoke) {

                sp.draw(canvas);

            }

            //draw missiles
            for(Missile m: missiles) {

                m.draw(canvas);

            }

            canvas.restoreToCount(savedState);

            //draw top border
            for(TopBorder tb: topborder) {

                tb.draw(canvas);

            }

            //draw bottom border
            for(BottomBorder bb: bottomborder) {

                bb.draw(canvas);

            }

        }

    }

    public void updateTopBorder() {

        //every 50 points, insert randomly placed top black that break the pattern
        if (player.getScore() % 50 == 0) {

            topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    topborder.get(topborder.size() - 1).getX() + 20, 0, (int) ((rand.nextDouble() * (maxBorderHeight)) + 1)));

        }

        for (int i = 0; i < topborder.size(); i++) {

            topborder.get(i).update();

            if (topborder.get(i).getX() < -20) {

                topborder.remove(i);
                //remove element of arraylist, replace it by adding a new one

                //calculate topdown wich determines the direcoton the border is moving (up or down)
                if (topborder.get(topborder.size() - 1).getHeight() >= maxBorderHeight) {

                    topDown = false;

                }

                if (topborder.get(topborder.size() - 1).getHeight() <= minBorderHeight) {

                    topDown = true;

                }

                //new border will have larger height
                if (topDown) {

                    topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topborder.get(topborder.size() - 1).getX() + 20, 0, topborder.get(topborder.size() - 1).getHeight() + 1));

                } else {

                    topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topborder.get(topborder.size() - 1).getX() + 20, 0, topborder.get(topborder.size() - 1).getHeight() - 1));

                }
            }
        }
    }

    public void updateBottomBorder() {

        //every 40 points, insert randomly placed bottom blocks that break pattern
        if(player.getScore()%40 == 0) {

            bottomborder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    bottomborder.get(bottomborder.size()-1).getX()+20, (int)((rand.nextDouble()
                        *maxBorderHeight)+(HEIGHT-maxBorderHeight))));

        }

        //update bottom border
        for(int i = 0; i<bottomborder.size(); i++) {

            bottomborder.get(i).update();

            //if border is moving off screen, remove it and add a corresponding new one
            if(bottomborder.get(i).getX()<-20) {

                bottomborder.remove(i);

                //determine if border will be moving up or down
                if (bottomborder.get(bottomborder.size() - 1).getY() <= HEIGHT - maxBorderHeight) {

                    botDown = true;

                }

                if (bottomborder.get(bottomborder.size() - 1).getY() >= HEIGHT - minBorderHeight) {

                    botDown = false;

                }

                if (botDown) {

                    bottomborder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            bottomborder.get(bottomborder.size() - 1).getX() + 20, bottomborder.get(bottomborder.size() - 1).getY() + 1));

                } else {

                    bottomborder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            bottomborder.get(bottomborder.size() - 1).getX() + 20, bottomborder.get(bottomborder.size() - 1).getY() - 1));

                }
            }
        }
    }

    public void newGame() {

        bottomborder.clear();
        topborder.clear();
        missiles.clear();
        smoke.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;

        player.resetDY();
        player.resetScore();
        player.setY(HEIGHT/2);

        //create initial borders

        //first top border create
        for(int i = 0; i*20<WIDTH+40; i++) {

            if(i==0) {

                //first border ever created
                topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, 0, 10));

            }

            else {

                topborder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, 0, topborder.get(i-1).getHeight()+1));

            }

        }

        //initial bottom border
        for(int i = 0; i*20<WIDTH+40; i++) {

            //first bottom border ever created
            if(i==0) {

                bottomborder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, HEIGHT - minBorderHeight));

            }

            //adding borders until the initial screen is filled
            else {

                bottomborder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, bottomborder.get(i-1).getY()-1));

            }

        }

        newGameCreated = true;

    }

}


