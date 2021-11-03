package io.github.lee0701.converter.history

import androidx.room.Entity

@Entity(primaryKeys = ["input", "result"])
data class Word(
    val input: String,
    val result: String,
    val count: Int,
    val lastUsed: Long,
) {
    fun usedOnce(): Word = this.copy(count = count + 1, lastUsed = System.currentTimeMillis())
}
