package ee.oyatl.ime.make.preset.table

import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import kotlinx.serialization.Serializable

@Serializable
sealed interface CodeConvertTable {
    fun get(keyCode: Int, state: ModifierKeyStateSet): Int?
    fun getAllForState(state: ModifierKeyStateSet): Map<Int, Int>
    fun getReversed(charCode: Int, entryKey: SimpleCodeConvertTable.EntryKey): Int?

    operator fun plus(table: CodeConvertTable): CodeConvertTable
}