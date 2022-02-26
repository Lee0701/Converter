package io.github.lee0701.converter.settings.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.EditTextPreference
import io.github.lee0701.converter.ConverterAccessibilityService
import io.github.lee0701.converter.R
import io.github.lee0701.converter.candidates.view.HorizontalCandidatesWindowAdjuster

class ShowCandidateWindowAdjusterPreference(context: Context, attrs: AttributeSet?): EditTextPreference(context, attrs) {

    private var adjuster: HorizontalCandidatesWindowAdjuster? = null

    override fun onClick() {
        closeAdjuster()
        val service = ConverterAccessibilityService.INSTANCE
        if(service != null) {
            super.onClick()
            adjuster = HorizontalCandidatesWindowAdjuster(service).apply { show() }
        } else {
            Toast.makeText(context, R.string.accessibility_service_required, Toast.LENGTH_LONG).show()
        }
    }
    fun closeAdjuster() {
        adjuster?.close()
        adjuster = null
    }

    override fun getText(): String {
        return context.resources.getString(R.string.adjust_window_information)
    }

}