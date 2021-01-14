package io.github.lee0701.converter.settings

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.preference.Preference
import io.github.lee0701.converter.candidates.HorizontalCandidateWindowAdjuster

class ShowCandidateWindowAdjusterPreference(context: Context?, attrs: AttributeSet?): Preference(context, attrs) {
    override fun onClick() {
        HorizontalCandidateWindowAdjuster(context).show()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}