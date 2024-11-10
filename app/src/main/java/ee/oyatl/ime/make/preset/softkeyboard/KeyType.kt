package ee.oyatl.ime.make.preset.softkeyboard

import kotlinx.serialization.Serializable

@Serializable
enum class KeyType {
    Alphanumeric,
    AlphanumericAlt,
    Modifier,
    ModifierAlt,
    Space,
    Action,
}