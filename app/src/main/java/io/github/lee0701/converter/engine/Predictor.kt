package io.github.lee0701.converter.engine

import android.content.Context
import io.github.lee0701.converter.candidates.CandidatesWindow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer

class Predictor(context: Context, modelPath: String) {

    private val indexToWord = context.assets.open("wordlist.txt").bufferedReader().readLines()
        .map { it.split("\t") }.map { (_, word, _) -> word }
    private val wordToIndex = indexToWord.mapIndexed { i, w -> w to i }.toMap()

    private val seqLength = 99
    private val interpreter = Interpreter(ByteBuffer.wrap(context.assets.open(modelPath).readBytes())).apply {
        allocateTensors()
    }

    fun predict(input: List<Int>, topn: Int = 10): List<CandidatesWindow.Candidate> {
        if(topn <= 0) return emptyList()
        val inputBuffer = ByteBuffer.allocateDirect(seqLength * Float.SIZE_BYTES)
        ((0 until seqLength).map { indexToWord.size } + input).takeLast(seqLength).forEach { inputBuffer.putFloat(it.toFloat()) }
        val outputBuffer = ByteBuffer.allocateDirect((indexToWord.size + 1) * Float.SIZE_BYTES)
        interpreter.run(inputBuffer, outputBuffer)
        val result = outputBuffer.asFloatBuffer().array()
        return result.mapIndexed { i, value -> i to value }.sortedByDescending { it.second }.take(topn)
            .map { (index, _) -> CandidatesWindow.Candidate(indexToWord[index], "") }
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