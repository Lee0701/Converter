package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.view.CandidatesWindow
import io.github.lee0701.converter.dictionary.HanjaDictionary
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.history.Word
import kotlinx.coroutines.*

class HanjaConverter(
    private val dictionary: HanjaDictionary,
    private val database: HistoryDatabase?,
    private val scope: CoroutineScope,
    private val freezeLearning: Boolean,
) {

    init {
        database?.let { db ->
            scope.launch {
                val oldWords = db.wordDao().searchWordsOlderThan(System.currentTimeMillis() - 1000*60*60*24*7)
                db.wordDao().deleteWords(*oldWords)
            }
        }
    }

    fun convertAsync(word: String): Deferred<List<CandidatesWindow.Candidate>> {
        return scope.async {
            val dictionaryResult = dictionary.search(word) ?: emptyList()
            val historyResult = database?.wordDao()?.searchWords(word) ?: arrayOf()

            val dictionaryCandidates = dictionaryResult.map { CandidatesWindow.Candidate(it.result, it.extra ?: "") }
            val historyCandidates = historyResult.sortedByDescending { it.count }.map { CandidatesWindow.Candidate(it.result, "") }

            return@async (historyCandidates + dictionaryCandidates).distinctBy { it.text }
        }
    }

    fun convertPrefixAsync(word: String): Deferred<List<List<CandidatesWindow.Candidate>>> {
        return scope.async {
            val historyCandidates = word.indices.reversed().map { i ->
                database?.wordDao()?.searchWords(word.slice(0 .. i))
                    ?.sortedByDescending { it.count }
                    ?.map { CandidatesWindow.Candidate(it.result, "") } ?: emptyList()
            }
            val dictionaryCandidates = word.indices.reversed().map { i ->
                dictionary.search(word.slice(0 .. i))
                    ?.sortedByDescending { it.frequency }
                    ?.map { CandidatesWindow.Candidate(it.result, it.extra ?: "") } ?: emptyList()
            }

            return@async historyCandidates.zip(dictionaryCandidates).map { (hist, dict) -> (hist + dict).distinctBy { it.text } }
        }
    }

    fun learnAsync(input: String, result: String) {
        if(!freezeLearning) scope.launch {
            val database = database ?: return@launch
            val word = database.wordDao().searchWords(input, result).firstOrNull() ?: Word(input, result, 0, 0L)
            val newWord = word.usedOnce()
            database.wordDao().insertWords(newWord)
        }
    }

}