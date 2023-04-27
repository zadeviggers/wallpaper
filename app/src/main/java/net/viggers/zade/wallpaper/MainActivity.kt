package net.viggers.zade.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// From https://stackoverflow.com/a/74741495
// Uses old method on old android and new method on new android (api >= 33)
fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        val installButton = findViewById<Button>(R.id.installButton)
        val prefsButton = findViewById<Button>(R.id.prefsButton)
        val resetPrefsButton = findViewById<Button>(R.id.resetPrefsButton)
        val clearShapesButton = findViewById<Button>(R.id.clearShapesButton)
        val downloadsPageButton = findViewById<Button>(R.id.downloadsPageButton)
        val versionText = findViewById<TextView>(R.id.app_version_text)

        val pInfo =
            this.packageManager.getPackageInfoCompat(this.packageName, PackageManager.GET_ACTIVITIES)
        val version = pInfo.versionName
        versionText.text = getString(R.string.version_number_display).replace("%%version", version)

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
        resetPrefsButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.reset_prefs_confirm_dialog_title_text)
                .setMessage(R.string.reset_prefs_confirm_dialog_body_text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(
                    R.string.reset_prefs_confirm_button_text
                ) { _, _ ->
                    // Reset the preferences
                    val preferences =
                        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                    preferences.edit().clear().apply()
                    Toast.makeText(
                        this,
                        R.string.reset_prefs_completed_toast_text,
                        Toast.LENGTH_SHORT
                    ).show();
                }
                .setNegativeButton(R.string.reset_prefs_cancel_button_text, null)
                .show()
        }
        clearShapesButton.setOnClickListener {
            // Send a broadcast to clear the shapes
            val intent = Intent(getString(R.string.action_remove_all_shapes))
            sendBroadcast(intent)
            Toast.makeText(this, R.string.shapes_cleared_toast, Toast.LENGTH_SHORT)
                .show()
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