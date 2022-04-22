package net.viggers.zade.wallpaper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.app.WallpaperManager
import android.content.ComponentName
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
    }

    // DO NOT REMOVE THIS PARAMETER, EVEN THOUGH IT'S NOT USED
    // Removing it makes the app crash when the button is clicked
    fun onInstallClick(view: View?) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, WallpaperService::class.java)
        )
        startActivity(intent)
    }

    // DO NOT REMOVE THIS PARAMETER, EVEN THOUGH IT'S NOT USED
    // Removing it makes the app crash when the button is clicked
    fun onPrefsClick(view: View?) {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }
}