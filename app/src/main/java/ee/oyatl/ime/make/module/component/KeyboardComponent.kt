package ee.oyatl.ime.make.module.component

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.preference.PreferenceManager
import ee.oyatl.ime.make.modifiers.ModifierKeyState
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import ee.oyatl.ime.make.module.inputengine.InputEngine
import ee.oyatl.ime.make.module.inputengine.TableInputEngine
import ee.oyatl.ime.make.module.keyboardview.CanvasKeyboardView
import ee.oyatl.ime.make.module.keyboardview.FlickDirection
import ee.oyatl.ime.make.module.keyboardview.FlickLongPressAction
import ee.oyatl.ime.make.module.keyboardview.KeyboardListener
import ee.oyatl.ime.make.module.keyboardview.KeyboardView
import ee.oyatl.ime.make.module.keyboardview.StackedViewKeyboardView
import ee.oyatl.ime.make.module.keyboardview.Theme
import ee.oyatl.ime.make.module.keyboardview.Themes
import ee.oyatl.ime.make.preset.softkeyboard.Key
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import ee.oyatl.ime.make.preset.table.CustomKeyCode

class KeyboardComponent(
    val keyboard: Keyboard,
    val rowHeight: Int,
    val direct: Boolean = false,
    private val disableTouch: Boolean = false,
): InputViewComponent, KeyboardListener {
    var connectedInputEngine: InputEngine? = null

    private var keyboardViewType: String = "canvas"

    private var longPressAction: FlickLongPressAction = FlickLongPressAction.Shifted
    private var flickUpAction: FlickLongPressAction = FlickLongPressAction.Shifted
    private var flickDownAction: FlickLongPressAction = FlickLongPressAction.Symbols
    private var flickLeftAction: FlickLongPressAction = FlickLongPressAction.None
    private var flickRightAction: FlickLongPressAction = FlickLongPressAction.None

    private var theme: Theme = Themes.Static
    private var keyboardView: KeyboardView? = null

    private var _modifiers: ModifierKeyStateSet = ModifierKeyStateSet()
    private val modifiers: ModifierKeyStateSet
        get() = _modifiers.copy(shift = connectedInputEngine?.shiftKeyHandler?.state ?: _modifiers.shift)
    private var ignoreCode: Int = 0

    override fun initView(context: Context): View? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        keyboardViewType = pref.getString("appearance_keyboard_view_type", "canvas") ?: keyboardViewType
        longPressAction = FlickLongPressAction.of(
            pref.getString("behaviour_long_press_action", "shift") ?: "shift"
        )
        flickUpAction = FlickLongPressAction.of(
            pref.getString("behaviour_flick_action_up", "shift") ?: "shift"
        )
        flickDownAction = FlickLongPressAction.of(
            pref.getString("behaviour_flick_action_down", "symbol") ?: "symbol"
        )
        flickLeftAction = FlickLongPressAction.of(
            pref.getString("behaviour_flick_action_left", "none") ?: "none"
        )
        flickRightAction = FlickLongPressAction.of(
            pref.getString("behaviour_flick_action_", "none") ?: "none"
        )

        val name = pref.getString("appearance_theme", "theme_dynamic")
        val theme = Themes.ofName(name)
        keyboardView = when(keyboardViewType) {
            "stacked_view" -> StackedViewKeyboardView(context, null, keyboard, theme, this, rowHeight, disableTouch)
            else -> CanvasKeyboardView(context, null, keyboard, theme, this, rowHeight, disableTouch = disableTouch)
        }
        this.theme = theme
        return keyboardView
    }

    override fun reset() {
    }

    override fun updateView() {
        updateLabelsAndIcons()
        updateMoreKeys()
        keyboardView?.invalidate()
    }

    private fun getShiftedLabels(shiftState: ModifierKeyState): Map<Int, CharSequence> {
        fun label(label: String) =
            if(shiftState.pressed || shiftState.locked) label.uppercase()
            else label.lowercase()
        return keyboard.rows.flatMap { it.keys }
            .filterIsInstance<Key>()
            .associate { it.code to label(it.label.orEmpty()) }
    }

    private fun updateLabelsAndIcons(labels: Map<Int, CharSequence>, icons: Map<Int, Int>) {
        keyboardView?.updateLabelsAndIcons(labels, icons)
    }

    private fun updateLabelsAndIcons() {
        val inputEngine = connectedInputEngine ?: return
        updateLabelsAndIcons(
            getShiftedLabels(modifiers.shift) + inputEngine.getLabels(modifiers),
            inputEngine.getIcons(modifiers, theme)
        )
    }

    private fun updateMoreKeys() {
        val inputEngine = connectedInputEngine ?: return
        if(inputEngine is TableInputEngine) {
            val keyboardView = keyboardView ?: return
            keyboardView.updateMoreKeysKeyboards(inputEngine.moreKeysTable.map)
        }
    }

    override fun onKeyDown(code: Int, output: String?) {
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                connectedInputEngine?.shiftKeyHandler?.onPress()
                connectedInputEngine?.updateView()
            }
        }
    }

    override fun onKeyUp(code: Int, output: String?) {
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                connectedInputEngine?.shiftKeyHandler?.onRelease()
                connectedInputEngine?.updateView()
            }
        }
    }

    override fun onKeyClick(code: Int, output: String?) {
        val inputEngine = connectedInputEngine ?: return
        if(ignoreCode != 0 && ignoreCode == code) {
            ignoreCode = 0
            return
        }
        when(code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
            }
            KeyEvent.KEYCODE_CAPS_LOCK -> {
                connectedInputEngine?.shiftKeyHandler?.onLock()
            }
            else -> {
                val standard = inputEngine.keyCharacterMap.isPrintingKey(code)
                val custom = CustomKeyCode.entries.find { it.code == code }?.type == CustomKeyCode.Type.PRINTING
                if(standard || custom) {
                    onPrintingKey(code, output)
                } else if(output != null) {
                    onPrintingKey(code, output)
                } else {
                    if(!inputEngine.listener.onNonPrintingKey(code)) {
                        inputEngine.listener.onDefaultAction(code)
                    }
                }
            }
        }
        inputEngine.updateView()
    }

    override fun onKeyLongClick(code: Int, output: String?) {
        val inputEngine = connectedInputEngine ?: return
        longPressAction.onKey(code, modifiers, inputEngine)
        ignoreCode = code
        onInput()
    }

    private fun onPrintingKey(code: Int, output: String?) {
        val inputEngine = connectedInputEngine ?: return
        if(code == 0 && output != null) {
            inputEngine.listener.onCommitText(output)
        } else if(code != 0) {
            inputEngine.onKey(code, modifiers)
        }
        onInput()
    }

    override fun onKeyFlick(direction: FlickDirection, code: Int, output: String?) {
        val inputEngine = connectedInputEngine ?: return
        val action = when(direction) {
            FlickDirection.Up -> flickUpAction
            FlickDirection.Down -> flickDownAction
            FlickDirection.Left -> flickLeftAction
            FlickDirection.Right -> flickRightAction
            else -> FlickLongPressAction.None
        }
        action.onKey(code, modifiers, inputEngine)
        ignoreCode = code
        onInput()
    }

    override fun onMoreKeys(code: Int, output: String?): Int? {
        val inputEngine = connectedInputEngine ?: return null
        if(inputEngine is TableInputEngine) {
            return inputEngine.convertTable.get(code, modifiers)
        }
        return null
    }

    private fun onInput() {
        connectedInputEngine?.shiftKeyHandler?.onInput()
    }
}