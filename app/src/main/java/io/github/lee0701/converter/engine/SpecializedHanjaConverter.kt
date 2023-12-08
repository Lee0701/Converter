package io.github.lee0701.converter.engine

import androidx.annotation.ColorInt

open class SpecializedHanjaConverter(
    private val dictionary: io.github.lee0701.converter.dictionary.ListDictionary<io.github.lee0701.converter.dictionary.HanjaDictionary.Entry>,
    @ColorInt private val candidateColor: Int,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map {
            Candidate(
                word,
                it.result,
                it.extra ?: "",
                learnable = false,
                color = candidateColor
            )
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        val candidates = result
            .sortedByDescending { it.frequency }
            .map {
                Candidate(
                    word,
                    it.result,
                    it.extra ?: "",
                    learnable = false,
                    color = candidateColor
                )
            }
        return listOf(candidates) + (0 until word.length-1).map { emptyList() }
    }

}