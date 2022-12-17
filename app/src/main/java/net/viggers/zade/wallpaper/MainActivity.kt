package net.viggers.zade.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        val installButton = findViewById<Button>(R.id.installButton)
        val prefsButton = findViewById<Button>(R.id.prefsButton)
        val clearShapesButton = findViewById<Button>(R.id.clearShapesButton)
        val downloadsPageButton = findViewById<Button>(R.id.downloadsPageButton)

        val context = this
        installButton.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, WallpaperService::class.java)
            )
            startActivity(intent)
        }
        prefsButton.setOnClickListener {
            val intent = Intent(context, PreferencesActivity::class.java)
            startActivity(intent)
        }
        clearShapesButton.setOnClickListener {
            // Send a broadcast to clear the shapes
            val intent = Intent(getString(R.string.action_remove_all_shapes))
            sendBroadcast(intent)
        }
        downloadsPageButton.setOnClickListener {
            goToUrl("https://github.com/zadeviggers/wallpaper/releases")
        }
    }
    private fun goToUrl(url: String) {
        val uriUrl: Uri = Uri.parse(url)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }

}