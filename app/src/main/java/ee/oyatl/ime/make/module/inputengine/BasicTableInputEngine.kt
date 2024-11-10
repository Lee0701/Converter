package ee.oyatl.ime.make.module.inputengine

import android.view.KeyEvent
import io.github.lee0701.converter.R
import ee.oyatl.ime.make.modifiers.ModifierKeyHandler
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import ee.oyatl.ime.make.module.keyboardview.Theme
import ee.oyatl.ime.make.preset.softkeyboard.KeyIconType
import ee.oyatl.ime.make.preset.table.CharOverrideTable
import ee.oyatl.ime.make.preset.table.CodeConvertTable
import ee.oyatl.ime.make.preset.table.MoreKeysTable

abstract class BasicTableInputEngine(
    override val convertTable: CodeConvertTable,
    override val overrideTable: CharOverrideTable,
    override val moreKeysTable: MoreKeysTable,
    override val shiftKeyHandler: ModifierKeyHandler,
    override val listener: InputEngine.Listener,
): BasicInputEngine(), TableInputEngine {
    override var alternativeInputEngine: InputEngine? = null
    override var symbolsInputEngine: InputEngine? = null

    override fun onKey(code: Int, modifiers: ModifierKeyStateSet) {
        val converted = convertTable.get(code, modifiers)
            ?: keyCharacterMap.get(code, modifiers.asMetaState())
        val override = overrideTable.get(converted) ?: converted
        if(override > 0) listener.onCommitText(override.toChar().toString())
    }

    override fun onDelete() {
        listener.onDeleteText(1, 0)
    }

    override fun onTextAroundCursor(before: String, after: String) {
    }

    override fun onReset() {
        listener.onFinishComposing()
    }

    override fun getLabels(state: ModifierKeyStateSet): Map<Int, CharSequence> {
        val codeMap = convertTable.getAllForState(state)
            .mapValues { (_, code) -> overrideTable.get(code) ?: code }
            .mapValues { (_, code) -> code.toChar().toString() }
        return DirectInputEngine.getLabels(keyCharacterMap, state) + codeMap
    }

    override fun getIcons(state: ModifierKeyStateSet, theme: Theme): Map<Int, Int> {
        val shift = when {
            state.shift.locked -> theme.keyIcon[KeyIconType.ShiftLocked]
            state.shift.active -> theme.keyIcon[KeyIconType.ShiftPressed]
            else -> theme.keyIcon[KeyIconType.Shift]
        }?.resource ?: R.drawable.keyic_shift
        return mapOf(
            KeyEvent.KEYCODE_SHIFT_LEFT to shift,
            KeyEvent.KEYCODE_SHIFT_RIGHT to shift
        )
    }
}