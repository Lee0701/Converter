package ee.oyatl.ime.make.module.inputengine

import android.content.Context
import android.view.KeyCharacterMap
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import ee.oyatl.ime.make.module.component.InputViewComponent

abstract class BasicInputEngine(
    override var components: List<InputViewComponent> = listOf(),
    override val keyCharacterMap: KeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
): InputEngine {

    override fun initView(context: Context): View? {
        val view = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            components.forEach { addView(it.initView(context)) }
        }
        return view
    }

    override fun updateView() {
        components.forEach { component -> component.updateView() }
    }

    override fun onResetComponents() {
        components.forEach { it.reset() }
    }

    override fun onKey(code: Int, modifiers: ModifierKeyStateSet) {
        val char = keyCharacterMap.get(code, modifiers.asMetaState())
        if(char > 0) listener.onCommitText(char.toChar().toString())
    }
}