package io.github.lee0701.converter.engine

import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.history.Word
import io.github.lee0701.converter.library.engine.Candidate
import io.github.lee0701.converter.library.engine.ComposingText
import io.github.lee0701.converter.library.engine.LearningHanjaConverter
import io.github.lee0701.converter.library.engine.PredictingHanjaConverter

class HistoryHanjaConverter(
    private val database: HistoryDatabase,
    private val freezeLearning: Boolean,
): LearningHanjaConverter,
    PredictingHanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val word = composingText.composing.toString()
        val result = database.wordDao().searchWords(word)
        return result.sortedByDescending { it.count }.map {
            Candidate(
                word,
                it.result,
                "",
                learnable = true
            )
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            val slicedWord = word.slice(0 .. i)
            database.wordDao().searchWords(slicedWord)
                .sortedByDescending { it.count }
                .map {
                    Candidate(
                        slicedWord,
                        it.result,
                        "",
                        learnable = true
                    )
                }
        }
    }

    override fun predict(composingText: ComposingText): List<Candidate> {
        TODO("Not yet implemented")
    }

    override fun learn(input: String, result: String) {
        if(!freezeLearning) {
            val database = database ?: return
            val word = database.wordDao().searchWords(input, result).firstOrNull() ?: Word(input, result, 0, 0L)
            val newWord = word.usedOnce()
            database.wordDao().insertWords(newWord)
        }
    }

    fun deleteOldWords() {
        val oldWords = database.wordDao().searchWordsOlderThan(System.currentTimeMillis() - 1000*60*60*24*7)
        database.wordDao().deleteWords(*oldWords)
    }

}