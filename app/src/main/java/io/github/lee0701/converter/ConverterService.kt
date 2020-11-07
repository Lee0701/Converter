package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.android.synthetic.main.candidates_view.view.*
import android.view.WindowManager.LayoutParams.*

class ConverterService: AccessibilityService() {

    val rect = Rect()
    var candidatesView: View? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if(event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        val word = event.text.firstOrNull()?.split("\\s".toRegex())?.lastOrNull()
        if(word != null) {
            onWord(word)
        }
    }

    fun onWord(word: String) {
        if(word == "asdf") {
            showWindow(listOf("aoeu"))
        } else {
            destroyWindow()
        }
    }

    fun showWindow(candidates: List<String>) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView == null) {
            candidatesView = LayoutInflater.from(this).inflate(R.layout.candidates_view, null)
            val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) TYPE_APPLICATION_OVERLAY else TYPE_SYSTEM_ALERT
            val flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL
            val params = WindowManager.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT,
                rect.left, rect.top,
                type, flags,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.LEFT
            windowManager.addView(candidatesView, params)
        }
        val view = candidatesView ?: return
        view.text.text = candidates.firstOrNull() ?: ""
    }

    fun destroyWindow() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
    }

    override fun onInterrupt() {
    }
}