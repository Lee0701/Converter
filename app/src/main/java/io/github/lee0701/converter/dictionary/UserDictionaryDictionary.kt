package io.github.lee0701.converter.dictionary

import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase

class UserDictionaryDictionary(
    private val database: UserDictionaryDatabase,
): io.github.lee0701.converter.library.dictionary.ListDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry>,
    io.github.lee0701.converter.library.dictionary.PredictingDictionary<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry> {
    override fun search(key: String): List<io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry> {
        return database.dictionaryDao().getAllDictionaries().filter { it.enabled }.flatMap { dict ->
            database.wordDao().searchWords(dictionaryId = dict.id, hangul = key).toList()
        }.map { word ->
            io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry(word.hanja, word.description, 0)
        }
    }

    override fun predict(key: String): List<Pair<String, io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry>> {
        return database.dictionaryDao().getAllDictionaries().filter { it.enabled }.flatMap { dict ->
            database.wordDao().searchWordsPrefix(dictionaryId = dict.id, hangul = key).toList()
        }.map { word ->
            word.hangul to io.github.lee0701.converter.library.dictionary.HanjaDictionary.Entry(word.hanja, word.description, 0)
        }
    }
}