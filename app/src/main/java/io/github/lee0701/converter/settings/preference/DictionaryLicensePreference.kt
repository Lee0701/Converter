package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.lee0701.converter.settings.DictionaryLicenseActivity

class DictionaryLicensePreference(context: Context, attrs: AttributeSet): Preference(context, attrs) {
    override fun onClick() {
        context.startActivity(Intent(context, DictionaryLicenseActivity::class.java))
        super.onClick()
    }
}