package io.github.lee0701.converter.userdictionary

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE

@Entity(
    tableName = "user_dictionary_word",
    primaryKeys = ["dictionaryId", "hangul", "hanja"],
    foreignKeys = [
        ForeignKey(
            entity = UserDictionary::class,
            parentColumns = ["id"],
            childColumns = ["dictionaryId"],
            onDelete = CASCADE
        )
    ]
)
data class UserDictionaryWord(
    val dictionaryId: Int,
    val hangul: String,
    val hanja: String,
    val description: String,
)