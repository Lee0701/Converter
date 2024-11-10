package ee.oyatl.ime.make.preset.serialization

object HexIntKeyOutputSerializer: KeyOutputSerializer {
    private const val PREFIX = "0x"

    override fun serialize(value: Int): String {
        return PREFIX + value.toString(16).padStart(4, '0')
    }

    override fun deserialize(value: String): Int? {
        if(!value.startsWith(PREFIX)) return null
        return value.replaceFirst(PREFIX, "").toIntOrNull(16)
    }
}