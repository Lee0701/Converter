package io.github.lee0701.converter

data class ComposingText(
    val text: String,
    val cursor: Int,
    val from: Int = cursor,
    val to: Int = cursor,
) {
    val composing: String = text.slice(from until to)
    val textBeforeCursor = text.take(cursor)
    val textAfterCursor = text.drop(cursor)

    fun replaced(with: String, length: Int): ComposingText {
        val lengthDiff = with.length - length
        val fullText = text.take(from) + with + composing.drop(length) + text.drop(to)
        return this.copy(text = fullText, from = from + with.length, to = to + lengthDiff)
    }

}