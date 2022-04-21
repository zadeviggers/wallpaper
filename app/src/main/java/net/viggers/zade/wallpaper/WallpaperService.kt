package net.viggers.zade.wallpaper

import android.content.SharedPreferences
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
        private val drawRunner = Runnable { drawTick() }
        private val shapes: MutableList<Point>
        private val paint = Paint()
        private var width = 0
        private var height = 0
        private var visible = true

        // Preferences
        private val maxCount: Int
        private val randomShapesEnabled: Boolean
        private val randomShapeSpawnDelay: Int

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
            addShape(x, y)
            super.onTouchEvent(event)
        }

        private fun addShape(x: Int, y: Int) {
            var canvas: Canvas? = null
            val holder = surfaceHolder
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    if (shapes.size >= maxCount) {
                        shapes.removeAt(0)
                    }
                    shapes.add(Point(nextShapeId, x, y))
                    drawShapes(canvas, shapes)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawTick() {
            if (randomShapesEnabled) {
                val x = (width * Math.random()).toInt()
                val y = (height * Math.random()).toInt()
                addShape(x, y)
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, randomShapeSpawnDelay.toLong())
            }
        }

        // Surface view requires that all elements are drawn completely
        private fun drawShapes(canvas: Canvas, shapes: List<Point>) {
            canvas.drawColor(Color.BLACK)
            for (point in shapes) {
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 20.0f, paint)
            }
        }

        val nextShapeId: Int
            get() = if (shapes.size > 0) {
                shapes[shapes.size - 1].num + 1
            } else 0

        init {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@WallpaperService)

            randomShapesEnabled = prefs.getBoolean("enableRandomShapes", true)
            maxCount = Integer.valueOf(prefs.getString("numberOfShapes", "4"))
            randomShapeSpawnDelay = Integer.valueOf(prefs.getString("randomShapeSpawnDelay", "500"))


            shapes = ArrayList()
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