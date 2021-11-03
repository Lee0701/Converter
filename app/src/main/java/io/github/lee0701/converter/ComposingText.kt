package io.github.lee0701.converter

data class ComposingText(
    val text: String,
    val from: Int,
    val to: Int = from,
    val unconverted: String = "",
    val converted: String = ""
) {
    val composing: String = text.slice(from until to)
    val textBeforeCursor = text.take(to)
    val textAfterCursor = text.drop(to)

    fun replaced(with: String, format: OutputFormat?): ComposingText {
        val replace = composing.take(with.length)
        val formatted = format?.getOutput(with, replace) ?: with
        val lengthDiff = formatted.length - with.length
        val fullText = text.take(from) + formatted + composing.drop(with.length) + text.drop(to)
        return this.copy(text = fullText, from = from + formatted.length, to = to + lengthDiff,
            unconverted = unconverted + replace, converted = converted + with)
    }

    fun inserted(with: String): ComposingText {
        val fullText = textBeforeCursor + with + textAfterCursor
        return ComposingText(fullText, to + with.length)
    }

}