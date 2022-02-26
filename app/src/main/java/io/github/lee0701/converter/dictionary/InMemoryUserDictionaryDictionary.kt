package io.github.lee0701.converter.dictionary

import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase

class InMemoryUserDictionaryDictionary(
    database: UserDictionaryDatabase,
): ReadOnlyTrieDictionary<List<HanjaDictionary.Entry>>(
    database.dictionaryDao().getAllDictionaries().filter { it.enabled }
        .flatMap { database.wordDao().getAllWords(it.id).toList() }
        .groupBy { word -> word.hangul }
        .mapValues { (_, list) -> list.map { HanjaDictionary.Entry(it.hanja, it.description, 0) } }
), ListDictionary<HanjaDictionary.Entry>