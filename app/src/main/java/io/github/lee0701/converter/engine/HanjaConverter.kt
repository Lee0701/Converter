package io.github.lee0701.converter.engine

import android.content.Context
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.HanjaDictionary
import io.github.lee0701.converter.OutputFormat
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.dictionary.ListDictionary
import io.github.lee0701.converter.dictionary.PrefixSearchDictionary

class HanjaConverter(
    context: Context,
    private val outputFormat: OutputFormat?,
) {

    private val dictionary = PrefixSearchHanjaDictionary(DiskDictionary(context.assets.open("dict.bin")))

    fun convert(word: String): List<CandidatesWindow.Candidate> {
        val result = dictionary.search(word) ?: return emptyList()
        val extra = getExtraCandidates(word).map { CandidatesWindow.Candidate(it, "") }
        return extra + result.map { list -> list.sortedByDescending { it.frequency } }
            .flatten().distinct().map { CandidatesWindow.Candidate(it.result, it.extra ?: "") }
    }

    fun convertExact(word: String): List<CandidatesWindow.Candidate> {
        val result = dictionary.searchExact(word) ?: return emptyList()
        return result.sortedByDescending { it.frequency }
            .map { CandidatesWindow.Candidate(it.result, it.extra ?: "") }
    }

    private fun getExtraCandidates(conversionTarget: String): List<String> {
        val list = mutableListOf<String>()
        val nonHangulIndex = conversionTarget.indexOfFirst { c -> !isHangul(c) }
        if(nonHangulIndex > 0) list += conversionTarget.substring(0 until nonHangulIndex)
        else list += conversionTarget
        if(isHangul(conversionTarget[0])) list.add(0, conversionTarget[0].toString())
        return list.toList()
    }

    class PrefixSearchHanjaDictionary(dictionary: ListDictionary<HanjaDictionary.Entry>)
        : PrefixSearchDictionary<List<HanjaDictionary.Entry>>(dictionary)

}