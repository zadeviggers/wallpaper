package net.viggers.zade.wallpaper

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        if (savedInstance == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PreferencesFragment()).commit()

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class PreferencesFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)

            val maxShapeNumberPreference =
                preferenceScreen.findPreference<EditTextPreference>(getString(R.string.numberOfShapes))
            maxShapeNumberPreference?.onPreferenceChangeListener = numberCheckListener

            val shapeSpawnDelayPreference =
                preferenceScreen.findPreference<EditTextPreference>(getString(R.string.randomShapeSpawnDelay))
            shapeSpawnDelayPreference?.onPreferenceChangeListener = numberCheckListener

            val shapeSizePreference =
                preferenceScreen.findPreference<EditTextPreference>(getString(R.string.shapeSize))
            shapeSizePreference?.onPreferenceChangeListener = numberCheckListener

            val shapeTypesPreference =
                preferenceScreen.findPreference<MultiSelectListPreference>(getString(R.string.shapeType))
            shapeTypesPreference?.onPreferenceChangeListener = multiSelectAtLeastOneListener
        }

        private var numberCheckListener = Preference.OnPreferenceChangeListener { _, o ->
            if ((o != null) and o.toString().isNotEmpty() and o.toString().matches(Regex("\\d*"))) {
                val num = Integer.valueOf(o.toString())
                if (num >= 1000000) {
                    Toast.makeText(
                        activity, R.string.number_too_big_input_toast, Toast.LENGTH_LONG
                    ).show()
                    return@OnPreferenceChangeListener false
                }
                return@OnPreferenceChangeListener true
            }
            Toast.makeText(
                activity, R.string.invalid_number_input_toast, Toast.LENGTH_LONG
            ).show()
            return@OnPreferenceChangeListener false
        }

        private var multiSelectAtLeastOneListener = Preference.OnPreferenceChangeListener { p, o ->
            if (p !is MultiSelectListPreference) {
                Log.v(
                    "ZV-Wallpaper:Preferences",
                    "Non-multiselect preference triggered multiselect change handler."
                )
                return@OnPreferenceChangeListener false
            }


            if ((o as Set<String>).isNotEmpty()) {
                return@OnPreferenceChangeListener true
            }

            Log.v(
                "ZV-Wallpaper:Preferences",
                "No items selected in MultiSelect."
            )
            Toast.makeText(
                activity, R.string.none_selected_multiselect_input_toast, Toast.LENGTH_LONG
            ).show()
            return@OnPreferenceChangeListener false
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?, key: String?
        ) {
            // Nothing to do here, but Kotlin complains if I don't have this here.
        }
    }
}