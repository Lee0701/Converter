package io.github.lee0701.converter.candidates

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.R
import kotlinx.android.synthetic.main.adjust_candidates_view_horizontal.view.*

class HorizontalCandidateWindowAdjuster(private val context: Context) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var windowY = preferences.getInt("horizontal_window_y", 500)
    private var windowHeight = preferences.getInt("horizontal_window_height", 200)

    private val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    private val layoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, windowHeight,
        0, windowY,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private val windowMoveAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics).toInt()

    @SuppressLint("InflateParams")
    fun show() {
        val window = LayoutInflater.from(context).inflate(R.layout.adjust_candidates_view_horizontal, null)
        window.close.setOnClickListener {
            windowManager.removeView(window)
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
                    windowManager.updateViewLayout(window, layoutParams)
                }
                else -> return@setOnTouchListener false
            }
            return@setOnTouchListener true
        }

        window.up.setOnClickListener {
            windowY -= windowMoveAmount
            windowManager.updateViewLayout(window, layoutParams)
        }
        window.down.setOnClickListener {
            windowY += windowMoveAmount
            windowManager.updateViewLayout(window, layoutParams)
        }

        window.larger.setOnClickListener {
            windowHeight += windowMoveAmount
            windowManager.updateViewLayout(window, layoutParams)
        }
        window.smaller.setOnClickListener {
            windowHeight -= windowMoveAmount
            windowManager.updateViewLayout(window, layoutParams)
        }

        window.save.setOnClickListener {
            val editor = preferences.edit()
            editor.putInt("horizontal_window_y", windowY)
            editor.putInt("horizontal_window_height", windowHeight)
            editor.apply()
        }

        window.discard.setOnClickListener {
            windowY = preferences.getInt("horizontal_window_y", windowY)
            windowHeight = preferences.getInt("horizontal_window_height", windowHeight)
            windowManager.updateViewLayout(window, layoutParams)
        }

        windowManager.addView(window, layoutParams)
    }

}