package io.github.lee0701.converter.engine

import io.github.lee0701.converter.candidates.Candidate

class CompoundHanjaConverter(
    val converters: List<HanjaConverter>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val converted = converters.flatMap { it.convert(composingText) }
        val labeled = converted.map { word ->
            if(word.extra.isNotEmpty()) word
            else word.copy(extra = converted.find { it.hanja == word.hanja && it.extra.isNotEmpty() }?.extra ?: "")
        }
        return labeled.distinctBy { it.hanja }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        return converters.map { it.convertPrefix(composingText) }       // Per-length list of list of candidates
            .reduce { acc, list -> acc.zip(list).map { (l1, l2) ->
                val merged = l1 + l2                                    // Merge same-length candidates
                val labeled = merged.map { word ->
                    if(word.extra.isNotEmpty()) word
                    else word.copy(extra = merged.find { it.hanja == word.hanja && it.extra.isNotEmpty() }?.extra ?: "")
                                                                        // Copy extra label from existing one if non existent
                }
                labeled.distinctBy { it.hanja }                          // Remove duplications
            } }
    }

    override fun learn(input: String, result: String) {
        converters.forEach { it.learn(input, result) }
    }
}