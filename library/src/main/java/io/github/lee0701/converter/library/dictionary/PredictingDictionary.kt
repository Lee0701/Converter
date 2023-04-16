package io.github.lee0701.converter.library.dictionary

interface PredictingDictionary<T>: ListDictionary<T> {

    fun predict(key: String): List<Pair<String, T>>
}