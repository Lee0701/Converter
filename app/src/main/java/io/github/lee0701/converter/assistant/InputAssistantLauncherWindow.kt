package io.github.lee0701.converter.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.InputAssistantLauncherViewBinding
import java.lang.IllegalArgumentException

@RequiresApi(Build.VERSION_CODES.N)
class InputAssistantLauncherWindow(private val context: Context) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager

    private val binding =  InputAssistantLauncherViewBinding.inflate(LayoutInflater.from(context))

    fun show() {
        try {
            windowManager.addView(binding.root, binding.root.layoutParams)
        } catch(ex: WindowManager.BadTokenException) {
            Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    fun destroy() {
        try {
            windowManager.removeView(binding.root)
        } catch(ex: IllegalArgumentException) {}
    }

    fun updateLayout() {
        try {
            windowManager.updateViewLayout(binding.root, binding.root.layoutParams)
        } catch(ex: IllegalArgumentException) {}
    }

}