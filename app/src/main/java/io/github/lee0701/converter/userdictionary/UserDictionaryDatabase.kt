package io.github.lee0701.converter.userdictionary

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserDictionary::class, UserDictionaryWord::class],
    version = 1,
    exportSchema = false
)
abstract class UserDictionaryDatabase: RoomDatabase() {
    abstract fun dictionaryDao(): UserDictionaryDao
    abstract fun wordDao(): UserDictionaryWordDao
}