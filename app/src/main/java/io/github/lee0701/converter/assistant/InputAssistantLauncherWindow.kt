package io.github.lee0701.converter.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import io.github.lee0701.converter.R

abstract class InputAssistantLauncherWindow(
    val context: Context,
) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    abstract val layoutParams: WindowManager.LayoutParams
    abstract val view: View?

    var xPos: Int = 0
    var yPos: Int = 0

    open fun show(onClick: () -> Unit) {
        hide()
        try {
            windowManager.addView(view ?: return, layoutParams)
        } catch(ex: WindowManager.BadTokenException) {
            Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    open fun hide() {
        try {
            windowManager.removeView(view ?: return)
        } catch(ex: IllegalArgumentException) {}
    }

    open fun updateLayout() {
        try {
            windowManager.updateViewLayout(view ?: return, layoutParams)
        } catch(ex: IllegalArgumentException) {}
    }

}