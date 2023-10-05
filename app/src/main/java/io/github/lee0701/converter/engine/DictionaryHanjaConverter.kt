package io.github.lee0701.converter.engine

open class DictionaryHanjaConverter(
    private val dictionary: io.github.lee0701.converter.dictionary.ListDictionary<io.github.lee0701.converter.dictionary.HanjaDictionary.Entry>,
): HanjaConverter {

    override fun convert(composingText: ComposingText): List<Candidate> {
        val word = composingText.composing.toString()
        val result = dictionary.search(word) ?: emptyList()
        return result.map {
            Candidate(
                word,
                it.result,
                it.extra ?: "",
                learnable = true
            )
        }
    }

    override fun convertPrefix(composingText: ComposingText): List<List<Candidate>> {
        val word = composingText.composing.toString()
        return word.indices.reversed().map { i ->
            val slicedWord = word.slice(0 .. i)
            dictionary.search(slicedWord)
                ?.sortedByDescending { it.frequency }
                ?.map {
                    Candidate(
                        slicedWord,
                        it.result,
                        it.extra ?: "",
                        learnable = true
                    )
                } ?: emptyList()
        }
    }

}