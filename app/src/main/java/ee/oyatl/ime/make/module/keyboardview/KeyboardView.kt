package ee.oyatl.ime.make.module.keyboardview

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.R
import ee.oyatl.ime.make.preset.softkeyboard.Key
import ee.oyatl.ime.make.preset.softkeyboard.KeyType
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import ee.oyatl.ime.make.preset.softkeyboard.Spacer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class KeyboardView(
    context: Context,
    attrs: AttributeSet?,
    protected val keyboard: Keyboard,
    protected val theme: Theme,
    protected val listener: KeyboardListener,
    rowHeight: Int,
    private val disableTouch: Boolean = false,
): FrameLayout(context, attrs) {

    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val rect = Rect()

    open val keyboardWidth: Int = context.resources.displayMetrics.widthPixels
    open val keyboardHeight: Int = rowHeight * keyboard.rows.size
    open val shrinkWidth: Float = 1.0f

    protected val typedValue = TypedValue()

    private val screenType = pref.getString("layout_screen_type", null) ?: "mobile"
    protected val showKeyPopups: Boolean = pref.getBoolean("appearance_show_popups", true)
    protected val showMoreKeys: Boolean = pref.getBoolean("appearance_show_more_keys", true)
    protected val longPressDuration: Long = pref.getFloat("behaviour_long_press_duration", 100f).toLong()
    protected val longPressAction: FlickLongPressAction = FlickLongPressAction.of(
        pref.getString("behaviour_long_press_action", "shift") ?: "shift"
    )
    protected val repeatInterval: Long = pref.getFloat("behaviour_repeat_interval", 50f).toLong()

    protected val slideAction = pref.getString("behaviour_slide_action", "flick")
    protected val flickSensitivity = dipToPixel(pref.getFloat("behaviour_flick_sensitivity", 100f)).toInt()

    protected val hapticFeedback = pref.getBoolean("appearance_haptic_feedback", true)
    protected val soundFeedback = pref.getBoolean("appearance_sound_feedback", true)
    protected val soundFeedbackVolume = pref.getFloat("appearance_sound_feedback_volume", 100f) / 100f

    protected val pointers: MutableMap<Int, TouchPointer> = mutableMapOf()
    protected val keyStates: MutableMap<Key, Boolean> = mutableMapOf()
    private var popups: MutableMap<Int, KeyboardPopup> = mutableMapOf()

    protected abstract val wrappedKeys: List<RowItemWrapper>
    protected val moreKeysKeyboards: MutableMap<Int, Keyboard> = mutableMapOf()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(disableTouch) return false
        if(event == null) return super.onTouchEvent(null)

        val pointerId = event.getPointerId(event.actionIndex)
        val x = event.getX(event.actionIndex).roundToInt()
        val y = event.getY(event.actionIndex).roundToInt()

        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val key = findKey(x, y) ?: return true
                onTouchDown(key, pointerId, x, y)
                postViewChanged()
            }
            MotionEvent.ACTION_MOVE -> {
                val key = pointers[pointerId]?.key ?: findKey(x, y) ?: return true
                onTouchMove(key, pointerId, x, y)
                postViewChanged()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val key = pointers[pointerId]?.key ?: findKey(x, y) ?: return true
                onTouchUp(key, pointerId, x, y)
                postViewChanged()
            }
        }
        return true
    }

    protected fun onTouchDown(key: KeyWrapper, pointerId: Int, x: Int, y: Int) {
        if(this.hapticFeedback) this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        if(this.soundFeedback) this.performSoundFeedback(key.key.code)
        maybeShowPreviewPopup(key, pointerId)
        fun repeater() {
            listener.onKeyClick(key.key.code, key.key.output)
            handler?.postDelayed({ repeater() }, repeatInterval)
        }
        handler?.postDelayed({
            if(key.key.repeatable || longPressAction == FlickLongPressAction.Repeat) {
                repeater()
            } else if(showMoreKeys) {
                if(maybeShowMoreKeysPopup(key, pointerId, x, y)) return@postDelayed
            }
            if(this.hapticFeedback && longPressAction != FlickLongPressAction.None) {
                this.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            if(key.key.code == KeyEvent.KEYCODE_SHIFT_LEFT
                || key.key.code == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                return@postDelayed
            }
            listener.onKeyLongClick(key.key.code, key.key.output)
        }, longPressDuration)

        listener.onKeyDown(key.key.code, key.key.output)
        if(key.key.code == KeyEvent.KEYCODE_ENTER)
            wrappedKeys.filterIsInstance<KeyWrapper>().forEach { if(it.key.code == key.key.code) keyStates[it.key] = true }
        else keyStates[key.key] = true
        val pointer = TouchPointer(x, y, key)
        pointers += pointerId to pointer
    }

    protected fun onTouchMove(key: KeyWrapper, pointerId: Int, x: Int, y: Int) {
        val pointer = pointers[pointerId] ?: return
        val popup = popups[pointerId]
        if(popup is MoreKeysPopup) {
            val parentLeft = key.x + key.width/2f - popup.width/2f + popup.offsetX
            val parentRight = parentLeft + popup.width

            // Correct touch positions when view has pushed from outside of screen bounds
            val correctedLeft = parentLeft - max(0f, parentLeft)
            val correctedRight = parentRight - min(keyboardWidth.toFloat(), parentRight)

            val parentY = key.y + key.height/2f - popup.height/2f + popup.offsetY
            val localX = x - parentLeft + correctedLeft + correctedRight
            val localY = y - parentY + popup.height/3f*2f
            popup.touchMove(localX.roundToInt(), localY.roundToInt())
            return
        }

        val dx = abs(pointer.initialX - x)
        val dy = abs(pointer.initialY - y)

        val direction = if(dx > flickSensitivity && dx > dy) {
            if(x < pointer.initialX) FlickDirection.Left
            else FlickDirection.Right
        } else if(dy > flickSensitivity && dy > dx) {
            if(y < pointer.initialY) FlickDirection.Up
            else FlickDirection.Down
        } else FlickDirection.None

        if(slideAction == "flick"
            && direction != FlickDirection.None
            && pointer.flickDirection == FlickDirection.None
        ) {
            handler.removeCallbacksAndMessages(null)
            onFlick(direction, pointer.key, pointerId, x, y)
            pointers[pointerId] = pointer.copy(flickDirection = direction)

        } else if(slideAction == "seek" && key.key.code !in setOf(KeyEvent.KEYCODE_DEL)) {

            if(x !in key.x until key.x+key.width
                || y !in key.y until key.y+key.height) {

                val newKey = findKey(x, y) ?: key
                if(newKey.key != key.key) {
                    handler.removeCallbacksAndMessages(null)

                    if(showMoreKeys) handler.postDelayed({
                        maybeShowMoreKeysPopup(newKey, pointerId, x, y)
                    }, longPressDuration)

                    keyStates[key.key] = false
                    keyStates[newKey.key] = true
                    pointers[pointerId] = pointer.copy(key = newKey)
                    popups[pointerId]?.cancel()
                    maybeShowPreviewPopup(newKey, pointerId)
                }
            }
        }
    }

    protected fun onTouchUp(key: KeyWrapper, pointerId: Int, x: Int, y: Int) {
        handler.removeCallbacksAndMessages(null)
        val popup = popups[pointerId]
        if(popup is MoreKeysPopup) {
            popup.touchUp()
            popup.dismiss()
        } else {
            popup?.dismiss()
            listener.onKeyClick(key.key.code, key.key.output)
            listener.onKeyUp(key.key.code, key.key.output)
        }
        performClick()
        if(key.key.code == KeyEvent.KEYCODE_ENTER)
            wrappedKeys.filterIsInstance<KeyWrapper>().forEach { if(it.key.code == key.key.code) keyStates[it.key] = false }
        else keyStates[key.key] = false
        pointers -= pointerId
    }

    protected fun onFlick(flickDirection: FlickDirection, key: KeyWrapper, pointerId: Int, x: Int, y: Int) {
        listener.onKeyFlick(flickDirection, key.key.code, key.key.output)
    }

    fun updateMoreKeysKeyboards(keyboards: Map<Int, Keyboard>) {
        moreKeysKeyboards.clear()
        moreKeysKeyboards += keyboards
    }

    abstract fun updateLabelsAndIcons(labels: Map<Int, CharSequence>, icons: Map<Int, Int>)
    abstract fun postViewChanged()
    abstract fun highlight(key: KeyWrapper)

    fun findKey(x: Int, y: Int): KeyWrapper? {
        wrappedKeys.forEach { key ->
            if(x in key.x until key.x + key.width) {
                if(y in key.y until key.y + key.height) {
                    if(key is KeyWrapper) return key
                }
            }
        }
        return null
    }

    private fun performSoundFeedback(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val fx = when(keyCode) {
            KeyEvent.KEYCODE_DEL -> AudioManager.FX_KEYPRESS_DELETE
            KeyEvent.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
            KeyEvent.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        audioManager.playSoundEffect(fx, soundFeedbackVolume)
    }

    private fun dipToPixel(dip: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.resources.displayMetrics)
    }

    private fun maybeShowPreviewPopup(key: KeyWrapper, pointerId: Int) {
        if(showKeyPopups &&
            (key.key.type == KeyType.Alphanumeric || key.key.type == KeyType.AlphanumericAlt)) {
            val keyPopup = KeyPreviewPopup(context, key)
            popups[pointerId]?.cancel()
            popups[pointerId] = keyPopup
            showPopup(key, keyPopup, 0, 0)
        } else {
            popups[pointerId]?.cancel()
            popups.remove(pointerId)
        }
    }

    private fun maybeShowMoreKeysPopup(key: KeyWrapper, pointerId: Int, x: Int, y: Int): Boolean {
        val result = showMoreKeysPopup(key, pointerId)
        // Call this once to initially point a key on popup
        handler?.post { onTouchMove(key, pointerId, x, y) }
        return result
    }

    private fun showMoreKeysPopup(key: KeyWrapper, pointerId: Int): Boolean {
        val charId = listener.onMoreKeys(key.key.code, key.key.output)
        val keyId = key.key.moreKeys
        val keyboard = moreKeysKeyboards[charId] ?: moreKeysKeyboards[keyId] ?: return false
        val keyPopup = MoreKeysPopup(context, key, keyboard, listener)
        popups[pointerId]?.cancel()
        popups[pointerId] = keyPopup
        val popupX = getPopupX(key)
        val popupY = getPopupY(key)
        // Correct popup position if outside the screen
        val offsetX = if(popupX < 0) -popupX else 0f
        val offsetY = 0f
        showPopup(key, keyPopup, offsetX.roundToInt(), offsetY.roundToInt())
        return true
    }

    private fun showPopup(key: KeyWrapper, popup: KeyboardPopup, offsetX: Int, offsetY: Int) {
        getGlobalVisibleRect(rect)
        val x = getPopupX(key) + offsetX + rect.left
        val y = getPopupY(key) + offsetY + rect.top
        popup.show(this, x.roundToInt(), y.roundToInt())
    }

    private fun getPopupX(key: KeyWrapper): Float = key.x + key.width/2f
    private fun getPopupY(key: KeyWrapper): Float = key.y + key.height/2f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(keyboardWidth, keyboardHeight)
    }

    interface RowItemWrapper {
        val x: Int
        val y: Int
        val width: Int
        val height: Int
    }

    interface KeyWrapper: RowItemWrapper {
        val key: Key
        val label: String?
        val icon: Drawable?
    }

    interface SpacerWrapper: RowItemWrapper {
        val spacer: Spacer
    }

    data class TouchPointer(
        val initialX: Int,
        val initialY: Int,
        val key: KeyWrapper,
        val flickDirection: FlickDirection = FlickDirection.None,
    )

}