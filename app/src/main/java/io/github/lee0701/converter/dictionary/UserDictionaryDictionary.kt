package io.github.lee0701.converter.dictionary

import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase

class UserDictionaryDictionary(
    private val database: UserDictionaryDatabase,
): ListDictionary<HanjaDictionary.Entry> {
    override fun search(key: String): List<HanjaDictionary.Entry> {
        return database.dictionaryDao().getAllDictionaries().filter { it.enabled }.flatMap { dict ->
            database.wordDao().searchWords(dictionaryId = dict.id, hangul = key).toList()
        }.map { word ->
            HanjaDictionary.Entry(word.hanja, word.description, 0)
        }
    }
}