package io.github.lee0701.converter.dictionary

class CompoundDictionary<T>(
    private val dictionaries: List<ListDictionary<T>>
): ListDictionary<T> {
    override fun search(key: String): List<T> {
        val result = dictionaries.mapNotNull { it.search(key) }
        return result.flatten()
    }
}