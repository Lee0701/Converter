package io.github.lee0701.converter.library.engine

class CompoundHanjaConverter(
    val converters: List<HanjaConverter>,
): LearningHanjaConverter, PredictingHanjaConverter {

    override fun convert(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate> {
        val converted = converters.flatMap { it.convert(composingText) }
        val labeled = converted.map { word ->
            if(word.extra.isNotEmpty()) word
            else word.copy(extra = converted.find { it.hanja == word.hanja && it.extra.isNotEmpty() }?.extra ?: "")
        }
        return labeled.distinctBy { it.hanja }
    }

    override fun convertPrefix(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<List<io.github.lee0701.converter.library.engine.Candidate>> {
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
        converters.forEach { if(it is LearningHanjaConverter) it.learn(input, result) }
    }

    override fun predict(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate> {
        return converters.filterIsInstance<PredictingHanjaConverter>().flatMap { it.predict(composingText) }
    }

}