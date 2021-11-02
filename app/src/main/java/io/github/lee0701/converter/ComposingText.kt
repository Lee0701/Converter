package io.github.lee0701.converter

data class ComposingText(
    val text: String,
    val from: Int,
    val to: Int = from,
) {
    val composing: String = text.slice(from until to)
    val textBeforeCursor = text.take(to)
    val textAfterCursor = text.drop(to)

    fun replaced(with: String, length: Int): ComposingText {
        val lengthDiff = with.length - length
        val fullText = text.take(from) + with + composing.drop(length) + text.drop(to)
        return this.copy(text = fullText, from = from + with.length, to = to + lengthDiff)
    }

    fun inserted(with: String): ComposingText {
        val fullText = textBeforeCursor + with + textAfterCursor
        return ComposingText(fullText, to + with.length)
    }

}