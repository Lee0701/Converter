package io.github.lee0701.converter.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class ColorEditTextPreference(context: Context?, attrs: AttributeSet?): EditTextPreference(context, attrs) {
    override fun persistString(value: String?): Boolean {
        if(value == null) return false
        return super.persistInt(value.replace("#", "").toUInt(16).toInt())
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return "#" + super.getPersistedInt(0).toUInt().toString(16).padStart(8, '0')
    }
}