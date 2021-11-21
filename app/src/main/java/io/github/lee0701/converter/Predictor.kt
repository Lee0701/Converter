package io.github.lee0701.converter

import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.engine.ComposingText
import io.github.lee0701.converter.engine.TFLitePredictor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class Predictor(
    private val predictor: TFLitePredictor,
) {

    fun predict(composingText: ComposingText)
    : Deferred<List<Candidate>> = CoroutineScope(Dispatchers.IO).async {
        val prediction = predictor.predict(predictor.tokenize(composingText.textBeforeCursor.toString()))
        return@async predictor.output(prediction, 10)
    }
}