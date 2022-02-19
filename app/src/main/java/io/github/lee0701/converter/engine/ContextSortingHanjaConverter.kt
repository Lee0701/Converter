package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

class ContextSortingHanjaConverter(
    private val hanjaConverter: HanjaConverter,
    private val predictor: TFLitePredictor,
): HanjaConverter {

    private var predictionContext: String = ""
    private var prediction: Predictor.Result? = null

    override fun convert(composingText: ComposingText): List<Candidate> {
        return hanjaConverter.convert(composingText).let { converted ->
            if(converted.isNotEmpty()) {
                if(predictionContext != composingText.textBeforeComposing.toString()) {
                    predictionContext = composingText.textBeforeComposing.toString()
                    prediction = predictor.predict(predictionContext)
                }
                val prediction = this.prediction
                if(prediction != null) {
                    return@let converted.sortedByDescending { prediction.score(it.hanja) }
                }
            }
            return@let converted
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        return hanjaConverter.convertPrefix(composingText).let { converted ->
            if(converted.any { it.isNotEmpty() }) {
                if(predictionContext != composingText.textBeforeComposing.toString()) {
                    predictionContext = composingText.textBeforeComposing.toString()
                    prediction = predictor.predict(predictionContext)
                }
                val prediction = this.prediction
                if(prediction != null) {
                    return@let converted.map { list ->
                        list.sortedByDescending { prediction.score(it.hanja) }
                    }
                }
            }
            return@let converted
        }
    }

}