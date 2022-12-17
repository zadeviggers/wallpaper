package net.viggers.zade.wallpaper

import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.Toast


class WallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    inner class WallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { drawTick() }
        private val shapes = ArrayList<Shape>()
        private val paint = Paint()
        private var width = 0
        private var height = 0
        private var visible = true

        // Preferences
        private val onSharedPreferenceChanged: OnSharedPreferenceChangeListener =
                OnSharedPreferenceChangeListener { newPrefs, _ ->
                    loadPreferences(newPrefs)
                    Log.v("ZV-Wallpaper", "Preferences changed")
                }

        private val defaultMaxCount: Int = R.integer.numberOfShapesDefault
        private val defaultRandomShapeSpawningEnabled: Boolean = R.bool.enableRandomShapeSpawningDefault == 1
        private val defaultRandomShapeDelay: Int = R.integer.randomShapeSpawnDelayDefault
        private val defaultShapeColour: Int = Color.RED
        private val defaultBackgroundColour: Int = Color.BLACK
        private val defaultShapeType: String = getString(R.string.shapeTypeDefault)
        private val defaultPauseRandomShapesWhenDragging: Boolean = R.bool.pauseRandomShapesWhenDraggingDefault == 1
        private val defaultSmoothDrawingEnabled: Boolean = R.bool.smoothDrawingEnabledDefault == 1
        private val defaultRandomShapeColoursEnabled: Boolean = R.bool.randomShapeColoursEnabledDefault == 1
        private val defaultRandomShapeTypesEnabled: Boolean = R.bool.randomShapeTypesEnabledDefault == 1
        private val enableTouchInteractionDefault: Boolean = R.bool.enableTouchInteractionDefault == 1
        private val defaultShapeSize: Float = R.integer.defaultShapeSize.toFloat()
        private val defaultRandomShapeSizesEnabled: Boolean = R.bool.enableRandomShapeSizesDefault == 1

        private var maxCount: Int = defaultMaxCount
        private var randomShapeSpawningEnabled: Boolean = defaultRandomShapeSpawningEnabled
        private var randomShapeSpawnDelay: Int = defaultRandomShapeDelay
        private var shapeColour: Int = defaultShapeColour
        private var backgroundColour: Int = defaultBackgroundColour
        private var shapeType: String = defaultShapeType
        private var pauseRandomShapesWhenDragging: Boolean = defaultPauseRandomShapesWhenDragging
        private var smoothDrawingEnabled: Boolean = defaultSmoothDrawingEnabled
        private var randomShapeColoursEnabled: Boolean = defaultRandomShapeColoursEnabled
        private var randomShapeTypesEnabled: Boolean = defaultRandomShapeTypesEnabled
        private var enableTouchInteraction: Boolean = enableTouchInteractionDefault
        private var shapeSize: Float = defaultShapeSize
        private var randomShapeSizesEnabled: Boolean = defaultRandomShapeSizesEnabled

        private var randomShapesDraggingCooldown: Int = 5


        private val nextShapeId: Int
            get() = if (shapes.size > 0) {
                if (shapes[shapes.size - 1].num > 100000) {
                    0
                } else {
                    shapes[shapes.size - 1].num + 1
                }
            } else 0

        private val nextShapeColour: Int
            get() {
                if (!randomShapeColoursEnabled) {
                    return shapeColour
                }
                val chosenColourString = resources.getStringArray(R.array.colourPalette).random()
                return Color.parseColor(chosenColourString)
            }

        private val nextShapeType: String
            get() {
                if (!randomShapeTypesEnabled) {
                    return shapeType
                }
                val shapeTypes: Array<String> = arrayOf("circle", "square", "triangle")
                return shapeTypes.random()
            }

        private val nextShapeSize: Float
            get() {
                if (!randomShapeSizesEnabled) {
                    return shapeSize
                }
                return (20..700).random().toFloat()
            }

        init {
            // Load preferences
            val prefs =
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@WallpaperService)

            prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChanged)
            loadPreferences(prefs)

            Log.v("ZV-Wallpaper", "Loaded wallpaper preferences")

            // Setup variables
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 10f

            // Start drawing loop
            handler.post(drawRunner)

            // Setup broadcast receivers

            // Listen for broadcasts to clear the shapes
            val clearShapesReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    this@WallpaperEngine.clearAllShapes()
                }
            }
            registerReceiver(clearShapesReceiver, IntentFilter(getString(R.string.action_remove_all_shapes)))
        }

        fun clearAllShapes() {
            shapes.clear()
            drawShapes()
            Toast.makeText(this@WallpaperService, R.string.shapes_cleared_toast, Toast.LENGTH_SHORT).show()
        }

        private fun loadPreferences(prefs: SharedPreferences) {
            randomShapeSpawningEnabled = prefs.getBoolean("enableRandomShapeSpawning", defaultRandomShapeSpawningEnabled)
            maxCount =
                    Integer.valueOf(
                            prefs.getString("numberOfShapes", defaultMaxCount.toString()).toString()
                    )
            randomShapeSpawnDelay = Integer.valueOf(
                    prefs.getString(
                            "randomShapeSpawnDelay",
                            defaultRandomShapeDelay.toString()
                    ).toString()
            )
            shapeColour = prefs.getInt("shapeColour", defaultShapeColour)
            backgroundColour = prefs.getInt("backgroundColour", defaultBackgroundColour)
            shapeType = prefs.getString("shapeType", defaultShapeType).toString()
            pauseRandomShapesWhenDragging = prefs.getBoolean(
                    "pauseRandomShapesWhenDragging",
                    defaultPauseRandomShapesWhenDragging
            )
            smoothDrawingEnabled =
                    prefs.getBoolean("smoothDrawingEnabled", defaultSmoothDrawingEnabled)
            randomShapeColoursEnabled =
                    prefs.getBoolean("randomShapeColoursEnabled", defaultRandomShapeColoursEnabled)
            randomShapeTypesEnabled =
                    prefs.getBoolean("randomShapeTypeEnabled", defaultRandomShapeTypesEnabled)
            enableTouchInteraction =
                    prefs.getBoolean("enableTouchInteraction", enableTouchInteractionDefault)
            shapeSize =
                    Integer.valueOf(
                            prefs.getString("shapeSize1", defaultShapeSize.toString()).toString()
                    ).toFloat()
            randomShapeSizesEnabled =
                    prefs.getBoolean("randomShapeSizesEnabled", defaultRandomShapeSizesEnabled)
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
            if (enableTouchInteraction) {
                val x = event.x
                val y = event.y

                val shapesToAdd = ArrayList<Shape>()

                // Add shape at touch location
                shapesToAdd.add(
                        Shape(
                                nextShapeId,
                                x,
                                y,
                                nextShapeSize,
                                nextShapeColour,
                                nextShapeType,
                                true
                        )
                )

                if (smoothDrawingEnabled && shapes.size > 0) {
                    val last = shapes.last()

                    if (last.spawnedByTouch) {
                        val lastX = last.x
                        val lastY = last.y

                        val differenceX = x - lastX
                        val differenceY = y - lastY

                        val x1 = lastX + (differenceX / 3)
                        val y1 = lastY + (differenceY / 3)
                        shapesToAdd.add(
                                Shape(
                                        nextShapeId,
                                        x1,
                                        y1,
                                        nextShapeSize,
                                        nextShapeColour,
                                        nextShapeType,
                                        true
                                )
                        )

                        val x2 = lastX + (differenceX * 2 / 3)
                        val y2 = lastY + (differenceY * 2 / 3)
                        shapesToAdd.add(
                                Shape(
                                        nextShapeId,
                                        x2,
                                        y2,
                                        nextShapeSize,
                                        nextShapeColour,
                                        nextShapeType,
                                        true
                                )
                        )
                    }
                }

                addShapes(shapesToAdd.toTypedArray())

                if (pauseRandomShapesWhenDragging) {
                    randomShapesDraggingCooldown = 5
                }
            }

            super.onTouchEvent(event)
        }

        private fun addShapes(shapesToAdd: Array<Shape>) {

            // Need to do this so that if the users lowers their shape limit, there aren't too many shapes on screen
            while ((shapes.size >= maxCount)) {
                shapes.removeFirst()
            }
            for (shape in shapesToAdd) {
                shapes.add(shape)
            }
            drawShapes()
        }

        private fun drawTick() {
            if (randomShapeSpawningEnabled) {
                val x = (width * Math.random()).toFloat()
                val y = (height * Math.random()).toFloat()
                if (pauseRandomShapesWhenDragging) {
                    if (randomShapesDraggingCooldown == 0) {
                        addShapes(
                                arrayOf(
                                        Shape(
                                                nextShapeId,
                                                x,
                                                y,
                                                nextShapeSize,
                                                nextShapeColour,
                                                nextShapeType,
                                                false
                                        )
                                )
                        )
                    } else if (randomShapesDraggingCooldown > 0) {
                        randomShapesDraggingCooldown -= 1
                    }
                } else {
                    addShapes(
                            arrayOf(
                                    Shape(
                                            nextShapeId,
                                            x,
                                            y,
                                            nextShapeSize,
                                            nextShapeColour,
                                            nextShapeType,
                                            false
                                    )
                            )
                    )
                }
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, randomShapeSpawnDelay.toLong())
            }
        }

        // Surface view requires that all elements are drawn completely
        private fun drawShapes() {
            var canvas: Canvas? = null
            val holder = surfaceHolder
            try {
                canvas = holder.lockCanvas()
                if (canvas != null && maxCount != 0) {
                    canvas.drawColor(backgroundColour)
                    for (shape in shapes) {
                        paint.color = shape.colour

                        val x = shape.x
                        val y = shape.y

                        val size = shape.size

                        when (shape.type) {
                            "circle" -> canvas.drawCircle(x, y, size / 2, paint)
                            "square" -> {
                                // Android graphics rectangles are really weird - they take top, left, top+height, and left+width distances,
                                // rather than being normal and having x, y, width, and height
                                val rect =
                                        RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
                                canvas.drawRect(rect, paint)
                            }
                            "triangle" -> drawTriangle(
                                    x - size / 2,
                                    y + size / 2,
                                    size,
                                    size,
                                    false,
                                    paint,
                                    canvas
                            )
                            else -> canvas.drawCircle(x, y, size, paint)
                        }
                    }
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
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
            val p1 = Point(x.toInt(), y.toInt())
            val pointX = x + width / 2
            val pointY = if (inverted) y + height else y - height
            val p2 = Point(pointX.toInt(), pointY.toInt())
            val p3 = Point((x + width).toInt(), y.toInt())
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