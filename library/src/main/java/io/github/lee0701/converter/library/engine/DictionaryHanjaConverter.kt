package io.github.lee0701.converter.library.engine

open class DictionaryHanjaConverter(
    private val dictionary: io.github.lee0701.converter.library.dictionary.ListDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry>,
): HanjaConverter {

    override fun convert(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<io.github.lee0701.converter.library.engine.Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map {
            io.github.lee0701.converter.library.engine.Candidate(
                word,
                it.result,
                it.extra ?: "",
                learnable = true
            )
        }
    }

    override fun convertPrefix(composingText: io.github.lee0701.converter.library.engine.ComposingText): List<List<io.github.lee0701.converter.library.engine.Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            val slicedWord = word.slice(0 .. i)
            dictionary.search(slicedWord)
                ?.sortedByDescending { it.frequency }
                ?.map {
                    io.github.lee0701.converter.library.engine.Candidate(
                        slicedWord,
                        it.result,
                        it.extra ?: "",
                        learnable = true
                    )
                } ?: emptyList()
        }
    }

}