package io.github.lee0701.converter.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.InputAssistantLauncherViewBinding
import java.lang.IllegalArgumentException

class InputAssistantLauncherWindow(
    private val context: Context,
) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    private var binding: InputAssistantLauncherViewBinding? = null

    var xPos: Int = 0
    var yPos: Int = 0

    private val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    private val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        xPos, yPos,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
    }

    fun show(onClick: () -> Unit) {
        if(binding == null) {
            binding = InputAssistantLauncherViewBinding.inflate(LayoutInflater.from(context)).apply {
                close.setOnClickListener { hide() }
                root.setOnClickListener { onClick() }
            }
        }
        hide()
        try {
            windowManager.addView(binding?.root ?: return, layoutParams)
        } catch(ex: WindowManager.BadTokenException) {
            Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    fun hide() {
        try {
            windowManager.removeView(binding?.root ?: return)
        } catch(ex: IllegalArgumentException) {}
    }

    fun updateLayout() {
        try {
            windowManager.updateViewLayout(binding?.root ?: return, layoutParams)
        } catch(ex: IllegalArgumentException) {}
    }

}