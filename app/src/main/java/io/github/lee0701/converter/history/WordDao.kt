package io.github.lee0701.converter.history

import androidx.room.*

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWords(vararg words: Word)

    @Update
    fun updateWords(vararg words: Word)

    @Delete
    fun deleteWords(vararg words: Word)

    @Query("SELECT * FROM word")
    fun getAllWords(): Array<Word>

    @Query("SELECT * FROM word WHERE input = :input AND result = :result")
    fun searchWords(input: String, result: String): Array<Word>

    @Query("SELECT * FROM word WHERE input = :input")
    fun searchWords(input: String): Array<Word>

    @Query("SELECT * FROM word WHERE lastUsed < :lastUsed")
    fun searchWordsOlderThan(lastUsed: Long): Array<Word>
}