package io.github.lee0701.converter.dictionary

interface MutableListDictionary<T>: MutableDictionary<List<T>>, ListDictionary<T> {

    fun insert(key: String, item: T) {
        put(key, (search(key) ?: listOf()) + item)
    }

    fun remove(key: String, item: T) {
        val newList = (search(key) ?: listOf()) - item
        if(newList.isEmpty()) remove(key)
        else put(key, newList)
    }
}