package io.github.lee0701.converter.candidates

import androidx.annotation.ColorInt

data class Candidate(
    val hangul: String,
    val hanja: String,
    val extra: String,
    val input: String = hangul,
    val learnable: Boolean = false,
    @ColorInt val color: Int? = null,
)