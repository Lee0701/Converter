package ee.oyatl.ime.make.preset.table

import ee.oyatl.ime.make.preset.serialization.CompoundKeyOutputSerializer
import ee.oyatl.ime.make.preset.serialization.KeyCodeSerializer
import ee.oyatl.ime.make.modifiers.ModifierKeyStateSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("simple")
class SimpleCodeConvertTable(
    @Serializable val map: Map<
            @Serializable(with = KeyCodeSerializer::class) Int,
            Entry> = mapOf(),
): CodeConvertTable {

    private val reversedMap: Map<Pair<Int, EntryKey>, Int> = map
        .flatMap { (key, value) ->
            value.explode().map { (entryKey, charCode) -> (charCode to entryKey) to key }
        }.toMap()

    override fun get(keyCode: Int, state: ModifierKeyStateSet): Int? {
        return map[keyCode]?.withKeyboardState(state)
    }

    override fun getAllForState(state: ModifierKeyStateSet): Map<Int, Int> {
        return map.map { (k, v) -> v.withKeyboardState(state)?.let { k to it } }
            .filterNotNull()
            .toMap()
    }

    override fun getReversed(charCode: Int, entryKey: EntryKey): Int? {
        return reversedMap[charCode to entryKey]
    }

    override fun plus(table: CodeConvertTable): CodeConvertTable {
        return when(table) {
            is SimpleCodeConvertTable -> this + table
            is LayeredCodeConvertTable -> this + table
        }
    }

    operator fun plus(table: SimpleCodeConvertTable): SimpleCodeConvertTable {
        return SimpleCodeConvertTable(map = this.map + table.map)
    }

    operator fun plus(table: LayeredCodeConvertTable): LayeredCodeConvertTable {
        return LayeredCodeConvertTable(table.layers.mapValues { (_, table) ->
            this + table
        })
    }

    @Serializable
    data class Entry(
        @Serializable(with = CompoundKeyOutputSerializer::class) val base: Int? = null,
        @Serializable(with = CompoundKeyOutputSerializer::class) val shift: Int? = base,
        @Serializable(with = CompoundKeyOutputSerializer::class) val capsLock: Int? = shift,
        @Serializable(with = CompoundKeyOutputSerializer::class) val alt: Int? = base,
        @Serializable(with = CompoundKeyOutputSerializer::class) val altShift: Int? = shift,
        @Serializable(with = CompoundKeyOutputSerializer::class) val moreKeys: Int? = base
    ) {
        fun withKeyboardState(modifiers: ModifierKeyStateSet): Int? {
            val shiftPressed = modifiers.shift.pressed || modifiers.shift.pressing
            val altPressed = modifiers.alt.pressed || modifiers.alt.pressing
            return if(modifiers.shift.locked) capsLock
            else if(shiftPressed && altPressed) altShift
            else if(shiftPressed) shift
            else if(altPressed) alt
            else base
        }
        fun forKey(key: EntryKey): Int? {
            return when(key) {
                EntryKey.Base -> base
                EntryKey.Shift -> shift ?: base
                EntryKey.CapsLock -> capsLock ?: shift ?: base
                EntryKey.Alt -> alt ?: base
                EntryKey.AltShift -> altShift ?: alt
            }
        }
        fun explode(): Map<EntryKey, Int> {
            return listOfNotNull(
                base?.let { EntryKey.Base to it },
                shift?.let { EntryKey.Shift to it },
                capsLock?.let { EntryKey.CapsLock to it },
                alt?.let { EntryKey.Alt to it },
                altShift?.let { EntryKey.AltShift to it },
            ).toMap()
        }
    }

    enum class EntryKey {
        Base, Shift, CapsLock, Alt, AltShift;
        companion object {
            fun fromKeyboardState(modifiers: ModifierKeyStateSet): EntryKey {
                return if(modifiers.alt.pressed && modifiers.shift.pressed) AltShift
                else if(modifiers.alt.pressed) Alt
                else if(modifiers.shift.locked) CapsLock
                else if(modifiers.shift.pressed) Shift
                else Base
            }
        }
    }
}