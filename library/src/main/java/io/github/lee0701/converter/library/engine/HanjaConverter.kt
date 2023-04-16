package io.github.lee0701.converter.library.engine

interface HanjaConverter {
    fun convert(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate>
    fun convertPrefix(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<List<io.github.lee0701.converter.library.engine.Candidate>>
}