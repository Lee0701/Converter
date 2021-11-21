package io.github.lee0701.converter

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.engine.ComposingText
import io.github.lee0701.converter.engine.HanjaConverter
import io.github.lee0701.converter.engine.TFLitePredictor
import kotlinx.coroutines.*

class Converter(
    private val hanjaConverter: HanjaConverter,
    private val predictor: TFLitePredictor?,
    private val sortByContext: Boolean,
) {
    private var predictionContext: String = ""
    private var prediction: FloatArray = floatArrayOf()

    fun convert(composingText: ComposingText)
    : Deferred<List<Candidate>> = CoroutineScope(Dispatchers.IO).async {
        var converted = hanjaConverter.convertPrefix(composingText.composing.toString())
        if(predictor != null && sortByContext && !converted.all { it.isEmpty() }) {
            if(predictionContext != composingText.textBeforeComposing.toString()) {
                predictionContext = composingText.textBeforeComposing.toString()
                prediction = predictor.predict(predictor.tokenize(predictionContext))
            }
            if(prediction.isNotEmpty()) {
                converted = converted.map { list ->
                    list.sortedByDescending { predictor.getConfidence(prediction, it.text) }
                }
            }
        }
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