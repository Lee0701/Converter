package ee.oyatl.ime.make.preset.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexIntSerializer: KSerializer<Int> {
    private const val PREFIX = "0x"
    override val descriptor = PrimitiveSerialDescriptor("HexInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        val string = PREFIX + value.toString(16).padStart(4, '0')
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Int {
        val string = decoder.decodeString()
        return string.replaceFirst(PREFIX, "").toIntOrNull(16) ?: 0
    }
}