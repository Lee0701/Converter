package io.github.lee0701.converter.library.engine

interface HanjaConverter {
    fun convert(composingText: ComposingText): List<Candidate>
    fun convertPrefix(composingText: ComposingText): List<List<Candidate>>
}