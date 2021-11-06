package io.github.lee0701.converter.candidates

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.ConverterService
import io.github.lee0701.converter.candidates.HorizontalCandidatesWindow.Key
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.AdjustCandidatesViewHorizontalBinding
import java.lang.IllegalArgumentException

class HorizontalCandidateWindowAdjuster(private val context: Context) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val landscape get() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val keyWindowY get() = if(landscape) Key.Y_LANDSCAPE else Key.Y_PORTRAIT
    private val keyWindowHeight get() = if(landscape) Key.HEIGHT_LANDSCAPE else Key.HEIGHT_PORTRAIT
    private var windowY = preferences.getInt(keyWindowY, 500)
    private var windowHeight = preferences.getInt(keyWindowHeight, 200)

    private val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    private val layoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, windowHeight,
        0, windowY,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private val windowMoveAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics).toInt()

    private var window: AdjustCandidatesViewHorizontalBinding? = null

    @SuppressLint("InflateParams")
    fun show() {
        val window = AdjustCandidatesViewHorizontalBinding.inflate(LayoutInflater.from(context))
        window.close.setOnClickListener {
            close()
        }

        var offset = 0
        window.move.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.performClick()
                    offset = motionEvent.rawY.toInt() - windowY
                }
                MotionEvent.ACTION_MOVE -> {
                    windowY = motionEvent.rawY.toInt() - offset
                    windowManager.updateViewLayout(window.root, layoutParams)
                }
                else -> return@setOnTouchListener false
            }
            return@setOnTouchListener true
        }

        window.up.setOnClickListener {
            windowY -= windowMoveAmount
            windowManager.updateViewLayout(window.root, layoutParams)
        }
        window.down.setOnClickListener {
            windowY += windowMoveAmount
            windowManager.updateViewLayout(window.root, layoutParams)
        }

        window.larger.setOnClickListener {
            windowHeight += windowMoveAmount
            windowManager.updateViewLayout(window.root, layoutParams)
        }
        window.smaller.setOnClickListener {
            windowHeight -= windowMoveAmount
            windowManager.updateViewLayout(window.root, layoutParams)
        }

        window.save.setOnClickListener {
            val editor = preferences.edit()
            editor.putInt(keyWindowY, windowY)
            editor.putInt(keyWindowHeight, windowHeight)
            editor.apply()
            Toast.makeText(context, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            ConverterService.INSTANCE?.restartService()
        }

        window.discard.setOnClickListener {
            windowY = preferences.getInt(keyWindowY, windowY)
            windowHeight = preferences.getInt(keyWindowHeight, windowHeight)
            windowManager.updateViewLayout(window.root, layoutParams)
        }

        try {
            windowManager.addView(window.root, layoutParams)
        } catch(ex: WindowManager.BadTokenException) {
            Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
        }
        this.window = window
    }

    fun close() {
        val window = this.window
        try {
            window?.root?.let { windowManager.removeView(it) }
        } catch(ex: IllegalArgumentException) {
            // Window is already removed, do nothing
        }
    }

}