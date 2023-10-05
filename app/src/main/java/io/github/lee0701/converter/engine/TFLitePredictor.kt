package io.github.lee0701.converter.engine

import android.content.res.AssetFileDescriptor
import org.tensorflow.lite.Interpreter
import java.io.InputStream
import java.nio.channels.FileChannel

class TFLitePredictor(
    wordList: InputStream,
    modelDescriptor: AssetFileDescriptor,
): Predictor {

    private val indexToWord = wordList.bufferedReader().readLines()
        .map { it.split("\t") }.map { (_, word, _) -> word }
    private val wordToIndex = indexToWord.mapIndexed { i, w -> w to i }.toMap()

    private val seqLength = 99
    private val interpreter = Interpreter(modelDescriptor.createInputStream().channel.map(
        FileChannel.MapMode.READ_ONLY, modelDescriptor.startOffset, modelDescriptor.declaredLength))

    override fun predict(composingText: ComposingText): Predictor.Result {
        return Result(predict(tokenize(composingText.textBeforeComposing.toString())))
    }

    fun predict(context: String): Predictor.Result {
        return Result(predict(tokenize(context)))
    }

    private fun predict(input: List<Int>): FloatArray {
        try {
            val inputArray = ((0 until seqLength).map { indexToWord.size } + input).takeLast(seqLength).map { it.toFloat() }.toFloatArray()
            val outputArray = arrayOf((0 until indexToWord.size + 1).map { 0f }.toFloatArray())
            interpreter.run(arrayOf(inputArray), outputArray)
            return outputArray[0]
        } catch(ex: IllegalArgumentException) {
            ex.printStackTrace()
            return floatArrayOf()
        }
    }

    private fun tokenize(text: String): List<Int> {
        val result = mutableListOf<Int>()
        text.indices.forEach { i ->
            if(result.sumOf { if(it == -1) 1 else indexToWord[it].length } <= i) {
                for(j in (i .. text.length).reversed()) {
                    val substr = text.substring(i, j)
                    val index = wordToIndex[substr]
                    if(index != null) {
                        result += index
                        break
                    } else if(substr.length == 1) {
                        result += -1
                    }
                }
            }
        }
        return result.filter { it != -1 }
    }

    inner class Result(
        private val prediction: FloatArray,
    ): Predictor.Result {
        override fun top(n: Int): List<Candidate> {
            if(n <= 0) return emptyList()
            return prediction.mapIndexed { i, value -> i to value }
                .sortedByDescending { it.second }.take(n)
                .map { (index, _) ->
                    Candidate(
                        "",
                        indexToWord[index],
                        ""
                    )
                }
        }

        override fun score(candidate: Candidate): Float {
            return wordToIndex[candidate.hanja]?.let { prediction.getOrNull(it) } ?: 0f
        }
    }
}