package io.github.lee0701.converter.candidates

object CandidateWindowColor {
    const val DEFAULT = (0xFFFAFAFA).toInt()
    const val GBOARD = (0xFFE8EAED).toInt()

    fun of(name: String, custom: Int = DEFAULT) = when(name) {
        "default" -> DEFAULT
        "gboard" -> GBOARD
        "custom" -> custom
        else -> DEFAULT
    }
}