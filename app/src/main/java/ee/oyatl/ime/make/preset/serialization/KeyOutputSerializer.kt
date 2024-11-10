package ee.oyatl.ime.make.preset.serialization

interface KeyOutputSerializer {
    fun serialize(value: Int): String?
    fun deserialize(value: String): Int?
}