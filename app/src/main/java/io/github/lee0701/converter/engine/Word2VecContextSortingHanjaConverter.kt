package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate
import org.allenai.word2vec.Searcher
import org.allenai.word2vec.Word2VecModel

class Word2VecContextSortingHanjaConverter(
    private val hanjaConverter: HanjaConverter,
    private val model: Word2VecModel
): HanjaConverter {

    private val modelSearcher: Searcher = model.forSearch()

    private var predictionContext: String = ""
    private var tokens: List<String> = listOf()

    override fun convert(composingText: ComposingText): List<Candidate> {
        return hanjaConverter.convert(composingText).let { converted ->
            if(converted.isNotEmpty()) {
                updateTokens(composingText)
                if(tokens.isNotEmpty()) converted.sortedByDescending { getAverageDistance(tokens, it.text) }
                else converted
            }
            else converted
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        return hanjaConverter.convertPrefix(composingText).let { converted ->
            if(converted.any { it.isNotEmpty() }) {
                updateTokens(composingText)
                if(tokens.isNotEmpty()) converted.map { list ->
                    println(list.map { it.text to getAverageDistance(tokens, it.text) })
                    list.sortedByDescending { getAverageDistance(tokens, it.text) }
                }
                else converted
            }
            else converted
        }
    }

    override fun learn(input: String, result: String) {
    }

    private fun updateTokens(composingText: ComposingText) {
        if(predictionContext != composingText.textBeforeComposing.toString()) {
            predictionContext = composingText.textBeforeComposing.toString()
            tokens = tokenize(composingText.textBeforeComposing.toString()).takeLast(10)
        }
    }

    private fun getAverageDistance(tokens: List<String>, candidate: String): Double {
        if(!modelSearcher.contains(candidate)) return -1.0
        return tokens
            .filter { modelSearcher.contains(it) }
            .map { modelSearcher.cosineDistance(candidate, it) }
            .average()
    }

    private fun tokenize(text: String): List<String> {
        val result = mutableListOf<String?>()
        text.indices.forEach { i ->
            if(result.sumOf { it?.length ?: 1 } <= i) {
                for(j in (i .. text.length).reversed()) {
                    val substr = text.substring(i, j)
                    if(modelSearcher.contains(substr)) {
                        result += substr
                        break
                    } else if(substr.length == 1) {
                        result += null
                    }
                }
            }
        }
        return result.filterNotNull()
    }

}