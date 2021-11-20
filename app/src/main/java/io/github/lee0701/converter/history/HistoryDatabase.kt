package io.github.lee0701.converter.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Word::class], version = 1, exportSchema = false)
abstract class HistoryDatabase: RoomDatabase() {
    abstract fun wordDao(): WordDao
}