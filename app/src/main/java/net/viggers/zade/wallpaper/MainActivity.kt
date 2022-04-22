package net.viggers.zade.wallpaper

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import net.viggers.zade.wallpaper.R
import android.content.Intent
import android.app.WallpaperManager
import android.content.ComponentName
import android.view.View

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
    }

    fun onInstallClick(view: View?) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, WallpaperService::class.java)
        )
        startActivity(intent)
    }

    fun onPrefsClick(view: View?) {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }
}