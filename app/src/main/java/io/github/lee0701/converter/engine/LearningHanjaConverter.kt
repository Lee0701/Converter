package io.github.lee0701.converter.engine

interface LearningHanjaConverter: HanjaConverter {
    fun learn(input: String, result: String)
}