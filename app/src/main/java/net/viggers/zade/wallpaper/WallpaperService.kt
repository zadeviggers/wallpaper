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
import kotlin.math.cos
import kotlin.math.sin


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
                Log.v("ZV-Wallpaper:Engine", "Preferences changed")
                loadPreferences(newPrefs)
                // Re-render wallpaper, in case thing such as background colour were changed.
                drawWallpaper()
            }

        // By default we just want all the shape types.
        private val defaultShapeTypes: Set<String> = resources.getStringArray(R.array.shape_types).toSet()
        private val defaultMaxCount: Int = R.integer.numberOfShapesDefault
        private val defaultRandomShapeSpawningEnabled: Boolean =
            R.bool.enableRandomShapeSpawningDefault == 1
        private val defaultRandomShapeDelay: Int = R.integer.randomShapeSpawnDelayDefault
        private val defaultShapeColour: Int = getColor(R.color.shapeColourDefault)
        private val defaultBackgroundColour: Int = getColor(R.color.backgroundColourDefault)
        private val defaultPauseRandomShapesWhenDragging: Boolean =
            R.bool.pauseRandomShapesWhenDraggingDefault == 1
        private val defaultRandomShapeColoursEnabled: Boolean =
            R.bool.randomShapeColoursEnabledDefault == 1
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
        private var enabledShapeTypes: Set<String> = defaultShapeTypes
        private var pauseRandomShapesWhenDragging: Boolean = defaultPauseRandomShapesWhenDragging
        private var randomShapeColoursEnabled: Boolean = defaultRandomShapeColoursEnabled
        private var enableTouchInteraction: Boolean = enableTouchInteractionDefault
        private var shapeSize: Float = defaultShapeSize
        private var randomShapeSizesEnabled: Boolean = defaultRandomShapeSizesEnabled
        private var randomShapeRotationEnabled: Boolean = defaultRandomShapeRotationEnabled


        private var randomShapesDraggingCoolDown: Int = 5


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
                if (enabledShapeTypes.isEmpty()) return defaultShapeTypes.random()
                return enabledShapeTypes.random()
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

            Log.v("ZV-Wallpaper:Engine", "Loaded wallpaper preferences")

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

        override fun onComputeColors(): WallpaperColors {
            return WallpaperColors(
                Color.valueOf(backgroundColour),
                Color.valueOf(nextShapeColour),
                Color.valueOf(nextShapeColour)
            )
        }

        fun clearAllShapes() {
            shapes.clear()
            // Re-draw the wallpaper with the shapes gone
            drawWallpaper()
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
            enabledShapeTypes = prefs.getStringSet(getString(R.string.shapeType), defaultShapeTypes) as Set<String>
            pauseRandomShapesWhenDragging = prefs.getBoolean(
                getString(R.string.pauseRandomShapesWhenDragging),
                defaultPauseRandomShapesWhenDragging
            )
            randomShapeColoursEnabled =
                prefs.getBoolean(
                    getString(R.string.randomShapeColoursEnabled),
                    defaultRandomShapeColoursEnabled
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
                // Re-draw everything when it becomes visible
                drawWallpaper()
            } else {
                handler.removeCallbacks(drawRunner)
            }
        }


        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunner)
        }

        // Getting the surface to stick the canvas to draw on
        override fun onSurfaceChanged(
            holder: SurfaceHolder, format: Int,
            width: Int, height: Int
        ) {
            this.width = width
            this.height = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        // Shape drawing
        override fun onTouchEvent(event: MotionEvent) {
            if (enableTouchInteraction) {
                val x = event.x
                val y = event.y

                val type = nextShapeType

                addShape(
                    Shape(
                        nextShapeId,
                        x,
                        y,
                        getAngle(if (type == "square") 45f else null),
                        nextShapeSize,
                        nextShapeColour,
                        type,
                        true
                    )
                )

                if (pauseRandomShapesWhenDragging) {
                    randomShapesDraggingCoolDown = 5
                }
            }

            super.onTouchEvent(event)
        }

        private fun addShape(shape: Shape) {

            // Need to do this so that if the users lowers their shape limit, there aren't too many shapes on screen
            while ((shapes.size >= maxCount)) {
                shapes.removeFirst()
            }
            shapes.add(shape)

            // Re-draw the wallpaper
            drawWallpaper()
        }

        // Ticks for random shape spawning
        private fun drawTick() {
            if (randomShapeSpawningEnabled) {
                var shouldAddShape = true

                if (pauseRandomShapesWhenDragging and (randomShapesDraggingCoolDown > 0)) {
                    shouldAddShape = false
                    randomShapesDraggingCoolDown -= 1
                }

                if (shouldAddShape) {
                    val x = (width * Math.random()).toFloat()
                    val y = (height * Math.random()).toFloat()
                    val type = nextShapeType
                    addShape(
                        Shape(
                            nextShapeId,
                            x,
                            y,
                            getAngle(if (type == "square") 45f else null), // Square needs 45deg angle to point up
                            nextShapeSize,
                            nextShapeColour,
                            type,
                            false,
                        )
                    )
                }
            }

            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.postDelayed(drawRunner, randomShapeSpawnDelay.toLong())
            }
        }

        private fun getAngle(
            defaultAngle: Float?
        ): Float {
            if (randomShapeRotationEnabled) {
                return (0..360).random().toFloat()
            }
            return defaultAngle ?: 270f // This makes most shapes point up.

        }

        // Surface view requires that all elements are drawn completely
        private fun drawWallpaper() {
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

                        val angle = shape.angle

                        val size = shape.size

                        when (shape.type) {
                            "circle" -> canvas.drawCircle(x, y, size, paint)
                            "triangle" -> drawPolygon(
                                canvas,
                                paint,
                                x,
                                y,
                                size,
                                3f,
                                angle
                            )
                            "square" ->
                                drawPolygon(
                                    canvas,
                                    paint,
                                    x,
                                    y,
                                    size,
                                    4f,
                                    angle
                                )
                            "pentagon" -> drawPolygon(
                                canvas,
                                paint,
                                x,
                                y,
                                size,
                                5f,
                                angle
                            )
                            "hexagon" -> drawPolygon(
                                canvas,
                                paint,
                                x,
                                y,
                                size,
                                6f,
                                angle
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
        ) {
            // From https://stackoverflow.com/a/36792553
            if (sides < 3) {
                return
            }
            val a = Math.PI.toFloat() * 2 / sides
            mCanvas.save()
            mCanvas.translate(x, y)
            mCanvas.rotate(startAngle)
            val path = Path()
            path.moveTo(radius, 0f)
            var i = 1
            while (i < sides) {
                path.lineTo(
                    radius * cos((a * i).toDouble()).toFloat(),
                    radius * sin((a * i).toDouble()).toFloat()
                )
                i++
            }
            path.close()
            mCanvas.drawPath(path, paint)
            mCanvas.restore()
        }
    }
}