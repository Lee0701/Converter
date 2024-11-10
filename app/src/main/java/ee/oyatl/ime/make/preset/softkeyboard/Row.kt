package ee.oyatl.ime.make.preset.softkeyboard

import kotlinx.serialization.Serializable

@Serializable
data class Row(
    @Serializable val keys: List<RowItem> = listOf(),
)