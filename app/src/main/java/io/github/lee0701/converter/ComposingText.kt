package io.github.lee0701.converter

import android.text.TextUtils

data class ComposingText(
    val text: CharSequence,
    val from: Int,
    val to: Int = from,
    val unconverted: String = "",
    val converted: String = ""
) {
    val composing: CharSequence = text.slice(from until to)
    val textBeforeCursor: CharSequence = text.take(to)
    val textAfterCursor: CharSequence = text.drop(to)

    fun replaced(with: String, format: OutputFormat?): ComposingText {
        val replace = composing.take(with.length)
        val formatted = format?.getOutput(with, replace) ?: with
        val lengthDiff = formatted.length - with.length
        val fullText = TextUtils.concat(text.take(from), formatted, composing.drop(with.length), text.drop(to))
        return this.copy(text = fullText, from = from + formatted.length, to = to + lengthDiff,
            unconverted = unconverted + replace, converted = converted + with)
    }

    fun inserted(with: CharSequence): ComposingText {
        val fullText = TextUtils.concat(textBeforeCursor, with, textAfterCursor)
        return ComposingText(fullText, to + with.length)
    }

}