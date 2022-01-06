package io.github.lee0701.converter.engine

import android.content.Context
import android.content.res.AssetFileDescriptor
import io.github.lee0701.converter.candidates.Candidate
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLitePredictor(context: Context, wordList: InputStream, model: AssetFileDescriptor) {

    private val indexToWord = wordList.bufferedReader().readLines()
        .map { it.split("\t") }.map { (_, word, _) -> word }
    private val wordToIndex = indexToWord.mapIndexed { i, w -> w to i }.toMap()

    private val seqLength = 99
    private val interpreter = Interpreter(model.createInputStream().channel.map(
        FileChannel.MapMode.READ_ONLY, model.startOffset, model.declaredLength))

    fun predict(input: List<Int>, topn: Int = 10): List<Candidate> {
        if(topn <= 0) return emptyList()
        return output(predict(input), topn)
    }

    fun predict(input: List<Int>): FloatArray {
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

    fun output(prediction: FloatArray, topn: Int = 10): List<Candidate> {
        if(topn <= 0) return emptyList()
        return prediction.mapIndexed { i, value -> i to value }.sortedByDescending { it.second }.take(topn)
            .map { (index, _) -> Candidate("", indexToWord[index], "") }
    }

    fun getConfidence(prediction: FloatArray, word: String): Float? {
        return wordToIndex[word]?.let { prediction[it] }
    }

    fun tokenize(text: String): List<Int> {
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

}