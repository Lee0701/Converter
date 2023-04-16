package io.github.lee0701.converter.library.engine

class NextWordPredictor(
    private val predictor: Predictor,
): Predictor {
    override fun predict(composingText: io.github.lee0701.converter.library.engine.ComposingText): Predictor.Result {
        if(composingText.composing.isEmpty()) return predictor.predict(composingText)
        else return EmptyResult
    }
    object EmptyResult: Predictor.Result {
        override fun top(n: Int): List<io.github.lee0701.converter.library.engine.Candidate> = emptyList()
        override fun score(candidate: io.github.lee0701.converter.library.engine.Candidate): Float = 1.0f
    }
}