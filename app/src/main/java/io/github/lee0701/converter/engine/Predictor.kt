package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

interface Predictor {

    fun predict(composingText: ComposingText): Result

    interface Result {
        fun top(n: Int): List<Candidate>
        fun score(candidate: Candidate): Float
    }
}