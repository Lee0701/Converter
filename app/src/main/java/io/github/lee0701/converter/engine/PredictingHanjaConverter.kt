package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

interface PredictingHanjaConverter: HanjaConverter {

    fun predict(composingText: ComposingText): List<Candidate>

}