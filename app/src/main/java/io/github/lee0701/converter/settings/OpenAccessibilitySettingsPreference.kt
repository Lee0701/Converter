package io.github.lee0701.converter.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import androidx.preference.Preference

class OpenAccessibilitySettingsPreference(context: Context?, attrs: AttributeSet?): Preference(
    context,
    attrs
) {
    override fun onClick() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        )
        context.startActivity(intent)
    }
}