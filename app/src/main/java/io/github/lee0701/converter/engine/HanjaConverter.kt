package io.github.lee0701.converter.engine

import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.dictionary.PrefixSearchHanjaDictionary
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.history.Word
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HanjaConverter(
    private val dictionary: PrefixSearchHanjaDictionary,
    private val database: HistoryDatabase?,
) {

    fun convertAsync(word: String): Deferred<List<CandidatesWindow.Candidate>> {
        return GlobalScope.async {
            val dictionaryResult = dictionary.search(word) ?: emptyList()
            val historyResult = database?.wordDao()?.searchWords(word) ?: arrayOf()

            val dictionaryCandidates = dictionaryResult.map { list -> list.sortedByDescending { it.frequency } }
                .flatten().distinct().map { CandidatesWindow.Candidate(it.result, it.extra ?: "") }
            val historyCandidates = historyResult.sortedByDescending { it.count }.map { CandidatesWindow.Candidate(it.result, "") }
            val extraCandidates = getExtraCandidates(word).map { CandidatesWindow.Candidate(it, "") }

            return@async (extraCandidates + historyCandidates + dictionaryCandidates).distinctBy { it.text }
        }
    }

    fun learnAsync(input: String, result: String) {
        GlobalScope.launch {
            val database = database ?: return@launch
            val word = database.wordDao().searchWords(input, result).firstOrNull() ?: Word(input, result, 0, 0L)
            val newWord = word.usedOnce()
            database.wordDao().insertWords(newWord)
        }
    }

    private fun getExtraCandidates(conversionTarget: String): List<String> {
        val list = mutableListOf<String>()
        val nonHangulIndex = conversionTarget.indexOfFirst { c -> !isHangul(c) }
        if(nonHangulIndex > 0) list += conversionTarget.substring(0 until nonHangulIndex)
        else list += conversionTarget
        if(isHangul(conversionTarget[0])) list.add(0, conversionTarget[0].toString())
        return list.toList()
    }

}