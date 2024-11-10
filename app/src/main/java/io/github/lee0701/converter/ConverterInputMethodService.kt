package io.github.lee0701.converter

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import ee.oyatl.ime.make.module.keyboardview.CanvasKeyboardView
import ee.oyatl.ime.make.module.keyboardview.FlickDirection
import ee.oyatl.ime.make.module.keyboardview.KeyboardListener
import ee.oyatl.ime.make.module.keyboardview.Themes
import ee.oyatl.ime.make.preset.softkeyboard.Include
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import ee.oyatl.ime.make.preset.softkeyboard.Row
import ee.oyatl.ime.make.preset.softkeyboard.RowItem
import kotlinx.serialization.modules.EmptySerializersModule

class ConverterInputMethodService: InputMethodService(), KeyboardListener {

    private val yamlConfig = YamlConfiguration(encodeDefaults = false)
    val yaml = Yaml(EmptySerializersModule(), yamlConfig)

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val keyboard = yaml.decodeFromStream<Keyboard>(assets.open("keyboard/soft_mobile_qwerty.yaml"))
        val resolved = keyboard.copy(rows = keyboard.rows.map { it.copy(keys = resolveSoftKeyIncludes(this, it)) })
        val keyboardView = CanvasKeyboardView(this, null, resolved, Themes.Dynamic, this, 160)
        return keyboardView
    }

    private fun resolveSoftKeyIncludes(context: Context, row: Row): List<RowItem> {
        return row.keys.flatMap { rowItem ->
            if(rowItem is Include) resolveSoftKeyIncludes(context,
                yaml.decodeFromStream(context.assets.open(rowItem.name)))
            else listOf(rowItem)
        }
    }

    override fun onKeyClick(code: Int, output: String?) {

    }

    override fun onKeyDown(code: Int, output: String?) {

    }

    override fun onKeyUp(code: Int, output: String?) {

    }

    override fun onKeyLongClick(code: Int, output: String?) {

    }

    override fun onKeyFlick(direction: FlickDirection, code: Int, output: String?) {

    }

    override fun onMoreKeys(code: Int, output: String?): Int? {
        return null
    }
}