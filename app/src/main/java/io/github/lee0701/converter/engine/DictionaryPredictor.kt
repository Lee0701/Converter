package io.github.lee0701.converter.engine

import io.github.lee0701.converter.Hangul
import java.text.Normalizer

class DictionaryPredictor(
    val dictionary: io.github.lee0701.converter.dictionary.PredictingDictionary<io.github.lee0701.converter.dictionary.HanjaDictionary.Entry>
): Predictor {

    override fun predict(composingText: ComposingText): Predictor.Result {
        val composing = composingText.composing.toString()
        val preprocessed = preprocess(composing)
        val predicted = preprocessed.flatMap { word ->
            val first = word.dropLast(1)
            val last = word.lastOrNull()
            val lastDecomposed = Normalizer.normalize(last?.toString() ?: "", Normalizer.Form.NFD)
            val input = if(last !in '가' .. '힣' || lastDecomposed.lastOrNull() in '\u1161' .. '\u1175') first else word
            if(input.isEmpty()) return@flatMap listOf()
            val predicted = dictionary.predict(input)
            return@flatMap predicted.filter { (k, _) -> word != k && match(word, k) }
        }
        val maxFrequency = predicted.map { (_, result) -> result.frequency }.maxOrNull()?.toFloat() ?: 1f
        val sorted = predicted.sortedByDescending { (_, result) -> result.frequency }
        val candidates = sorted.map { (key, value) -> Candidate(
            key,
            value.result,
            value.extra ?: "",
            composing
        ) to value.frequency / maxFrequency }

        return Result(candidates)
    }

    private fun match(input: String, output: String): Boolean {
        val decomposedInput = Normalizer.normalize(input, Normalizer.Form.NFD)
        val decomposedOutput = Normalizer.normalize(output, Normalizer.Form.NFD)
        return decomposedOutput.startsWith(decomposedInput)
    }

    private fun preprocess(word: String): List<String> {
        if(word.isEmpty()) return listOf()
        val result = mutableListOf(word)

        val first = word.dropLast(1)
        val last = word.last()

        val lastDecomposed = Normalizer.normalize(last.toString(), Normalizer.Form.NFD)
        val lastFirst = lastDecomposed.dropLast(1)
        val lastJamo = lastDecomposed.last()

        if(lastJamo in '\u11a8' .. '\u11c2') {
            val split = Hangul.splitJamo(lastJamo)
            val combined = if(split != null) {
                lastFirst + split.first + Hangul.toStandardInitial(
                    Hangul.toCompat(split.second))
            } else {
                lastFirst + Hangul.toStandardInitial(Hangul.toCompat(lastJamo))
            }
            result += first + Normalizer.normalize(combined, Normalizer.Form.NFC)
        } else if(last in 'ㄱ' .. 'ㅎ') {
            val combined = Hangul.toStandardInitial(last)?.toString() ?: ""
            result += first + Normalizer.normalize(combined, Normalizer.Form.NFC)
        }

        return result.toList()
    }

    class Result(
        candidates: List<Pair<Candidate, Float>>
    ): Predictor.Result {

        private val candidates = candidates.sortedByDescending { it.second }.toMap()

        override fun top(n: Int): List<Candidate> {
            return candidates.keys.take(n)
        }

        override fun score(candidate: Candidate): Float {
            return candidates[candidate] ?: 0f
        }
    }

}