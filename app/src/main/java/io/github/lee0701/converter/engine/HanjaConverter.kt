package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

interface HanjaConverter {
    fun convert(composingText: ComposingText): List<Candidate>
    fun convertPrefix(composingText: ComposingText): List<List<Candidate>>
    fun learn(input: String, result: String)
}