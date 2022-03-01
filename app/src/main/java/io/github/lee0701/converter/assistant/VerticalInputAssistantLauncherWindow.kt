package io.github.lee0701.converter.assistant

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import io.github.lee0701.converter.databinding.InputAssistantLauncherViewVerticalBinding

class VerticalInputAssistantLauncherWindow(
    context: Context,
): InputAssistantLauncherWindow(context) {

    private var binding: InputAssistantLauncherViewVerticalBinding? = null
    override val view: View? get() = binding?.root

    var xPos: Int = 0
    var yPos: Int = 0

    private val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    override val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        xPos, yPos,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.LEFT
    }

    override fun show(onClick: () -> Unit) {
        if(binding == null) {
            binding = InputAssistantLauncherViewVerticalBinding.inflate(LayoutInflater.from(context)).apply {
                close.setOnClickListener { hide() }
                root.setOnClickListener { onClick() }
            }
        }
        super.show(onClick)
    }

}