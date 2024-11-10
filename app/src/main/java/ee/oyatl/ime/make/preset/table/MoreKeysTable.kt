package ee.oyatl.ime.make.preset.table

import android.content.res.AssetManager
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import ee.oyatl.ime.make.preset.serialization.CompoundKeyOutputSerializer
import ee.oyatl.ime.make.preset.softkeyboard.Keyboard
import kotlinx.serialization.Serializable

data class MoreKeysTable(
    val map: Map<
            @Serializable(with = CompoundKeyOutputSerializer::class) Int,
            Keyboard> = mapOf(),
) {
    operator fun plus(another: MoreKeysTable): MoreKeysTable {
        return MoreKeysTable(this.map + another.map)
    }

    @Serializable
    data class RefMap(
        @Serializable val map: Map<
                @Serializable(with = CompoundKeyOutputSerializer::class) Int,
                String> = mapOf(),
    ) {
        fun resolve(assets: AssetManager, yaml: Yaml): MoreKeysTable {
            return MoreKeysTable(map.map { (key, value) ->
                val keyboard = yaml.decodeFromStream<Keyboard>(assets.open(value))
                return@map key to keyboard
            }.toMap())
        }
    }
}