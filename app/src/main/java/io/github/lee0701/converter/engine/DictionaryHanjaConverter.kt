package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.dictionary.ListDictionary

class DictionaryHanjaConverter(
    private val dictionary: ListDictionary<HanjaDictionary.Entry>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map { Candidate(it.result, it.extra ?: "") }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            dictionary.search(word.slice(0 .. i))
                ?.sortedByDescending { it.frequency }
                ?.map { Candidate(it.result, it.extra ?: "") } ?: emptyList()
        }
    }

    override fun learn(input: String, result: String) {
    }
}