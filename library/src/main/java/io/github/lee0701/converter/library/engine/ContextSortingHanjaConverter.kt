package io.github.lee0701.converter.library.engine

class ContextSortingHanjaConverter(
    private val hanjaConverter: HanjaConverter,
    private val predictor: Predictor,
): HanjaConverter {

    override fun convert(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate> {
        return hanjaConverter.convert(composingText).let { converted ->
            if(converted.isNotEmpty()) {
                val prediction = predictor.predict(composingText)
                return@let converted.sortedByDescending { prediction.score(it) }
            }
            return@let converted
        }
    }

    override fun convertPrefix(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<List<io.github.lee0701.converter.library.engine.Candidate>> {
        return hanjaConverter.convertPrefix(composingText).let { converted ->
            if(converted.any { it.isNotEmpty() }) {
                val prediction = predictor.predict(composingText)
                return@let converted.map { list ->
                    list.sortedByDescending { prediction.score(it) }
                }
            }
            return@let converted
        }
    }

}