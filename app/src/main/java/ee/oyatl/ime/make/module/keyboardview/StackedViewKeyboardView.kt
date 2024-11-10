package ee.oyatl.ime.make.module.keyboardview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import io.github.lee0701.converter.databinding.KeyboardBinding
import io.github.lee0701.converter.databinding.KeyboardKeyBinding
import io.github.lee0701.converter.databinding.KeyboardRowBinding
import io.github.lee0701.converter.databinding.KeyboardSpacerBinding
import ee.oyatl.ime.make.preset.softkeyboard.Key
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import ee.oyatl.ime.make.preset.softkeyboard.Row
import ee.oyatl.ime.make.preset.softkeyboard.Spacer
import kotlin.math.roundToInt

class StackedViewKeyboardView(
    context: Context,
    attrs: AttributeSet?,
    keyboard: Keyboard,
    theme: Theme,
    listener: KeyboardListener,
    rowHeight: Int,
    disableTouch: Boolean = false,
): KeyboardView(context, attrs, keyboard, theme, listener, rowHeight, disableTouch) {

    private val keyboardViewWrapper = initKeyboardView(keyboard, theme, listener)
    override val wrappedKeys: List<KeyWrapper> = keyboardViewWrapper.keys.toList()

    init {
        this.addView(keyboardViewWrapper.binding.root)
        this.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initKeyboardView(keyboard: Keyboard, theme: Theme, listener: KeyboardListener): KeyboardViewWrapper {
        val context = theme.keyboardBackground.wrapContext(context)
        val rowViewWrappers = mutableListOf<RowViewWrapper>()
        val binding = KeyboardBinding.inflate(LayoutInflater.from(context), null, false).apply {
            root.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT, keyboardHeight
            )
            keyboard.rows.forEach { row ->
                val rowViewBinding = initRowView(row, theme)
                rowViewWrappers += rowViewBinding
                root.addView(rowViewBinding.binding.root)
            }
        }
        return KeyboardViewWrapper(keyboard, binding, rowViewWrappers, rowViewWrappers.flatMap { it.keys })
    }

    private fun initRowView(row: Row, theme: Theme): RowViewWrapper {
        val wrappers = mutableListOf<RowItemViewWrapper>()
        val binding = KeyboardRowBinding.inflate(LayoutInflater.from(context), null, false).apply {
            root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0
            ).apply {
                weight = 1f
            }
            row.keys.forEach { rowItem ->
                when(rowItem) {
                    is Key -> {
                        val keyViewWrapper = initKeyView(rowItem, this, theme)
                        wrappers += keyViewWrapper
                        root.addView(keyViewWrapper.binding.root)
                    }
                    is Spacer -> {
                        val spacerViewWrapper = initSpacerView(rowItem)
                        wrappers += spacerViewWrapper
                        root.addView(spacerViewWrapper.binding.root)
                    }
                    else -> {}
                }
            }
        }
        return RowViewWrapper(row, binding, wrappers)
    }

    private fun initSpacerView(spacerModel: Spacer): SpacerViewWrapper {
        val binding = KeyboardSpacerBinding.inflate(LayoutInflater.from(context), null, false).apply {
            root.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = spacerModel.width
            }
        }
        return SpacerViewWrapper(spacerModel, binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initKeyView(keyModel: Key, row: KeyboardRowBinding, theme: Theme): KeyViewWrapper {
        val context = theme.keyBackground[keyModel.type]?.wrapContext(context)
        val binding = KeyboardKeyBinding.inflate(LayoutInflater.from(context), null, false).apply {
            val icon = theme.keyIcon[keyModel.iconType]
            if(keyModel.label != null) this.label.text = keyModel.label
            if(icon != null) this.icon.setImageResource(icon.resource)
            root.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = keyModel.width
            }
        }
        return KeyViewWrapper(keyModel, row, binding)
    }

    data class KeyboardViewWrapper(
        val keyboard: Keyboard,
        val binding: KeyboardBinding,
        val rows: List<RowViewWrapper>,
        val keys: List<KeyViewWrapper>,
    )

    data class RowViewWrapper(
        val row: Row,
        val binding: KeyboardRowBinding,
        val rowItems: List<RowItemViewWrapper>,
    ) {
        val keys: List<KeyViewWrapper> = rowItems.filterIsInstance<KeyViewWrapper>()
    }

    interface RowItemViewWrapper

    data class KeyViewWrapper(
        override val key: Key,
        private val row: KeyboardRowBinding,
        val binding: KeyboardKeyBinding,
    ): RowItemViewWrapper, KeyWrapper {
        override val x: Int get() = binding.root.x.roundToInt()
        override val y: Int get() = row.root.y.roundToInt()
        override val width: Int get() = binding.root.width
        override val height: Int get() = row.root.height
        override val label: String get() = binding.label.text.toString()
        override val icon: Drawable? get() = binding.icon.drawable
    }

    data class SpacerViewWrapper(
        override val spacer: Spacer,
        val binding: KeyboardSpacerBinding,
    ): RowItemViewWrapper, SpacerWrapper {
        override val x: Int get() = binding.root.x.roundToInt()
        override val y: Int get() = binding.root.y.roundToInt()
        override val width: Int get() = binding.root.width
        override val height: Int get() = binding.root.height
    }

    override fun updateLabelsAndIcons(labels: Map<Int, CharSequence>, icons: Map<Int, Int>) {
        keyboardViewWrapper.keys.forEach { key ->
            val icon = icons[key.key.code]
            val label = labels[key.key.code]
            if(icon != null) key.binding.icon.setImageResource(icon)
            if(label != null) key.binding.label.text = label
        }
    }

    override fun postViewChanged() {
        wrappedKeys.filterIsInstance<KeyViewWrapper>().forEach { key ->
            key.binding.root.isPressed = keyStates[key.key] == true
        }
    }

    override fun highlight(key: KeyWrapper) {
        wrappedKeys.filterIsInstance<KeyViewWrapper>().forEach { it.binding.root.isPressed = false }
        val wrappedKey = wrappedKeys.filterIsInstance<KeyViewWrapper>().find { it == key } ?: return
        wrappedKey.binding.root.isPressed = true
    }

}