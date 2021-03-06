package jliu.plumberrun;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

class GameLoop extends Thread {
    private final Game game;
    private final SurfaceHolder surfaceHolder;
    private final static double TARGET_UPS = 60;
    private double averageUPS, averageFPS;
    private boolean running = true, paused = false;

    GameLoop(Game game, SurfaceHolder surfaceHolder) {
        this.game = game;
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void run() {
        super.run();
        Canvas canvas = null;
        int updateCount = 0, frameCount = 0;
        long startTime, elapsedTime, sleepTime;
        startTime = System.currentTimeMillis();

        while (running) {
            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    game.update();
                    updateCount++;
                    game.draw(canvas);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        frameCount++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            elapsedTime = System.currentTimeMillis() - startTime;
            sleepTime = (long) (updateCount * (1E+3 / TARGET_UPS) - elapsedTime);
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (sleepTime < 0) {
                game.update();
                updateCount++;
                elapsedTime = System.currentTimeMillis() - startTime;
                sleepTime = (long) (updateCount * (1E+3 / TARGET_UPS) - elapsedTime);
            }

            if (elapsedTime > 1000) {
                averageUPS = updateCount * elapsedTime * 1E-3;
                averageFPS = frameCount * elapsedTime * 1E-3;
                printUPSFPS();
                updateCount = frameCount = 0;
                startTime = System.currentTimeMillis();
            }

            if (paused) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                updateCount = frameCount = 0;
                startTime = System.currentTimeMillis();
            }
        }
    }

    private void printUPSFPS() {
        Log.i("UPS: ", Double.toString(averageUPS));
        Log.i("FPS: ", Double.toString(averageFPS));
    }

    void endLoop() {
        running = false;
    }

    void pause() {
        paused = true;
    }

    void resumeGame() {
        paused = false;
        synchronized (this) {
            notify();
        }
    }
}
