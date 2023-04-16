package io.github.lee0701.converter.library.dictionary

interface WritableDictionary<T>: Dictionary<T> {
    fun insert(key: String, value: T)
    fun remove(key: T)
}
