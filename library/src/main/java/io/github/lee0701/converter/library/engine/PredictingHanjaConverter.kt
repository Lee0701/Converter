package io.github.lee0701.converter.library.engine

interface PredictingHanjaConverter: HanjaConverter {

    fun predict(composingText: ComposingText): List<Candidate>

}