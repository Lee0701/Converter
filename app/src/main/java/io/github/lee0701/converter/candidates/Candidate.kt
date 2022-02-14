package io.github.lee0701.converter.candidates

data class Candidate(
    val hangul: String,
    val hanja: String,
    val extra: String,
    val input: String = hangul,
)