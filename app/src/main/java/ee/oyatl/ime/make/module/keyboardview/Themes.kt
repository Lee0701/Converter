package ee.oyatl.ime.make.module.keyboardview

import com.google.android.material.color.DynamicColors
import io.github.lee0701.converter.R
import ee.oyatl.ime.make.preset.softkeyboard.KeyType.*
import ee.oyatl.ime.make.preset.softkeyboard.KeyIconType.*

object Themes {
    val Static = Theme(
        R.style.Theme_Converter_Keyboard,
        mapOf(
            Alphanumeric to R.style.Theme_Converter_Keyboard_Key,
            AlphanumericAlt to R.style.Theme_Converter_Keyboard_Key_Mod,
            Modifier to R.style.Theme_Converter_Keyboard_Key_Mod,
            ModifierAlt to R.style.Theme_Converter_Keyboard_Key,
            Space to R.style.Theme_Converter_Keyboard_Key,
            Action to R.style.Theme_Converter_Keyboard_Key_Return,
        ),
        mapOf(
            Shift to R.drawable.keyic_shift,
            ShiftPressed to R.drawable.keyic_shift_pressed,
            ShiftLocked to R.drawable.keyic_shift_locked,
            Caps to R.drawable.keyic_shift_lock,
            Option to R.drawable.keyic_option,
            Tab to R.drawable.keyic_tab,
            Backspace to R.drawable.keyic_backspace,
            Language to R.drawable.keyic_language,
            Return to R.drawable.keyic_return,
        ),
        R.style.Theme_Converter_Keyboard_KeyPopup
    )

    val Dynamic = Theme(
        R.style.Theme_Converter_Keyboard_Overlay,
        mapOf(
            Alphanumeric to R.style.Theme_Converter_Keyboard_Key_Overlay,
            AlphanumericAlt to R.style.Theme_Converter_Keyboard_Key_Mod_Overlay,
            Modifier to R.style.Theme_Converter_Keyboard_Key_Mod_Overlay,
            ModifierAlt to R.style.Theme_Converter_Keyboard_Key_Overlay,
            Space to R.style.Theme_Converter_Keyboard_Key_Overlay,
            Action to R.style.Theme_Converter_Keyboard_Key_Return_Overlay,
        ),
        mapOf(
            Shift to R.drawable.keyic_shift,
            ShiftPressed to R.drawable.keyic_shift_pressed,
            ShiftLocked to R.drawable.keyic_shift_locked,
            Caps to R.drawable.keyic_shift_lock,
            Option to R.drawable.keyic_option,
            Tab to R.drawable.keyic_tab,
            Backspace to R.drawable.keyic_backspace,
            Language to R.drawable.keyic_language,
            Return to R.drawable.keyic_return,
        ),
        R.style.Theme_Converter_Keyboard_KeyPopup_Overlay
    )

    val map: Map<String, Theme> = mapOf(
        "theme_static" to Static,
        "theme_dynamic" to Dynamic,
    )

    fun ofName(name: String?): Theme {
        return (map[name] ?: Static).let {
            if(!DynamicColors.isDynamicColorAvailable() && it == Dynamic) Static
            else it
        }
    }
}