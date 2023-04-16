package io.github.lee0701.converter.library.dictionary

interface HanjaDictionary: ListDictionary<HanjaDictionary.Entry> {
    data class Entry(
        val result: String,
        val extra: String?,
        val frequency: Int,
    )
}
