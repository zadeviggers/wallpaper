package net.viggers.zade.wallpaper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat


class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        if (savedInstance == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PreferencesFragment())
                .commit()
        }
    }

    class PreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)

            val maxShapeNumberPreference = preferenceScreen.findPreference<EditTextPreference>("numberOfShapes")
            maxShapeNumberPreference?.onPreferenceChangeListener = numberCheckListener

            val shapeSpawnDelayPreference = preferenceScreen.findPreference<EditTextPreference>("randomShapeSpawnDelay")
            shapeSpawnDelayPreference?.onPreferenceChangeListener = numberCheckListener
        }

        private var numberCheckListener = androidx.preference.Preference.OnPreferenceChangeListener { _, o ->
            if (o != null && o.toString().isNotEmpty() && o.toString().matches(Regex("\\d*"))) {
                return@OnPreferenceChangeListener true
            }
            Toast.makeText(
                activity,
                R.string.invalid_number_input_toast,
                Toast.LENGTH_SHORT
            )
                .show()
            false
        }
    }
}