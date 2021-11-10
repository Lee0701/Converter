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

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayShowHomeEnabled(false)

        preferences.registerOnSharedPreferenceChangeListener { pref, key ->
            when(key) {
                "custom_window_color" -> {
                    pref.edit().putInt("custom_window_color_text", pref.getInt(key, 0)).apply()
                }
                "custom_window_color_text" -> {
                    pref.edit().putInt("custom_window_color", pref.getInt(key, 0)).apply()
                }
            }
            ConverterService.INSTANCE?.restartService()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            PREFERENCE_LIST.forEach { addPreferencesFromResource(it) }
        }

        override fun onPause() {
            super.onPause()
            val preference = preferenceScreen.findPreference<ShowCandidateWindowAdjusterPreference>("adjust_window")
            preference?.closeAdjuster()
        }
    }

    companion object {
        val PREFERENCE_LIST = listOf(
            R.xml.pref_service,
            R.xml.pref_conversion,
            R.xml.pref_prediction,
            R.xml.pref_visuals,
            R.xml.pref_about,
        )
    }
}