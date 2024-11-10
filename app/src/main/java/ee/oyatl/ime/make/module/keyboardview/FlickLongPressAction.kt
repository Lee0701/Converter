package ee.oyatl.ime.make.module.keyboardview

import ee.oyatl.ime.make.module.inputengine.InputEngine
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet

sealed interface FlickLongPressAction {
    fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine)

    data object None: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
        }
    }

    data object MoreKeys: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
        }
    }

    data object Repeat: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
            // This action will be intercepted and be processed by Soft Keyboard
        }
    }

    data object Shifted: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
            inputEngine.onKey(code, makeShiftOn(modifiers))
        }
    }

    data object Symbols: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
            inputEngine.onReset()
            inputEngine.symbolsInputEngine?.onKey(code, modifiers)
        }
    }

    data object ShiftedSymbols: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
            inputEngine.onReset()
            inputEngine.symbolsInputEngine?.onKey(code, makeShiftOn(modifiers))
        }
    }

    data object AlternativeLanguage: FlickLongPressAction {
        override fun onKey(code: Int, modifiers: ModifierKeyStateSet, inputEngine: InputEngine) {
            inputEngine.onReset()
            inputEngine.alternativeInputEngine?.onKey(code, modifiers)
        }
    }

    companion object {
        fun of(value: String): FlickLongPressAction {
            return when(value) {
                "repeat" -> Repeat
                "symbol" -> Symbols
                "shift" -> Shifted
                "shift_symbol" -> ShiftedSymbols
                "alternative_language" -> AlternativeLanguage
                else -> None
            }
        }

        fun makeShiftOn(modifierKeyStateSet: ModifierKeyStateSet): ModifierKeyStateSet
                = modifierKeyStateSet.copy(shift = modifierKeyStateSet.shift.copy(pressed = true))
    }
}