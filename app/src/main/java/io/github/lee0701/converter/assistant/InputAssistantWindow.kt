package io.github.lee0701.converter.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.InputAssistantViewBinding
import java.lang.IllegalArgumentException

class InputAssistantWindow(private val context: Context) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    private var binding: InputAssistantViewBinding? = null

    private val type =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    private val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    private val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320f, context.resources.displayMetrics).toInt(),
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240f, context.resources.displayMetrics).toInt(),
        0, 0,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
        // Required for keyboard to show initially
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
    }

    fun show(onAccept: (CharSequence) -> Unit) {
        if(this.binding == null) {
            val binding = InputAssistantViewBinding.inflate(LayoutInflater.from(context))
            binding.close.setOnClickListener { hide() }
            binding.paste.setOnClickListener { onAccept(binding.text.text) }

            this.binding = binding
        }
        // Close notification panel
        val intent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        context.sendBroadcast(intent)

        val binding = this.binding ?: return
        try {
            windowManager.addView(binding.root, layoutParams)
        } catch(ex: WindowManager.BadTokenException) {
            Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
        binding.text.requestFocus()
    }

    fun hide() {
        try {
            binding?.root?.let { windowManager.removeView(it) }
        } catch (ex: IllegalArgumentException) {}
    }

}