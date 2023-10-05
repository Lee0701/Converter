package io.github.lee0701.converter.engine

interface HanjaConverter {
    fun convert(composingText: ComposingText): List<Candidate>
    fun convertPrefix(composingText: ComposingText): List<List<Candidate>>
}