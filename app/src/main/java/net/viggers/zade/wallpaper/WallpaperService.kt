package net.viggers.zade.wallpaper

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.*
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.util.Log
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
        val onSharedPreferenceChanged: OnSharedPreferenceChangeListener =
            OnSharedPreferenceChangeListener { newPrefs, _ ->
                loadPreferences(newPrefs)
                Log.d("ZV-Wallpaper", "Preferences changed")
            }

        val defaultMaxCount: Int = 40
        val defaultRandomShapesEnabled: Boolean = true
        val defaultRandomShapeDelay: Int = 500
        val defaultShapeColour: Int = Color.RED
        val defaultBackgroundColour: Int = Color.BLACK
        val defaultShapeType: String = "circle"
        val defaultPauseRandomShapesWhenDragging: Boolean = false

        private var maxCount: Int = defaultMaxCount
        private var randomShapesEnabled: Boolean = defaultRandomShapesEnabled
        private var randomShapeSpawnDelay: Int = defaultRandomShapeDelay
        private var shapeColour: Int = defaultShapeColour
        private var backgroundColour: Int = defaultBackgroundColour
        private var shapeType: String = defaultShapeType
        private var pauseRandomShapesWhenDragging: Boolean = defaultPauseRandomShapesWhenDragging

        private var randomShapesDraggingCooldown: Int = 5

        private val size = 40.0f


        val nextShapeId: Int
            get() = if (shapes.size > 0) {
                shapes[shapes.size - 1].num + 1
            } else 0

        init {
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@WallpaperService)

            prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChanged)
            loadPreferences(prefs)

            Log.d("ZV-Wallpaper", "Loaded wallpaper service")

            shapes = ArrayList()
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 10f

            handler.post(drawRunner)
        }

        private fun loadPreferences(prefs: SharedPreferences) {
            randomShapesEnabled = prefs.getBoolean("enableRandomShapes", defaultRandomShapesEnabled)
            maxCount = Integer.valueOf(prefs.getString("numberOfShapes", defaultMaxCount.toString()))
            randomShapeSpawnDelay = Integer.valueOf(prefs.getString("randomShapeSpawnDelay", defaultRandomShapeDelay.toString()))
            shapeColour = prefs.getInt("shapeColour", defaultShapeColour)
            backgroundColour = prefs.getInt("backgroundColour", defaultBackgroundColour)
            shapeType = prefs.getString("shapeType", defaultShapeType).toString()
            pauseRandomShapesWhenDragging = prefs.getBoolean("pauseRandomShapesWhenDragging", defaultPauseRandomShapesWhenDragging)
        }

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
            if (pauseRandomShapesWhenDragging) {
                randomShapesDraggingCooldown = 5
            }
            super.onTouchEvent(event)
        }

        private fun addShape(x: Int, y: Int) {
            var canvas: Canvas? = null
            val holder = surfaceHolder
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // Need to do this so that if the users lowers their shape limit, there aren't too many shapes on screen
                    while (shapes.size >= maxCount) {
                        shapes.removeAt(0)
                    }
                    shapes.add(Point(nextShapeId, x, y, shapeColour))
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
                if (pauseRandomShapesWhenDragging) {
                    if (randomShapesDraggingCooldown == 0) {
                        addShape(x, y)
                    } else if (randomShapesDraggingCooldown > 0) {
                        randomShapesDraggingCooldown -= 1
                    }
                } else {
                    addShape(x, y)
                }
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, randomShapeSpawnDelay.toLong())
            }
        }

        // Surface view requires that all elements are drawn completely
        private fun drawShapes(canvas: Canvas, shapes: List<Point>) {
            canvas.drawColor(backgroundColour)
            for (point in shapes) {
                paint.color = point.colour

                val x = point.x.toFloat()
                val y = point.y.toFloat()

                when (shapeType) {
                    "circle" -> canvas.drawCircle(x, y, size / 2, paint)
                    "square" -> {
                        // Android graphics rectangles are really weird - they take top, left, top+height, and left+width distances,
                        // rather than being normal and having x, y, width, and height
                        val rect = RectF(x, y, x + size, y + size)
                        canvas.drawRect(rect, paint)
                    }
                    "triangle" -> drawTriangle(x, y, size, size, false, paint, canvas)
                    else -> canvas.drawCircle(x, y, size, paint)
                }

            }
        }

        // From https://stackoverflow.com/a/35873562
        private fun drawTriangle(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            inverted: Boolean,
            paint: Paint,
            canvas: Canvas
        ) {
            val p1 = android.graphics.Point(x.toInt(), y.toInt())
            val pointX = x + width / 2
            val pointY = if (inverted) y + height else y - height
            val p2 = android.graphics.Point(pointX.toInt(), pointY.toInt())
            val p3 = android.graphics.Point((x + width).toInt(), y.toInt())
            val path = Path()
            path.fillType = Path.FillType.EVEN_ODD
            path.moveTo(p1.x.toFloat(), p1.y.toFloat())
            path.lineTo(p2.x.toFloat(), p2.y.toFloat())
            path.lineTo(p3.x.toFloat(), p3.y.toFloat())
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}