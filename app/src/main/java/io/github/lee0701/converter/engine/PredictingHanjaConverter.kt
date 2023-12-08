package io.github.lee0701.converter.engine

interface PredictingHanjaConverter: HanjaConverter {

    fun predict(composingText: ComposingText): List<Candidate>

}