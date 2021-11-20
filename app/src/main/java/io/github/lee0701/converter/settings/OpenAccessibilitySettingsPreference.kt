package io.github.lee0701.converter.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.information.InformationActivity

class OpenAccessibilitySettingsPreference(context: Context?, attrs: AttributeSet?): Preference(
    context,
    attrs
) {
    override fun onClick() {
        openAccessibilitySettings(context)
    }
    companion object {
        fun openAccessibilitySettings(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            if(!preferences.getBoolean("accessibility_service_agreed", false)) {
                // User has not agreed to use accessibility service, show information window
                val intent = Intent(context, InformationActivity::class.java)
                context.startActivity(intent)
            } else {
                // Already agreed, Open accessibility service settings
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                context.startActivity(intent)
            }
        }
    }
}