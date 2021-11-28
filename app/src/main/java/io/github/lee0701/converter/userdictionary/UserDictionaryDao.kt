package io.github.lee0701.converter.userdictionary

import androidx.room.*

@Dao
interface UserDictionaryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertDictionary(dictionary: UserDictionary)

    @Update
    fun updateDictionary(dictionary: UserDictionary)

    @Delete
    fun deleteDictionary(dictionary: UserDictionary)

    @Query("SELECT * FROM user_dictionary")
    fun getAllDictionaries(): Array<UserDictionary>

    @Query("SELECT * FROM user_dictionary WHERE id = :id")
    fun getDictionary(id: Int): UserDictionary?

}