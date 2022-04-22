package net.viggers.zade.wallpaper

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)

        if (savedInstance == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PreferencesFragment())
                .commit()

            supportActionBar?.setDisplayShowHomeEnabled(true);
            supportActionBar?.setDisplayHomeAsUpEnabled(true);
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class PreferencesFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
            val pref = findPreference<Preference>(key)
            if (null != pref) {
                val value = sharedPreferences?.getString(pref.key, "")
                setPreferenceSummary(pref, value)
            }

        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs, rootKey)

            val maxShapeNumberPreference = preferenceScreen.findPreference<EditTextPreference>("numberOfShapes")
            maxShapeNumberPreference?.onPreferenceChangeListener = numberCheckListener

            val shapeSpawnDelayPreference = preferenceScreen.findPreference<EditTextPreference>("randomShapeSpawnDelay")
            shapeSpawnDelayPreference?.onPreferenceChangeListener = numberCheckListener

            for (i in 0 until preferenceScreen.preferenceCount) {
                val pref = preferenceScreen.getPreference(i)
                val value = preferenceScreen.sharedPreferences?.getString(pref.key, "")
                setPreferenceSummary(pref, value)
            }

            val listPref = findPreference<ListPreference>("shapeType")
            listPref?.summary = listPref?.entry
        }

        private fun setPreferenceSummary(pref: Preference?, value: String?) {
            if (pref is ListPreference) {
                val listPreference: ListPreference = pref
                val prefIndex = listPreference.findIndexOfValue(value)
                if (prefIndex >=0) {
                    listPreference.summary = listPreference.entries[prefIndex]
                }
            }

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