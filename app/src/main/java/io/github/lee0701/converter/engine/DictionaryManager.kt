package io.github.lee0701.converter.engine

import android.content.res.AssetManager
import io.github.lee0701.converter.dictionary.CompoundDictionary
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import java.io.IOException

object DictionaryManager {
    fun loadCompoundDictionary(assetManager: AssetManager, names: List<String>): ListDictionary<HanjaDictionary.Entry> {
        val dictionaries = names.mapNotNull { name ->
            try {
                return@mapNotNull DiskDictionary(assetManager.open("dict/$name.bin"))
            } catch (ex: IOException) {
                return@mapNotNull null
            }
        }
        return CompoundDictionary(dictionaries)
    }
}