package ee.oyatl.ime.make.module.keyboardview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.R
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import kotlin.math.roundToInt

class MoreKeysPopup(
    context: Context,
    key: KeyboardView.KeyWrapper,
    val keyboard: Keyboard,
    val listener: KeyboardListener,
): KeyboardPopup(context, key), KeyboardListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val wrappedContext = ContextThemeWrapper(context, R.style.Theme_Converter_Keyboard_KeyPopup)

    private val keyWidth: Float = wrappedContext.resources.getDimension(R.dimen.key_popup_morekeys_width)
    private val keyHeight: Float = wrappedContext.resources.getDimension(R.dimen.key_popup_morekeys_height)

    private val maxRows: Int = (keyboard.rows.maxOfOrNull { it.keys.size } ?: 1)

    override val offsetX: Int = if(maxRows % 2 == 0) keyWidth.roundToInt()/2 else 0
    override val offsetY: Int = 0
    private val keyMarginHorizontal = context.resources.getDimension(R.dimen.key_margin_horizontal)
    private val keyMarginVertical = context.resources.getDimension(R.dimen.key_margin_vertical)

    private val keyboardWidth = (maxRows * keyWidth) + keyMarginHorizontal*2
    private val keyboardHeight = (keyboard.rows.size * keyHeight) + keyMarginVertical*2

    override val width: Int = keyboardWidth.roundToInt()
    override val height: Int = keyboardHeight.roundToInt()

    val theme = Themes.ofName(preferences.getString("appearance_theme", "theme_dynamic"))
    private val keyboardViewType = preferences.getString("appearance_keyboard_view_type", "canvas") ?: "canvas"
    private val keyboardView: KeyboardView =
        when(keyboardViewType) {
            "stacked_view" -> StackedViewKeyboardView(
                context, null, keyboard, theme, this, keyHeight.roundToInt())
            else -> CanvasKeyboardView(
                context, null, keyboard, theme, this, keyHeight.roundToInt(), width, height)
        }
    private var pointedKey: KeyboardView.KeyWrapper? = null

    private val animator: Animator = ValueAnimator.ofFloat(1f, 0f).apply {
        addUpdateListener {
            val value = animatedValue as Float
            popupWindow.background.alpha = (value * 256).toInt()
            (keyboardView as View).alpha = value
        }
        addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                popupWindow.dismiss()
                popupWindow.background.alpha = 255
                (keyboardView as View).alpha = 1f
            }
        })
    }

    override fun show(parent: View, parentX: Int, parentY: Int) {
        popupWindow.apply {
            this.contentView = keyboardView as ViewGroup
            this.width = this@MoreKeysPopup.width
            this.height = this@MoreKeysPopup.height
            keyboardView.layoutParams = ViewGroup.LayoutParams(width, height)
            this.isClippingEnabled = true
            this.isTouchable = false
            this.elevation = wrappedContext.resources.getDimension(R.dimen.key_popup_elevation)
            val drawable = ContextCompat.getDrawable(wrappedContext, R.drawable.key_popup_bg)
            if(drawable != null) {
                val typedValue = TypedValue()
                wrappedContext.theme.resolveAttribute(R.attr.backgroundTint, typedValue, true)
                val backgroundTint = ContextCompat.getColor(wrappedContext, typedValue.resourceId)
                val backgroundDrawable = DrawableCompat.wrap(drawable)
                DrawableCompat.setTint(backgroundDrawable.mutate(), backgroundTint)
                this.setBackgroundDrawable(backgroundDrawable)
            }
        }

        val x = parentX - popupWindow.width/2f + offsetX
        val y = parentY - popupWindow.height - keyHeight + offsetY

        popupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, x.roundToInt(), y.roundToInt())
    }

    override fun onKeyClick(code: Int, output: String?) {
        listener.onKeyClick(code, output)
    }

    override fun onKeyLongClick(code: Int, output: String?) {
    }

    override fun onKeyDown(code: Int, output: String?) {
    }

    override fun onKeyUp(code: Int, output: String?) {
    }

    override fun onKeyFlick(direction: FlickDirection, code: Int, output: String?) {
    }

    override fun onMoreKeys(code: Int, output: String?): Int? {
        return null
    }

    override fun touchMove(x: Int, y: Int) {
        val pointX = x
        val pointY = y - keyHeight.roundToInt()
        val pointedKey = keyboardView.findKey(pointX, pointY)
        if(pointedKey != null && this.pointedKey != pointedKey) {
            keyboardView.highlight(pointedKey)
            this.pointedKey = pointedKey
        }
    }

    override fun touchUp() {
        val pointedKey = pointedKey ?: return
        this.onKeyClick(pointedKey.key.code, pointedKey.key.output)
        this.pointedKey = null
    }

    override fun dismiss() {
        animator.start()
    }

    override fun cancel() {
        animator.cancel()
        popupWindow.dismiss()
    }
}