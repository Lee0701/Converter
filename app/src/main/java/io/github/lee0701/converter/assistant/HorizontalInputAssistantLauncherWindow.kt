package io.github.lee0701.converter.assistant

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.candidates.view.CandidatesWindowColor
import io.github.lee0701.converter.candidates.view.HorizontalCandidatesWindow
import io.github.lee0701.converter.databinding.InputAssistantLauncherViewHorizontalBinding

class HorizontalInputAssistantLauncherWindow(
    context: Context,
): InputAssistantLauncherWindow(context) {

    private var binding: InputAssistantLauncherViewHorizontalBinding? = null
    override val view: View? get() = binding?.root

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    protected val customWindowColor =
        preferences.getInt("custom_window_color", CandidatesWindowColor.DEFAULT)
    protected val windowColor =
        preferences.getString("window_color", "default").let {
            CandidatesWindowColor.of(
                it ?: "",
                customWindowColor
            )
        }
    private val darkMode = preferences.getString("window_color", "default")?.contains("dark") ?: false
    private val textAlpha = if(darkMode) CandidatesWindowColor.ALPHA_DARK else CandidatesWindowColor.ALPHA_LIGHT
    private val textColor = CandidatesWindowColor.textColorOf(windowColor)

    private val landscape get() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private val windowY get() = preferences.getInt(if(landscape) HorizontalCandidatesWindow.Key.Y_LANDSCAPE else HorizontalCandidatesWindow.Key.Y_PORTRAIT, 500)
    private val windowHeight get() = preferences.getInt(if(landscape) HorizontalCandidatesWindow.Key.HEIGHT_LANDSCAPE else HorizontalCandidatesWindow.Key.HEIGHT_PORTRAIT, 200)

    private val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    override val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, windowHeight,
        0, windowY,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    override fun show(onClick: () -> Unit) {
        if(binding == null) {
            val binding = InputAssistantLauncherViewHorizontalBinding.inflate(LayoutInflater.from(context))

            binding.root.setBackgroundColor(windowColor)

            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, windowHeight)
            binding.closeWrapper.layoutParams = params

            binding.root.setOnClickListener { onClick() }
            binding.open.setTextColor(textColor)
            binding.open.alpha = textAlpha

            binding.close.setOnClickListener { hide() }
            binding.close.backgroundTintList = ColorStateList.valueOf(textColor)
            binding.close.alpha = textAlpha

            this.binding = binding
        }
        super.show(onClick)
    }

}