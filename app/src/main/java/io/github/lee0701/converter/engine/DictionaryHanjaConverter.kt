package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.history.Word
import kotlinx.coroutines.*

class DictionaryHanjaConverter(
    private val dictionary: HanjaDictionary,
): HanjaConverter {

    override fun convert(word: String): List<Candidate> {
        val result = dictionary.search(word) ?: emptyList()
        return result.map { Candidate(it.result, it.extra ?: "") }
    }

    override fun convertPrefix(word: String): List<List<Candidate>> {
        return word.indices.reversed().map { i ->
            dictionary.search(word.slice(0 .. i))
                ?.sortedByDescending { it.frequency }
                ?.map { Candidate(it.result, it.extra ?: "") } ?: emptyList()
        }
    }

    override fun learn(input: String, result: String) {
    }
}