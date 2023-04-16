package io.github.lee0701.converter.library.engine

interface PredictingHanjaConverter: HanjaConverter {

    fun predict(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate>

}