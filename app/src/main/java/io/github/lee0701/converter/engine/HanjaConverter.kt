package io.github.lee0701.converter.engine

import android.content.Context
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.HanjaDictionary
import io.github.lee0701.converter.OutputFormat
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.dictionary.Dictionary
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary

class HanjaConverter(private val dictionary: ListDictionary<HanjaDictionary.Entry>) {

    fun convert(word: String): List<CandidatesWindow.Candidate> {
        val result = dictionary.search(word)
        return result?.sortedByDescending { it.frequency }
            ?.map { CandidatesWindow.Candidate(it.result, it.extra ?: "") } ?: emptyList()
    }

    fun convertPrefix(word: String): List<List<CandidatesWindow.Candidate>> {
        return word.indices.reversed().map { i ->
            dictionary.search(word.slice(0 .. i))
                ?.sortedByDescending { it.frequency }
                ?.map { CandidatesWindow.Candidate(it.result, it.extra ?: "") } ?: emptyList()
        }
    }

}