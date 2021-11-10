package io.github.lee0701.converter

import android.text.TextUtils

data class ComposingText(
    val text: CharSequence,
    val from: Int,
    val to: Int = from,
) {
    val composing: CharSequence = text.slice(from until to)
    val textBeforeCursor: CharSequence = text.take(to)
    val textAfterCursor: CharSequence = text.drop(to)

    fun replaced(with: CharSequence, length: Int): ComposingText {
        val lengthDiff = with.length - length
        val fullText = TextUtils.concat(text.take(from), with, composing.drop(length), text.drop(to))
        return this.copy(text = fullText, from = from + with.length, to = to + lengthDiff)
    }

    fun inserted(with: CharSequence): ComposingText {
        val fullText = TextUtils.concat(textBeforeCursor, with, textAfterCursor)
        return ComposingText(fullText, to + with.length)
    }

}