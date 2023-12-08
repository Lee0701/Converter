package io.github.lee0701.converter.dictionary

class CompoundDictionary<T>(
    private val dictionaries: List<ListDictionary<T>>
): ListDictionary<T>, PredictingDictionary<T> {
    override fun search(key: String): List<T> {
        val result = dictionaries.mapNotNull { it.search(key) }
        return result.flatten()
    }

    override fun predict(key: String): List<Pair<String, T>> {
        val result = dictionaries.filterIsInstance<PredictingDictionary<T>>().map { it.predict(key) }
        return result.flatten()
    }
}