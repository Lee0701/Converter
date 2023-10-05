package io.github.lee0701.converter.dictionary

class MapDictionary<T>(private val entries: Map<String, T>): Dictionary<T> {
    override fun search(key: String): T? {
        return entries[key]
    }
}
