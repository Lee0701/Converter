package io.github.lee0701.converter

data class ComposingText(
    val text: String,
    val cursor: Int,
    val from: Int,
    val to: Int,
    val convertedLength: Int = 0
) {
    val composing: String = text.slice(from + convertedLength until to)
    val converted: String = text.slice(from until from + convertedLength)
    val beforeComposing: String = text.take(from)
    val afterComposing: String = text.drop(to)

    fun replaced(with: String): ComposingText {
        val fullText = beforeComposing + converted + with + composing.drop(with.length) + afterComposing
        return this.copy(text = fullText, convertedLength = convertedLength + with.length)
    }
}