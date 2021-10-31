package io.github.lee0701.converter

import android.content.Context
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

    fun preProcessWord(word: String): String {
        if(word.isEmpty()) return word
        if(isHangul(word[0])) return word
        val hangulIndex = word.indexOfFirst { c -> isHangul(c) }
        if(hangulIndex == -1 || hangulIndex >= word.length) return ""
        else return word.substring(hangulIndex)
    }

    private fun getExtraCandidates(conversionTarget: String): List<String> {
        val list = mutableListOf<String>()
        val nonHangulIndex = conversionTarget.indexOfFirst { c -> !isHangul(c) }
        if(nonHangulIndex > 0) list += conversionTarget.substring(0 until nonHangulIndex)
        else list += conversionTarget
        if(isHangul(conversionTarget[0])) list.add(0, conversionTarget[0].toString())
        return list.toList()
    }

    private fun isHanja(c: Char) = c.toInt() in 0x4E00 .. 0x62FF
            || c.toInt() in 0x6300 .. 0x77FF || c.toInt() in 0x7800 .. 0x8CFF
            || c.toInt() in 0x8D00 .. 0x9FFF || c.toInt() in 0x3400 .. 0x4DBF

    private fun isHangul(c: Char) = c.toInt() in 0xAC00 .. 0xD7AF
            || c.toInt() in 0x1100 .. 0x11FF || c.toInt() in 0xA960 .. 0xA97F
            || c.toInt() in 0xD7B0 .. 0xD7FF || c.toInt() in 0x3130 .. 0x318F

    class PrefixSearchHanjaDictionary(dictionary: ListDictionary<HanjaDictionary.Entry>)
        : PrefixSearchDictionary<List<HanjaDictionary.Entry>>(dictionary)

}