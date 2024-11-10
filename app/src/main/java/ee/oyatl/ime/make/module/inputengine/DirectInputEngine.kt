package ee.oyatl.ime.make.module.inputengine

import android.view.KeyCharacterMap
import ee.oyatl.ime.make.modifiers.ModifierKeyHandler
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import ee.oyatl.ime.make.module.keyboardview.Theme

class DirectInputEngine(
    override val listener: InputEngine.Listener,
    override var shiftKeyHandler: ModifierKeyHandler
): BasicInputEngine() {
    override var alternativeInputEngine: InputEngine? = null
    override var symbolsInputEngine: InputEngine? = null

    override fun onDelete() {
        listener.onDeleteText(1, 0)
    }

    override fun onTextAroundCursor(before: String, after: String) {
    }

    override fun onReset() {
        listener.onFinishComposing()
    }

    override fun getLabels(state: ModifierKeyStateSet): Map<Int, CharSequence> {
        return getLabels(keyCharacterMap, state)
    }

    override fun getIcons(state: ModifierKeyStateSet, theme: Theme): Map<Int, Int> {
        return emptyMap()
    }

    companion object {
        fun getLabels(keyCharacterMap: KeyCharacterMap, state: ModifierKeyStateSet): Map<Int, CharSequence> {
            val range = 0 .. 304
            return range.map { keyCode -> keyCode to keyCharacterMap.get(keyCode, state.asMetaState()) }
                .mapNotNull { (keyCode, label) -> (if(label == 0) null else label)?.let { keyCode to it } }.toMap()
                .mapValues { (_, label) -> label.toChar().toString() }
        }
    }
}