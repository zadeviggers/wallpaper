package net.viggers.zade.wallpaper;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        addPreferencesFromResource(R.xml.prefs);

        Preference circlePreference = getPreferenceScreen().findPreference("numberOfCircles");

        circlePreference.setOnPreferenceChangeListener(numberCheckListener);
    }

    Preference.OnPreferenceChangeListener numberCheckListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if (o != null && o.toString().length() > 0 && o.toString().matches("\\d*")) {
                return true;
            }

            Toast.makeText(
                    PreferencesActivity.this,
                    R.string.invalid_number_input_toast,
                    Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
    };
}
