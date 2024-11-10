package ee.oyatl.ime.make.preset.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CompoundKeyOutputSerializer: KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KeyOutput", PrimitiveKind.INT)

    private val serializers: List<KeyOutputSerializer> = listOf(
        HangulJamoSerializer,
        KeyCharSerializer,
        HexIntKeyOutputSerializer
    )

    override fun serialize(encoder: Encoder, value: Int) {
        val result = serializers.firstNotNullOfOrNull { it.serialize(value) }.orEmpty()
        encoder.encodeString(result)
    }

    override fun deserialize(decoder: Decoder): Int {
        val value = decoder.decodeString()
        return serializers.firstNotNullOfOrNull { it.deserialize(value) } ?: 0
    }
}