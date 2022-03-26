package io.github.lee0701.converter.engine

import android.content.res.AssetManager
import io.github.lee0701.converter.dictionary.CompoundDictionary
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import java.io.IOException

object DictionaryManager {
    fun loadCompoundDictionary(assetManager: AssetManager, names: List<String>): CompoundDictionary<HanjaDictionary.Entry> {
        val dictionaries = names.mapNotNull { name ->
            loadDictionary(assetManager, name)
        }
        return CompoundDictionary(dictionaries)
    }
    fun loadDictionary(assetManager: AssetManager, name: String): ListDictionary<HanjaDictionary.Entry>? {
        return try {
            DiskDictionary(assetManager.open("dict/$name.bin"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}