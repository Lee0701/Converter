package io.github.lee0701.converter.userdictionary

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_dictionary",
)
data class UserDictionary (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val enabled: Boolean = true,
)