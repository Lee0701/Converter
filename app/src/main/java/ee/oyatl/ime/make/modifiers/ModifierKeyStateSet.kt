package ee.oyatl.ime.make.modifiers

import android.view.KeyEvent

data class ModifierKeyStateSet(
    val shift: ModifierKeyState = ModifierKeyState(),
    val alt: ModifierKeyState = ModifierKeyState(),
    val control: ModifierKeyState = ModifierKeyState(),
    val meta: ModifierKeyState = ModifierKeyState()
) {
    fun asMetaState(): Int {
        return if(shift.active) KeyEvent.META_SHIFT_ON else 0 or
                if(alt.active) KeyEvent.META_ALT_ON else 0 or
                if(control.active) KeyEvent.META_CTRL_ON else 0 or
                if(meta.active) KeyEvent.META_META_ON else 0
    }

    companion object {
        val MODIFIER_KEYS = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT
        )
    }
}