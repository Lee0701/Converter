package io.github.lee0701.converter.library.engine

class CachingTFLitePredictor(
    val predictor: TFLitePredictor,
): Predictor {

    var context: String = ""
    var result: Predictor.Result? = null

    override fun predict(composingText: io.github.lee0701.converter.library.engine.ComposingText): Predictor.Result {
        val result = result
        if(context == composingText.textBeforeComposing.toString() && result != null) {
            return result
        } else {
            context = composingText.textBeforeComposing.toString()
            val newResult = predictor.predict(composingText)
            this.result = newResult
            return newResult
        }
    }
}