package ee.oyatl.ime.make.preset.softkeyboard

import kotlinx.serialization.Serializable

@Serializable
data class Keyboard(
    @Serializable val rows: List<Row> = listOf(),
) {
    operator fun plus(another: Keyboard): Keyboard {
        return Keyboard(
            rows = this.rows + another.rows,
        )
    }
}