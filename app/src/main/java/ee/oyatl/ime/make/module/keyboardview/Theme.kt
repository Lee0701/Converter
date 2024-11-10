package ee.oyatl.ime.make.module.keyboardview

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors
import ee.oyatl.ime.make.preset.softkeyboard.KeyIconType
import ee.oyatl.ime.make.preset.softkeyboard.KeyType

data class Theme(
    val keyboardBackground: StyleComponent,
    val keyBackground: Map<KeyType, StyleComponent> = mapOf(),
    val keyIcon: Map<KeyIconType, IconComponent> = mapOf(),
    val popupBackground: StyleComponent,
) {
    constructor(
        @StyleRes keyboardBackground: Int,
        keyBackground: Map<KeyType, Int>,
        keyIcon: Map<KeyIconType, Int>,
        @StyleRes popupBackground: Int,
    ): this(
        keyboardBackground = StyleComponent(keyboardBackground),
        keyBackground = keyBackground.mapValues { (_, v) -> StyleComponent(v) },
        keyIcon = keyIcon.mapValues { (_, v) -> IconComponent(v) },
        popupBackground = StyleComponent(popupBackground),
    )

    data class StyleComponent(
        @StyleRes override val resource: Int
    ): ThemeComponent()

    data class IconComponent(
        @DrawableRes override val resource: Int
    ): ThemeComponent()

    abstract class ThemeComponent {
        abstract val resource: Int

        fun wrapContext(context: Context): Context {
            return wrapContextDynamic(wrapContextStatic(context))
        }

        private fun wrapContextDynamic(context: Context): Context {
            return DynamicColors.wrapContextIfAvailable(context)
        }

        private fun wrapContextStatic(context: Context): Context {
            return ContextThemeWrapper(context, resource)
        }
    }
}