package ee.oyatl.ime.make.modifiers

class DefaultShiftKeyHandler(
    private val doubleTapGap: Int = 500,
    private val longPressDuration: Int = 100,
    private val autoUnlock: Boolean = true
): ModifierKeyHandler {

    override var state: ModifierKeyState = ModifierKeyState()
    private var pressedTime: Long = 0L
    private var releasedTime: Long = 0L
    private var inputEventExists: Boolean = false

    override fun reset() {
        state = ModifierKeyState()
        releasedTime = 0L
        inputEventExists = false
    }

    override fun onPress() {
        state = state.copy(pressing = true)
        pressedTime = System.currentTimeMillis()
        inputEventExists = false
    }

    override fun onRelease() {
        val lastState = state
        val currentState = lastState.copy(pressing = false)

        val currentTime = System.currentTimeMillis()
        val tapTimeDiff = currentTime - releasedTime
        val pressTimeDiff = currentTime - pressedTime

        val newState =
            if(currentState.locked) {
                ModifierKeyState()
            } else if(currentState.pressed) {
                if(tapTimeDiff < doubleTapGap) {
                    ModifierKeyState(pressed = true, locked = true)
                } else {
                    ModifierKeyState()
                }
            } else if(inputEventExists) {
                ModifierKeyState()
            } else if(pressTimeDiff > longPressDuration) {
                ModifierKeyState(pressed = true, locked = true)
            } else {
                ModifierKeyState(pressed = true)
            }

        state = newState.copy(pressing = false)
        releasedTime = currentTime
        inputEventExists = false
    }

    override fun onLock() {
        val currentCapsLockState = state.locked
        state = state.copy(pressed = !currentCapsLockState, locked = !currentCapsLockState)
    }

    override fun onInput() {
        autoUnlock()
        inputEventExists = true
    }

    private fun autoUnlock() {
        if(!autoUnlock) return
        if(state.pressing && inputEventExists) return
        val lastState = state
        if(!lastState.locked && !lastState.pressing) state = ModifierKeyState()
    }
}