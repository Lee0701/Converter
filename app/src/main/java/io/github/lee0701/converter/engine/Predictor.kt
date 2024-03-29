package io.github.lee0701.converter.engine

interface Predictor {

    fun predict(composingText: ComposingText): Result

    interface Result {
        fun top(n: Int): List<Candidate>
        fun score(candidate: Candidate): Float
    }
}