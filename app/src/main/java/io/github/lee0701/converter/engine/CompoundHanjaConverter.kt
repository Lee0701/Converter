package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

class CompoundHanjaConverter(
    val converters: List<HanjaConverter>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        return converters.flatMap { it.convert(composingText) }.distinctBy { it.text }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        return converters.map { it.convertPrefix(composingText) }
            .reduce { acc, list -> acc.zip(list).map { (l1, l2) -> (l1 + l2).distinctBy { it.text } } }
    }

    override fun learn(input: String, result: String) {
        converters.forEach { it.learn(input, result) }
    }
}