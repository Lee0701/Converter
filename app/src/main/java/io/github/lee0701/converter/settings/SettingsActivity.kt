package io.github.lee0701.converter.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayShowHomeEnabled(false)

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener { preferences, key ->
                when(key) {
                    "custom_window_color" -> {
                        preferences.edit().putInt("custom_window_color_text", preferences.getInt(key, 0)).apply()
                    }
                    "custom_window_color_text" -> {
                        preferences.edit().putInt("custom_window_color", preferences.getInt(key, 0)).apply()
                    }
                }
                ConverterService.INSTANCE?.restartService()
            }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPause() {
            super.onPause()
            val preference = preferenceScreen.findPreference<ShowCandidateWindowAdjusterPreference>("adjust_window")
            preference?.closeAdjuster()
        }
    }
}