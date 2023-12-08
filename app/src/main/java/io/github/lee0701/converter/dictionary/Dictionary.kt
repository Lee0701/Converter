package io.github.lee0701.converter.dictionary

interface Dictionary<T> {
    fun search(key: String): T?
}
