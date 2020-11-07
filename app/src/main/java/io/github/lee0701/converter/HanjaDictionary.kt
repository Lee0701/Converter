package io.github.lee0701.converter

import io.github.lee0701.converter.dictionary.Dictionary
import io.github.lee0701.converter.dictionary.ListDictionary

interface HanjaDictionary: ListDictionary<HanjaDictionary.Entry> {
    data class Entry(
        val result: String,
        val extra: String?,
        val frequency: Int,
    )
}
