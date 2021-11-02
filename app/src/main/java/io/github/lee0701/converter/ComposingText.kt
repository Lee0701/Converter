package io.github.lee0701.converter

data class ComposingText(
    val text: String,
    val cursor: Int,
    val from: Int,
    val to: Int,
) {
    val composing: String = text.slice(from until to)

    fun replaced(with: String, length: Int): ComposingText {
        val lengthDiff = with.length - length
        val fullText = text.take(from) + with + composing.drop(length) + text.drop(to)
        return this.copy(text = fullText, from = from + with.length, to = to + lengthDiff)
    }
}