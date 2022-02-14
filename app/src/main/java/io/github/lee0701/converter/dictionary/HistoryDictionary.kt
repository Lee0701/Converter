package io.github.lee0701.converter.dictionary

import io.github.lee0701.converter.history.HistoryDatabase

class HistoryDictionary(
    private val database: HistoryDatabase
): ListDictionary<HanjaDictionary.Entry>, PredictingDictionary<HanjaDictionary.Entry> {
    override fun search(key: String): List<HanjaDictionary.Entry> {
        return database.wordDao().searchWords(key).map { word -> HanjaDictionary.Entry(word.result, "", word.count) }
    }

    override fun predict(key: String): List<Pair<String, HanjaDictionary.Entry>> {
        return database.wordDao().searchWordsPrefix(key).map { word -> word.input to HanjaDictionary.Entry(word.result, "", word.count) }
    }
}