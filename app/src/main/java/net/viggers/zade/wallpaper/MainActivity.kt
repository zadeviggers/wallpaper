package net.viggers.zade.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// Compat stuff
fun PackageManager.getInstallingPackageNameCompat(packageName: String): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstallSourceInfo(packageName).installingPackageName
    } else {
       @Suppress("DEPRECATION") getInstallerPackageName(packageName)
    }

fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0));
    }else{
        @Suppress("DEPRECATION") getApplicationInfo(packageName, PackageManager.GET_META_DATA);
    }
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
        val githubPageButton = findViewById<Button>(R.id.githubPageButton)

        val versionText = findViewById<TextView>(R.id.app_version_text)
        val installLocationText = findViewById<TextView>(R.id.app_install_location_text)

        versionText.text = getString(R.string.version_text, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

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
                    ).show()
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

        githubPageButton.setOnClickListener {
            goToUrl("https://github.com/zadeviggers/wallpaper")
        }

        val installerName = packageManager.getInstallingPackageNameCompat(packageName);
        Log.v("ZV-Wallpaper:AppHome", installerName.toString())

        if (installerName != null) {
            val installerInfo = packageManager.getApplicationInfoCompat(installerName);
            val installerLabel = packageManager.getApplicationLabel(installerInfo)
            installLocationText.text = getString(R.string.installed_text_with_app, installerLabel, installerName)
            downloadsPageButton.setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(installerName)
                startActivity(launchIntent)
            }
            downloadsPageButton.text = getString(R.string.downloads_page_button_text_template, installerLabel)
            downloadsPageButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.store_icon, 0,0,0)
        } else {
            installLocationText.text = getString(R.string.installed_text_manually)
            downloadsPageButton.setOnClickListener {
                goToUrl("https://github.com/zadeviggers/wallpaper/releases")
            }
        }
    }

    private fun goToUrl(url: String) {
        val uriUrl: Uri = Uri.parse(url)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }

}