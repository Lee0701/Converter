package io.github.lee0701.converter.candidates

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.WindowManager
import androidx.preference.PreferenceManager

abstract class CandidatesWindow(context: Context) {

    protected val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    protected val customWindowColor =
        preferences.getInt("custom_window_color", CandidateWindowColor.DEFAULT)
    protected val windowColor =
        preferences.getString("window_color", "default").let { CandidateWindowColor.of(it ?: "", customWindowColor) }
    protected val darkMode = preferences.getString("window_color", "default")?.contains("dark") ?: false
    protected val textAlpha = if(darkMode) CandidateWindowColor.ALPHA_DARK else CandidateWindowColor.ALPHA_LIGHT
    protected val textColor = CandidateWindowColor.textColorOf(windowColor)
    protected val extraColor = CandidateWindowColor.extraColorOf(windowColor)

    protected val showExtra = preferences.getBoolean("show_extra", true)

    abstract fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (String) -> Unit)
    abstract fun destroy()

    data class Candidate(
        val text: String,
        val extra: String = "",
    )

}