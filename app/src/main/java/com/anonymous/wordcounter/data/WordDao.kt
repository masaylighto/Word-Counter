package com.anonymous.wordcounter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    fun observeWords(): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Query("SELECT * FROM words WHERE word = :word COLLATE NOCASE LIMIT 1")
    suspend fun findByWord(word: String): WordEntity?

    @Query("UPDATE words SET occurrence = occurrence + 1, updated_at = :updatedAt WHERE word = :word COLLATE NOCASE")
    suspend fun incrementOccurrence(word: String, updatedAt: Long): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT id FROM words WHERE word = :word COLLATE NOCASE AND id != :excludedId LIMIT 1")
    suspend fun findConflictingId(word: String, excludedId: Long): Long?

    @Query("UPDATE words SET occurrence = occurrence + :occurrence, meaning = :meaning, updated_at = :updatedAt WHERE id = :id")
    suspend fun mergeInto(id: Long, occurrence: Int, meaning: String, updatedAt: Long)

    @Query("UPDATE words SET word = :word, meaning = :meaning, occurrence = :occurrence, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateById(id: Long, word: String, meaning: String, occurrence: Int, updatedAt: Long)

    @Transaction
    suspend fun updateWordWithMerge(id: Long, word: String, meaning: String, occurrence: Int): Boolean {
        val now = System.currentTimeMillis()
        val conflictId = findConflictingId(word, id)
        return if (conflictId != null) {
            mergeInto(conflictId, occurrence, meaning, now)
            deleteById(id)
            true
        } else {
            updateById(id, word, meaning, occurrence, now)
            false
        }
    }
}
