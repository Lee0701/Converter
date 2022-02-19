package io.github.lee0701.converter.engine

interface Predictor {

    fun predict(context: String): Result

    interface Result {
        fun top(n: Int): List<String>
        fun score(candidate: String): Float
    }
}