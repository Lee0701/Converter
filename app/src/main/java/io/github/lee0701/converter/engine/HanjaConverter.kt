package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

interface HanjaConverter {
    fun convert(word: String): List<Candidate>
    fun convertPrefix(word: String): List<List<Candidate>>
    fun learn(input: String, result: String)
}