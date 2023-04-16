package io.github.lee0701.converter.library.engine

import android.content.res.AssetManager
import java.io.IOException

object DictionaryManager {
    fun loadCompoundDictionary(assetManager: AssetManager, names: List<String>): io.github.lee0701.converter.library.dictionary.CompoundDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry> {
        val dictionaries = names.mapNotNull { name ->
            loadDictionary(assetManager, name)
        }
        return io.github.lee0701.converter.library.dictionary.CompoundDictionary(dictionaries)
    }
    fun loadDictionary(assetManager: AssetManager, name: String): io.github.lee0701.converter.library.dictionary.ListDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry>? {
        return try {
            io.github.lee0701.converter.library.dictionary.DiskDictionary(assetManager.open("dict/$name.bin"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}