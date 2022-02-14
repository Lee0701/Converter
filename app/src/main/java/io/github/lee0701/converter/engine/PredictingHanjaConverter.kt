package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.dictionary.PredictingDictionary

class PredictingHanjaConverter(
    private val dictionary: PredictingDictionary<List<HanjaDictionary.Entry>>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        return emptyList()
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            val slicedWord = word.slice(0 .. i)
            dictionary.predict(slicedWord)
                .sortedByDescending { it.frequency }
                .map { Candidate(slicedWord, it.result, it.extra ?: "") }
        }
    }

    override fun learn(input: String, result: String) {
    }
}