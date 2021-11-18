package io.github.lee0701.converter.engine

import android.content.Context
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.IllegalArgumentException

class Predictor(context: Context) {

    private val indexToWord = context.assets.open("wordlist.txt").bufferedReader().readLines()
        .map { it.split("\t") }.map { (_, word, _) -> word }
    private val wordToIndex = indexToWord.mapIndexed { i, w -> w to i }.toMap()

    private val seqLength = 99
    private val model = Model.newInstance(context)

    fun predict(input: List<Int>, topn: Int = 10): List<CandidatesWindow.Candidate> {
        if(topn <= 0) return emptyList()
        return output(predict(input), topn)
    }

    fun predict(input: List<Int>): FloatArray {
        try {
            val inputArray = ((0 until seqLength).map { indexToWord.size } + input).takeLast(seqLength).toIntArray()
            val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, seqLength), DataType.FLOAT32)
            inputBuffer.loadArray(inputArray)
            return model.process(inputBuffer).outputFeature0AsTensorBuffer.floatArray
        } catch(ex: IllegalArgumentException) {
            return floatArrayOf()
        }
    }

    fun output(prediction: FloatArray, topn: Int = 10): List<CandidatesWindow.Candidate> {
        if(topn <= 0) return emptyList()
        return prediction.mapIndexed { i, value -> i to value }.sortedByDescending { it.second }.take(topn)
            .map { (index, _) -> CandidatesWindow.Candidate(indexToWord[index], "") }
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