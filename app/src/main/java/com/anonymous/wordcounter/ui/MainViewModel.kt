package com.anonymous.wordcounter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anonymous.wordcounter.data.AppDatabase
import com.anonymous.wordcounter.data.WordEntity
import com.anonymous.wordcounter.data.WordRepository
import com.anonymous.wordcounter.network.OpenAiClient
import com.anonymous.wordcounter.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SortMethod {
    OCCURRENCE,
    ALPHABETICAL,
    ADDED_DATE,
}

enum class DefinitionLanguage(val preferenceValue: String, val label: String) {
    ENGLISH("english", "English"),
    ARABIC("arabic", "Arabic");

    companion object {
        fun fromPreference(value: String): DefinitionLanguage {
            val normalized = value.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.preferenceValue == normalized } ?: ENGLISH
        }
    }
}

data class UiState(
    val wordInput: String = "",
    val meaningInput: String = "",
    val apiKeyInput: String = "",
    val definitionLanguage: DefinitionLanguage = DefinitionLanguage.ENGLISH,
    val statusText: String = "Ready",
    val errorText: String = "",
    val isBusy: Boolean = false,
    val needsDefinitionStep: Boolean = false,
    val sortMethod: SortMethod = SortMethod.OCCURRENCE,
    val sortAscending: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WordRepository(AppDatabase.get(application).wordDao())
    private val settingsStore = SettingsStore(application)
    private val initialDefinitionLanguage = DefinitionLanguage.fromPreference(settingsStore.loadDefinitionLanguage())

    private val _uiState = MutableStateFlow(
        UiState(
            apiKeyInput = settingsStore.loadApiKey(),
            definitionLanguage = initialDefinitionLanguage,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val sourceWords = repository.observeWords().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val words = combine(sourceWords, uiState) { entries, state ->
        sortWords(entries, state.sortMethod, state.sortAscending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val stats = sourceWords.map { entries ->
        val totalOccurrences = entries.sumOf { it.occurrence }
        entries.size to totalOccurrences
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0 to 0,
    )

    fun onWordInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            wordInput = value,
            needsDefinitionStep = false,
            errorText = "",
        )
    }

    fun onMeaningInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            meaningInput = value,
            errorText = "",
        )
    }

    fun onApiKeyInputChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKeyInput = value)
    }

    fun onDefinitionLanguageChange(language: DefinitionLanguage) {
        settingsStore.saveDefinitionLanguage(language.preferenceValue)
        _uiState.value = _uiState.value.copy(definitionLanguage = language)
    }

    fun onSortMethodChange(method: SortMethod) {
        _uiState.value = _uiState.value.copy(sortMethod = method)
    }

    fun toggleSortDirection() {
        _uiState.value = _uiState.value.copy(sortAscending = !_uiState.value.sortAscending)
    }

    fun onSortDirectionChange(ascending: Boolean) {
        _uiState.value = _uiState.value.copy(sortAscending = ascending)
    }

    fun addWord() {
        val word = _uiState.value.wordInput.trim()
        val meaning = _uiState.value.meaningInput.trim()

        if (word.isEmpty()) {
            setError("Please enter a German word.")
            return
        }

        launchBusyTask {
            val existing = repository.findWord(word)
            if (existing != null) {
                val incremented = repository.incrementOccurrence(word)
                if (!incremented) {
                    throw IllegalStateException("Could not update occurrence for \"$word\".")
                }
                _uiState.value = _uiState.value.copy(
                    wordInput = "",
                    meaningInput = "",
                    needsDefinitionStep = false,
                    errorText = "",
                    statusText = "Occurrence increased for \"$word\".",
                )
                return@launchBusyTask
            }

            if (meaning.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    needsDefinitionStep = true,
                    errorText = "",
                    statusText = "\"$word\" is new. Add meaning or tap Get Definition, then press Save Word.",
                )
                return@launchBusyTask
            }

            val inserted = repository.insertWord(word, meaning)
            if (!inserted) {
                repository.incrementOccurrence(word)
                _uiState.value = _uiState.value.copy(
                    wordInput = "",
                    meaningInput = "",
                    needsDefinitionStep = false,
                    errorText = "",
                    statusText = "Word already existed, occurrence increased for \"$word\".",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    wordInput = "",
                    meaningInput = "",
                    needsDefinitionStep = false,
                    errorText = "",
                    statusText = "Word added.",
                )
            }
        }
    }

    fun generateDefinition() {
        val word = _uiState.value.wordInput.trim()
        val apiKey = _uiState.value.apiKeyInput.trim()
        val language = _uiState.value.definitionLanguage.label

        if (word.isEmpty()) {
            setError("Enter a German word first.")
            return
        }

        if (apiKey.isEmpty()) {
            setError("API key needed. Add your OpenAI API key in Settings.")
            return
        }

        launchBusyTask {
            val definition = withContext(Dispatchers.IO) {
                OpenAiClient.generateDefinition(word, apiKey, language)
            }
            _uiState.value = _uiState.value.copy(
                meaningInput = definition,
                needsDefinitionStep = true,
                errorText = "",
                statusText = "Definition ready. Review it, then press Save Word.",
            )
        }
    }

    fun deleteWord(id: Long) {
        launchBusyTask {
            repository.deleteWord(id)
            _uiState.value = _uiState.value.copy(statusText = "Row deleted.")
        }
    }

    fun saveEdit(id: Long, word: String, meaning: String, occurrenceText: String, onMerged: (Boolean) -> Unit) {
        val trimmedWord = word.trim()
        val trimmedMeaning = meaning.trim()
        val occurrence = occurrenceText.trim().toIntOrNull()

        if (trimmedWord.isEmpty() || trimmedMeaning.isEmpty()) {
            setError("Please enter both word and meaning for editing.")
            return
        }

        if (occurrence == null || occurrence < 1) {
            setError("Occurrence must be a positive whole number.")
            return
        }

        launchBusyTask {
            val merged = repository.updateWordWithMerge(id, trimmedWord, trimmedMeaning, occurrence)
            if (merged) {
                _uiState.value = _uiState.value.copy(statusText = "Rows merged because the edited word already existed.")
            } else {
                _uiState.value = _uiState.value.copy(statusText = "Row updated.")
            }
            onMerged(merged)
        }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isEmpty()) {
            setError("API key cannot be empty.")
            return
        }

        launchBusyTask {
            withContext(Dispatchers.IO) {
                settingsStore.saveApiKey(key)
            }
            _uiState.value = _uiState.value.copy(statusText = "API key saved.")
        }
    }

    fun removeApiKey() {
        launchBusyTask {
            withContext(Dispatchers.IO) {
                settingsStore.clearApiKey()
            }
            _uiState.value = _uiState.value.copy(apiKeyInput = "", statusText = "API key removed.")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorText = "")
    }

    private fun launchBusyTask(task: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isBusy = true, errorText = "", statusText = "Working...")
                task()
                _uiState.value = _uiState.value.copy(isBusy = false)
            } catch (error: Exception) {
                val message = error.message?.takeIf { it.isNotBlank() } ?: "Unexpected error."
                _uiState.value = _uiState.value.copy(isBusy = false, errorText = message, statusText = "Action failed.")
            }
        }
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorText = message)
    }

    private fun sortWords(entries: List<WordEntity>, sortMethod: SortMethod, sortAscending: Boolean): List<WordEntity> {
        val comparator = when (sortMethod) {
            SortMethod.OCCURRENCE -> compareBy<WordEntity>({ it.occurrence }, { it.word.lowercase(Locale.ROOT) })
            SortMethod.ALPHABETICAL -> compareBy<WordEntity>({ it.word.lowercase(Locale.ROOT) }, { it.createdAt })
            SortMethod.ADDED_DATE -> compareBy<WordEntity>({ it.createdAt }, { it.word.lowercase(Locale.ROOT) })
        }

        val sorted = entries.sortedWith(comparator)
        return if (sortAscending) sorted else sorted.asReversed()
    }
}
