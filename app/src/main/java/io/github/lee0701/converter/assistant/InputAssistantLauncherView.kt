package io.github.lee0701.converter.assistant

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.GestureDetectorCompat
import io.github.lee0701.converter.ConverterTileService

@RequiresApi(Build.VERSION_CODES.N)
class InputAssistantLauncherView(
    context: Context,
    attributeSet: AttributeSet?,
): FrameLayout(context, attributeSet), GestureDetector.OnGestureListener {

    private val gestureDetector = GestureDetectorCompat(context, this)

    private var xPos: Float = 0f
    private var yPos: Float = 0f

    private var downXPos: Float = 0f
    private var downYPos: Float = 0f

    private val type =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    private val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, context.resources.displayMetrics).toInt(),
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, context.resources.displayMetrics).toInt(),
        xPos.toInt(), yPos.toInt(),
        type, flags, PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if(gestureDetector.onTouchEvent(event)) true
        else super.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent?): Boolean {
        downXPos = xPos
        downYPos = yPos
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if(e1 != null && e2 != null) {
            xPos = downXPos + e2.rawX - e1.rawX
            yPos = downYPos + e2.rawY - e1.rawY
            ConverterTileService.INSTANCE?.updateLayout()
        }
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return true
    }
}