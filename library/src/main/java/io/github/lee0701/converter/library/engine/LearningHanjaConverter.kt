package io.github.lee0701.converter.library.engine

interface LearningHanjaConverter: HanjaConverter {
    fun learn(input: String, result: String)
}