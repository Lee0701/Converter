package io.github.lee0701.converter.library.engine

import androidx.annotation.ColorInt

open class SpecializedHanjaConverter(
    private val dictionary: io.github.lee0701.converter.library.dictionary.ListDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry>,
    @ColorInt private val candidateColor: Int,
): HanjaConverter {

    override fun convert(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map {
            io.github.lee0701.converter.library.engine.Candidate(
                word,
                it.result,
                it.extra ?: "",
                learnable = false,
                color = candidateColor
            )
        }
    }

    override fun convertPrefix(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<List<io.github.lee0701.converter.library.engine.Candidate>> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        val candidates = result
            .sortedByDescending { it.frequency }
            .map {
                io.github.lee0701.converter.library.engine.Candidate(
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