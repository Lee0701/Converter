package io.github.lee0701.converter.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.lee0701.converter.ConverterAccessibilityService
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.ActivitySettingsBinding
import io.github.lee0701.converter.settings.preference.OpenAccessibilitySettingsPreference
import io.github.lee0701.converter.settings.preference.ShowCandidateWindowAdjusterPreference

class SettingsActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            ConverterAccessibilityService.INSTANCE?.restartService()
        }
    }

    override fun onStart() {
        super.onStart()

        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if(!list.any { it.id.startsWith(packageName) }) {
            Snackbar.make(binding.root, R.string.accessibility_service_not_enabled, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.enable) { OpenAccessibilitySettingsPreference.openAccessibilitySettings(this) }
                .show()
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
            R.xml.pref_behavior,
            R.xml.pref_about,
        )
    }
}