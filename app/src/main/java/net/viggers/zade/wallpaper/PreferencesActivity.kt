package net.viggers.zade.wallpaper

import android.preference.PreferenceActivity
import android.os.Bundle
import net.viggers.zade.wallpaper.R
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.widget.Toast

class PreferencesActivity : PreferenceActivity() {
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        addPreferencesFromResource(R.xml.prefs)
        val circlePreference = preferenceScreen.findPreference("numberOfCircles")
        circlePreference.onPreferenceChangeListener = numberCheckListener
    }

    private var numberCheckListener = OnPreferenceChangeListener { _, o ->
        if (o != null && o.toString().isNotEmpty() && o.toString().matches(Regex("\\d*"))) {
            return@OnPreferenceChangeListener true
        }
        Toast.makeText(
            this@PreferencesActivity,
            R.string.invalid_number_input_toast,
            Toast.LENGTH_SHORT
        )
            .show()
        false
    }
}