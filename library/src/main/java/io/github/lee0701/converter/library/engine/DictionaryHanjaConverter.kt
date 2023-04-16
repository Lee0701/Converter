package io.github.lee0701.converter.library.engine

import io.github.lee0701.converter.library.dictionary.HanjaDictionary
import io.github.lee0701.converter.library.dictionary.ListDictionary

open class DictionaryHanjaConverter(
    private val dictionary: ListDictionary<HanjaDictionary.Entry>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map {
            Candidate(
                word,
                it.result,
                it.extra ?: "",
                learnable = true
            )
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            val slicedWord = word.slice(0 .. i)
            dictionary.search(slicedWord)
                ?.sortedByDescending { it.frequency }
                ?.map {
                    Candidate(
                        slicedWord,
                        it.result,
                        it.extra ?: "",
                        learnable = true
                    )
                } ?: emptyList()
        }
    }

}