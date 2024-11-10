package ee.oyatl.ime.make.modifiers

data class ModifierKeyState(
    val pressed: Boolean = false,
    val locked: Boolean = false,
    val pressing: Boolean = pressed,
) {
    val active: Boolean = pressing || pressed || locked
}