package io.github.lee0701.converter.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.EditTextPreference
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.R
import io.github.lee0701.converter.candidates.HorizontalCandidateWindowAdjuster

class ShowCandidateWindowAdjusterPreference(context: Context?, attrs: AttributeSet?): EditTextPreference(context, attrs) {

    private var adjuster: HorizontalCandidateWindowAdjuster? = null

    override fun onClick() {
        closeAdjuster()
        val service = ConverterService.INSTANCE
        if(service != null) {
            super.onClick()
            adjuster = HorizontalCandidateWindowAdjuster(service).apply { show() }
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