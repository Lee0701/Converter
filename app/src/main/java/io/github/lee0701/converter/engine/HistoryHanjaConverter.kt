package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.history.Word

class HistoryHanjaConverter(
    private val database: HistoryDatabase,
    private val freezeLearning: Boolean,
): HanjaConverter {

    override fun convert(word: String): List<Candidate> {
        val result = database.wordDao().searchWords(word)
        return result.sortedByDescending { it.count }.map { Candidate(it.result, "") }
    }

    override fun convertPrefix(word: String): List<List<Candidate>> {
        return word.indices.reversed().map { i ->
            database.wordDao().searchWords(word.slice(0 .. i))
                .sortedByDescending { it.count }
                .map { Candidate(it.result, "") }
        }
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