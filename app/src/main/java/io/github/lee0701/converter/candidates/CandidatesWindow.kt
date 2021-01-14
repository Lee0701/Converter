package io.github.lee0701.converter.candidates

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.view.WindowManager

abstract class CandidatesWindow(context: Context) {

    protected val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    abstract fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (String) -> Unit)
    abstract fun destroy()

    data class Candidate(
        val text: String,
        val extra: String
    )

}