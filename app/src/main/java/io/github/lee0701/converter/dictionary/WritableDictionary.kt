package io.github.lee0701.converter.dictionary

interface WritableDictionary<T>: Dictionary<T> {
    fun put(key: String, value: T)
    fun remove(key: String)
}
