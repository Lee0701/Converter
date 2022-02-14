package io.github.lee0701.converter.dictionary

interface PredictingDictionary<T>: ListDictionary<T> {

    fun predict(key: String): List<Pair<String, T>>
}