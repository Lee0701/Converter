package io.github.lee0701.converter

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.engine.ComposingText
import io.github.lee0701.converter.engine.HanjaConverter
import io.github.lee0701.converter.engine.TFLitePredictor
import kotlinx.coroutines.*

class Converter(
    private val hanjaConverter: HanjaConverter,
) {

    fun convertAsync(scope: CoroutineScope, composingText: ComposingText)
    : Deferred<List<Candidate>> = scope.async {
        val converted = hanjaConverter.convertPrefix(composingText)
        return@async getExtraCandidates(composingText.composing) + converted.flatten()
    }

    fun learn(input: String, result: String) {
        hanjaConverter.learn(input, result)
    }

    private fun getExtraCandidates(hangul: CharSequence): List<Candidate> {
        if(hangul.isEmpty()) return emptyList()
        val list = mutableListOf<CharSequence>()
        val nonHangulIndex = hangul.indexOfFirst { c -> !CharacterSet.isHangul(c) }
        list += if(nonHangulIndex > 0) hangul.slice(0 until nonHangulIndex) else hangul
        if(CharacterSet.isHangul(hangul[0])) list.add(0, hangul[0].toString())
        return list.map { Candidate(it.toString()) }
    }

}