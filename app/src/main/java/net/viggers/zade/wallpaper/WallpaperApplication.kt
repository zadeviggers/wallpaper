package net.viggers.zade.wallpaper

import android.app.Application
import com.google.android.material.color.DynamicColors

class WallpaperApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}