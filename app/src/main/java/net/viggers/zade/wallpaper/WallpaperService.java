package net.viggers.zade.wallpaper;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

public class WallpaperService extends android.service.wallpaper.WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    private class WallpaperEngine extends Engine {

        private final Handler handler = new Handler();

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        private List<Point> circles;
        private Paint paint = new Paint();

        private int width;
        private int height;

        private boolean visible = true;
        private int maxCount;

        private boolean touchEnabled;

        public WallpaperEngine() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WallpaperService.this);

            // TODO: Don't store this as a string!
            maxCount = Integer.valueOf(prefs.getString("numberOfCircles", "4"));

            touchEnabled = prefs.getBoolean("touchEnabled", false);

            circles = new ArrayList<Point>();

            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(10f);

            handler.post(drawRunner);

        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            this.visible = isVisible;
            if (isVisible) {
                handler.post(drawRunner);
            } else {
              handler.removeCallbacks(drawRunner);
            }

        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (touchEnabled) {

                int x = (int) event.getX();
                int y = (int) event.getY();
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(Color.BLACK);
                        circles.clear();
                        circles.add(new Point(getNextCircleId(), x, y));
                        drawCircles(canvas, circles);

                    }
                } finally {
                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas);
                }
                super.onTouchEvent(event);
            }
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    if (circles.size() > maxCount) {
                        circles.remove(0);
                    }
                    int x = (int) (width * Math.random());
                    int y = (int) (height * Math.random());
                    circles.add(new Point(getNextCircleId(), x, y));

                    drawCircles(canvas, circles);
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
            handler.removeCallbacks(drawRunner);
            if (visible) {
                handler.postDelayed(drawRunner, 500);
            }
        }

        // Surface view requires that all elements are drawn completely
        private void drawCircles(Canvas canvas, List<Point> circles) {
            canvas.drawColor(Color.BLACK);
            for (Point point : circles) {
                canvas.drawCircle(point.x, point.y, 20.0f, paint);
            }
        }

        public int getNextCircleId() {
            if (circles.size() > 0) {
                return circles.get(circles.size() - 1).num + 1;
            }
            return 0;
        }
    }
}
