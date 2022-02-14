package io.github.lee0701.converter.dictionary

interface PredictingDictionary<T>: Dictionary<T> {

    fun predict(key: String): T
}