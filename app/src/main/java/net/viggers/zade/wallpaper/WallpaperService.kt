package net.viggers.zade.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.preference.PreferenceManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

class WallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    private inner class WallpaperEngine : Engine() {
        private val handler = Handler()
        private val drawRunner = Runnable { draw() }
        private val circles: MutableList<Point>
        private val paint = Paint()
        private var width = 0
        private var height = 0
        private var visible = true
        private val maxCount: Int

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                handler.post(drawRunner)
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int,
            width: Int, height: Int
        ) {
            this.width = width
            this.height = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onTouchEvent(event: MotionEvent) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    circles.add(Point(nextCircleId, x, y))
                    drawCircles(canvas, circles)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            super.onTouchEvent(event)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (circles.size >= maxCount) {
                        circles.removeAt(0)
                    }
                    val x = (width * Math.random()).toInt()
                    val y = (height * Math.random()).toInt()
                    circles.add(Point(nextCircleId, x, y))
                    drawCircles(canvas, circles)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, 500)
            }
        }

        // Surface view requires that all elements are drawn completely
        private fun drawCircles(canvas: Canvas, circles: List<Point>) {
            canvas.drawColor(Color.BLACK)
            for (point in circles) {
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 20.0f, paint)
            }
        }

        val nextCircleId: Int
            get() = if (circles.size > 0) {
                circles[circles.size - 1].num + 1
            } else 0

        init {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@WallpaperService)

            // TODO: Don't store this as a string!
            maxCount = Integer.valueOf(prefs.getString("numberOfCircles", "4"))
            circles = ArrayList()
            paint.isAntiAlias = true
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 10f
            handler.post(drawRunner)
        }
    }
}