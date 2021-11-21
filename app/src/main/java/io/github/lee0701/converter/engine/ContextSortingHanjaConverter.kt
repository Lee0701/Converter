package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

class ContextSortingHanjaConverter(
    private val hanjaConverter: HanjaConverter,
    private val predictor: TFLitePredictor,
): HanjaConverter {

    private var predictionContext: String = ""
    private var prediction: FloatArray = floatArrayOf()

    override fun convert(composingText: ComposingText): List<Candidate> {
        return hanjaConverter.convert(composingText).let { converted ->
            if(converted.isNotEmpty()) {
                if(predictionContext != composingText.textBeforeComposing.toString()) {
                    predictionContext = composingText.textBeforeComposing.toString()
                    prediction = predictor.predict(predictor.tokenize(predictionContext))
                }
                if(prediction.isNotEmpty()) {
                    converted.sortedByDescending { predictor.getConfidence(prediction, it.text) }
                }
            }
            converted
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        return hanjaConverter.convertPrefix(composingText).let { converted ->
            if(!converted.all { it.isEmpty() }) {
                if(predictionContext != composingText.textBeforeComposing.toString()) {
                    predictionContext = composingText.textBeforeComposing.toString()
                    prediction = predictor.predict(predictor.tokenize(predictionContext))
                }
                if(prediction.isNotEmpty()) {
                    converted.map { list ->
                        list.sortedByDescending { predictor.getConfidence(prediction, it.text) }
                    }
                }
            }
            converted
        }
    }

    override fun learn(input: String, result: String) {
    }
}