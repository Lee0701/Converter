package io.github.lee0701.converter.dictionary

open class PrefixSearchDictionary<T>(private val dictionary: Dictionary<T>): ListDictionary<T> {
    override fun search(key: String): List<T>? {
        val results = mutableListOf<T>()
        key.indices.reversed().forEach { i ->
            val slicedKey = key.slice(0 .. i)
            results += dictionary.search(slicedKey) ?: return@forEach
        }
        return results.toList()
    }
    fun searchExact(key: String): T? {
        return dictionary.search(key)
    }
}