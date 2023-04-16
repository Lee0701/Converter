package io.github.lee0701.converter.candidates.view

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.WindowManager
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.library.engine.Candidate

abstract class CandidatesWindow(context: Context) {

    protected val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    protected val customWindowColor =
        preferences.getInt("custom_window_color", CandidatesWindowColor.DEFAULT)
    protected val windowColor =
        preferences.getString("window_color", "default").let {
            CandidatesWindowColor.of(
                it ?: "",
                customWindowColor
            )
        }
    protected val darkMode = preferences.getString("window_color", "default")?.contains("dark") ?: false
    protected val textAlpha = if(darkMode) CandidatesWindowColor.ALPHA_DARK else CandidatesWindowColor.ALPHA_LIGHT
    protected val textColor = CandidatesWindowColor.textColorOf(windowColor)
    protected val extraColor = CandidatesWindowColor.extraColorOf(windowColor)

    protected val showExtra = preferences.getBoolean("show_extra", true)

    abstract fun show(candidates: List<io.github.lee0701.converter.library.engine.Candidate>, rect: Rect, onItemClick: (io.github.lee0701.converter.library.engine.Candidate) -> Unit)
    abstract fun destroy()

}