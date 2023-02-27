package net.viggers.zade.wallpaper

import android.app.WallpaperColors
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
        private val defaultRandomShapeSpawningEnabled: Boolean =
            R.bool.enableRandomShapeSpawningDefault == 1
        private val defaultRandomShapeDelay: Int = R.integer.randomShapeSpawnDelayDefault
        private val defaultShapeColour: Int = getColor(R.color.shapeColourDefault)
        private val defaultBackgroundColour: Int = getColor(R.color.backgroundColourDefault)
        private val defaultShapeType: String = getString(R.string.shapeTypeDefault)
        private val defaultPauseRandomShapesWhenDragging: Boolean =
            R.bool.pauseRandomShapesWhenDraggingDefault == 1
        private val defaultSmoothDrawingEnabled: Boolean = R.bool.smoothDrawingEnabledDefault == 1
        private val defaultRandomShapeColoursEnabled: Boolean =
            R.bool.randomShapeColoursEnabledDefault == 1
        private val defaultRandomShapeTypesEnabled: Boolean =
            R.bool.randomShapeTypesEnabledDefault == 1
        private val enableTouchInteractionDefault: Boolean =
            R.bool.enableTouchInteractionDefault == 1
        private val defaultShapeSize: Float = R.integer.defaultShapeSize.toFloat()
        private val defaultRandomShapeSizesEnabled: Boolean =
            R.bool.enableRandomShapeSizesDefault == 1
        private val defaultRandomShapeRotationEnabled: Boolean =
            R.bool.enableRandomShapeRotationDefault == 1

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
        private var randomShapeRotationEnabled: Boolean = defaultRandomShapeRotationEnabled


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
                val shapeTypes: Array<String> = resources.getStringArray(R.array.shape_types)
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
            registerReceiver(
                clearShapesReceiver,
                IntentFilter(getString(R.string.action_remove_all_shapes))
            )
        }

        override fun onComputeColors(): WallpaperColors? {
            return WallpaperColors(
                Color.valueOf(backgroundColour),
                Color.valueOf(nextShapeColour),
                Color.valueOf(nextShapeColour)
            )
        }

        fun clearAllShapes() {
            shapes.clear()
            drawShapes()
        }

        private fun loadPreferences(prefs: SharedPreferences) {
            // Check if colours have changed. If they have, notify Android system.
            val newShapeColour = prefs.getInt(getString(R.string.shapeColour), defaultShapeColour)
            val newBackgroundColour =
                prefs.getInt(getString(R.string.backgroundColour), defaultBackgroundColour)
            if ((newShapeColour != shapeColour) or (newBackgroundColour != backgroundColour)) {
                notifyColorsChanged()
            }
            shapeColour = newShapeColour
            backgroundColour = newBackgroundColour


            randomShapeSpawningEnabled =
                prefs.getBoolean(
                    getString(R.string.enableRandomShapeSpawning),
                    defaultRandomShapeSpawningEnabled
                )
            maxCount =
                Integer.valueOf(
                    prefs.getString(getString(R.string.numberOfShapes), defaultMaxCount.toString())
                        .toString()
                )
            randomShapeSpawnDelay = Integer.valueOf(
                prefs.getString(
                    getString(R.string.randomShapeSpawnDelay),
                    defaultRandomShapeDelay.toString()
                ).toString()
            )
            shapeType = prefs.getString(getString(R.string.shapeType), defaultShapeType).toString()
            pauseRandomShapesWhenDragging = prefs.getBoolean(
                getString(R.string.pauseRandomShapesWhenDragging),
                defaultPauseRandomShapesWhenDragging
            )
            smoothDrawingEnabled =
                prefs.getBoolean(
                    getString(R.string.smoothDrawingEnabled),
                    defaultSmoothDrawingEnabled
                )
            randomShapeColoursEnabled =
                prefs.getBoolean(
                    getString(R.string.randomShapeColoursEnabled),
                    defaultRandomShapeColoursEnabled
                )
            randomShapeTypesEnabled =
                prefs.getBoolean(
                    getString(R.string.randomShapeTypeEnabled),
                    defaultRandomShapeTypesEnabled
                )
            enableTouchInteraction =
                prefs.getBoolean(
                    getString(R.string.enableTouchInteraction),
                    enableTouchInteractionDefault
                )
            shapeSize =
                Integer.valueOf(
                    prefs.getString(getString(R.string.shapeSize), defaultShapeSize.toString())
                        .toString()
                ).toFloat()
            randomShapeSizesEnabled =
                prefs.getBoolean(
                    getString(R.string.randomShapeSizesEnabled),
                    defaultRandomShapeSizesEnabled
                )
            randomShapeRotationEnabled =
                prefs.getBoolean(
                    getString(R.string.randomShapeRotationEnabled),
                    defaultRandomShapeRotationEnabled
                )
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
                            "square" ->
                                drawPolygon(
                                    canvas,
                                    paint,
                                    x - size / 2,
                                    y - size / 2,
                                    size,
                                    4f,
                                    45f,
                                    false
                                )
                            "triangle" -> drawPolygon(
                                canvas,
                                paint,
                                x - size / 2,
                                y + size / 2,
                                size,
                                3f,
                                270f, // Point up
                                false,

                                )
                            "pentagon" -> drawPolygon(
                                canvas,
                                paint,
                                x - size / 2,
                                y + size / 2,
                                size,
                                5f,
                                270f, // Point up
                                false,

                                )
                            "hexagon" -> drawPolygon(
                                canvas,
                                paint,
                                x - size / 2,
                                y + size / 2,
                                size,
                                6f,
                                270f, // Point up
                                false,

                                )
                            else -> canvas.drawCircle(x, y, size, paint)
                        }
                    }
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawPolygon(
            mCanvas: Canvas,
            paint: Paint,
            x: Float,
            y: Float,
            radius: Float,
            sides: Float,
            startAngle: Float,
            anticlockwise: Boolean,
        ) {
            // From https://stackoverflow.com/a/36792553
            if (sides < 3) {
                return
            }
            val a = Math.PI.toFloat() * 2 / sides * if (anticlockwise) -1 else 1
            mCanvas.save()
            mCanvas.translate(x, y)
            mCanvas.rotate(startAngle)
            val path = Path()
            path.moveTo(radius, 0f)
            var i = 1
            while (i < sides) {
                path.lineTo(
                    radius * Math.cos((a * i).toDouble()).toFloat(),
                    radius * Math.sin((a * i).toDouble()).toFloat()
                )
                i++
            }
            path.close()
            mCanvas.drawPath(path, paint)
            mCanvas.restore()
        }
    }

}