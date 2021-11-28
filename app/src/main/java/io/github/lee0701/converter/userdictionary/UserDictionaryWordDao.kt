package io.github.lee0701.converter.userdictionary

import androidx.room.*

@Dao
interface UserDictionaryWordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWords(vararg words: UserDictionaryWord)

    @Update
    fun updateWords(vararg words: UserDictionaryWord)

    @Delete
    fun deleteWords(vararg words: UserDictionaryWord)

    @Query("SELECT * FROM user_dictionary_word WHERE dictionaryId = :dictionaryId")
    fun getAllWords(dictionaryId: Int): Array<UserDictionaryWord>

    @Query("SELECT * FROM user_dictionary_word WHERE dictionaryId = :dictionaryId AND hangul = :hangul AND hanja = :hanja")
    fun searchWords(dictionaryId: Int, hangul: String, hanja: String): Array<UserDictionaryWord>

    @Query("SELECT * FROM user_dictionary_word WHERE dictionaryId = :dictionaryId AND hangul = :hangul")
    fun searchWords(dictionaryId: Int, hangul: String): Array<UserDictionaryWord>

}