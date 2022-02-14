package io.github.lee0701.converter.engine

import android.text.TextUtils
import io.github.lee0701.converter.CharacterSet
import kotlin.math.abs

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
    val textBeforeComposing: CharSequence = text.take(from)

    fun textChanged(text: CharSequence, fromIndex: Int, addedCount: Int, removedCount: Int): ComposingText {
        val addedText = text.drop(fromIndex).take(addedCount)
        val toIndex = fromIndex + addedCount

        if(addedText.isNotEmpty() && addedText.all { CharacterSet.isHangul(it) }) {
            if(composing.isEmpty()) {
                // Create composing text if not exists
                return ComposingText(text, fromIndex, toIndex)
            } else {
                val spaceIndex = this.composing.lastIndexOfAny(charArrayOf(' ', '\t', '\r', '\n'))
                val from = this.from + if(spaceIndex > -1) spaceIndex + 1 else 0
                return this.copy(text = text, from = from, to = toIndex)
            }
        } else {
            // Reset composing if non-hangul
            return ComposingText(text, toIndex)
        }
    }

    fun textSelectionChanged(start: Int, end: Int): ComposingText {
        if(start == -1 || end == -1) return this
        if(start == end && abs(start - this.to) > 1) {
            return ComposingText(this.text, start)
        }
        return this
    }

    fun replaced(hangul: String, hanja: String, length: Int, format: OutputFormat?): ComposingText {
        val replace = composing.take(length)
        val formatted = format?.getOutput(hanja, hangul) ?: hanja
        val lengthDiff = formatted.length - length
        val fullText = TextUtils.concat(text.take(from), formatted, composing.drop(length), text.drop(to))
        return this.copy(text = fullText, from = from + formatted.length, to = to + lengthDiff,
            unconverted = unconverted + replace, converted = converted + hanja)
    }

    fun inserted(with: CharSequence): ComposingText {
        val fullText = TextUtils.concat(textBeforeCursor, with, textAfterCursor)
        return ComposingText(fullText, to + with.length)
    }

}