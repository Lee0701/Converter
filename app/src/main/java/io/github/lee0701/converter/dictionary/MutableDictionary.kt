package io.github.lee0701.converter.dictionary

interface MutableDictionary<T>: Dictionary<T> {
    fun put(key: String, value: T)
    fun remove(key: String)
}
