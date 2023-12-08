package io.github.lee0701.converter.engine

class ResortingPredictor(
    private val predictor: Predictor,
    private val sorter: Predictor,
): Predictor {

    override fun predict(composingText: ComposingText): Predictor.Result {
        return if(composingText.composing.isEmpty()) sorter.predict(composingText)
        else Result(predictor.predict(composingText), sorter.predict(composingText))
    }

    class Result(
        private val prediction: Predictor.Result,
        private val sorter: Predictor.Result,
    ): Predictor.Result {
        override fun top(n: Int): List<Candidate> {
            return prediction.top(100).sortedByDescending { sorter.score(it) }
        }

        override fun score(candidate: Candidate): Float {
            return sorter.score(candidate)
        }
    }

}