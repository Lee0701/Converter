package io.github.lee0701.converter.library.engine

interface Predictor {

    fun predict(composingText: io.github.lee0701.converter.library.engine.ComposingText): Result

    interface Result {
        fun top(n: Int): List<io.github.lee0701.converter.library.engine.Candidate>
        fun score(candidate: io.github.lee0701.converter.library.engine.Candidate): Float
    }
}