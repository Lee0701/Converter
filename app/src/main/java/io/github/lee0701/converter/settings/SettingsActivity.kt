package io.github.lee0701.converter.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.R
import io.github.lee0701.converter.information.InformationActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        if(!preferences.getBoolean("accessibility_service_agreed", false)) {
            startActivity(Intent(this, InformationActivity::class.java))
            finish()
        }

        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayShowHomeEnabled(false)

        preferences.registerOnSharedPreferenceChangeListener { preferences, key ->
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