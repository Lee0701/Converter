package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import java.lang.NumberFormatException

class ColorEditTextPreference(context: Context, attrs: AttributeSet?): EditTextPreference(context, attrs) {
    override fun persistString(value: String?): Boolean {
        if(value == null) return false
        return try {
            super.persistInt(value.replace("#", "").toUInt(16).toInt())
        } catch(ex: NumberFormatException) {
            false
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return "#" + super.getPersistedInt(0).toUInt().toString(16).padStart(8, '0')
    }
}