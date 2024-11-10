package ee.oyatl.ime.make.modifiers

interface ModifierKeyHandler {
    val state: ModifierKeyState
    fun reset()
    fun onPress()
    fun onRelease()
    fun onLock()
    fun onInput()
}