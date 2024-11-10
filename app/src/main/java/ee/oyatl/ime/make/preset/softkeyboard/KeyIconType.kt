package ee.oyatl.ime.make.preset.softkeyboard

import kotlinx.serialization.Serializable

@Serializable
enum class KeyIconType {
    Shift,
    ShiftPressed,
    ShiftLocked,
    Caps,
    Option,
    Tab,
    Backspace,
    Language,
    Return,

    Left,
    Right,
    ExpandLeft,
    ExpandRight,
    SelectAll,
    Cut,
    Copy,
    Paste,
}