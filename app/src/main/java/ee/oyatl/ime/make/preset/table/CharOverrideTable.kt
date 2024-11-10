package ee.oyatl.ime.make.preset.table

import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import ee.oyatl.ime.make.preset.serialization.CompoundKeyOutputSerializer
import kotlinx.serialization.Serializable

@Serializable
data class CharOverrideTable(
    @Serializable val map: Map<
            @Serializable(with = CompoundKeyOutputSerializer::class) Int,
            @Serializable(with = CompoundKeyOutputSerializer::class) Int> = mapOf(),
) {

    private val reversedMap: Map<Int, Int> = map.map { (key, value) ->
        value to key
    }.toMap()

    fun get(charCode: Int): Int? {
        return map[charCode]
    }

    fun getAllForState(state: ModifierKeyStateSet): Map<Int, Int> {
        return map
    }

    fun getReversed(charCode: Int): Int? {
        return reversedMap[charCode]
    }

    operator fun plus(table: CharOverrideTable): CharOverrideTable {
        return CharOverrideTable(map = this.map + table.map)
    }
}