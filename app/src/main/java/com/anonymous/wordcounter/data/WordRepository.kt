package com.anonymous.wordcounter.data

import kotlinx.coroutines.flow.Flow

class WordRepository(private val dao: WordDao) {
    fun observeWords(): Flow<List<WordEntity>> = dao.observeWords()

    suspend fun findWord(word: String): WordEntity? = dao.findByWord(word)

    suspend fun insertWord(word: String, meaning: String): Boolean {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            WordEntity(
                word = word,
                meaning = meaning,
                occurrence = 1,
                createdAt = now,
                updatedAt = now,
            )
        )
        return id != -1L
    }

    suspend fun incrementOccurrence(word: String): Boolean {
        val changed = dao.incrementOccurrence(word, System.currentTimeMillis())
        return changed > 0
    }

    suspend fun deleteWord(id: Long) = dao.deleteById(id)

    suspend fun updateWordWithMerge(id: Long, word: String, meaning: String, occurrence: Int): Boolean {
        return dao.updateWordWithMerge(id, word, meaning, occurrence)
    }
}
