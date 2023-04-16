package io.github.lee0701.converter.library.dictionary

interface Dictionary<T> {
    fun search(key: String): T?
}
