package ee.oyatl.ime.make.preset.serialization

object KeyCharSerializer: KeyOutputSerializer {
    override fun serialize(value: Int): String? {
        if(value < 0x20) return null
        return value.toChar().toString()
    }

    override fun deserialize(value: String): Int? {
        if(value.length != 1) return null
        return value.codePointAt(0)
    }
}