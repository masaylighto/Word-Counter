package com.anonymous.wordcounter.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("word_counter_settings", Context.MODE_PRIVATE)

    fun loadApiKey(): String = preferences.getString(KEY_OPENAI_API, "").orEmpty()
    fun loadDefinitionLanguage(): String =
        preferences.getString(KEY_DEFINITION_LANGUAGE, DEFAULT_DEFINITION_LANGUAGE).orEmpty()

    fun saveApiKey(apiKey: String) {
        preferences.edit().putString(KEY_OPENAI_API, apiKey).apply()
    }

    fun saveDefinitionLanguage(language: String) {
        preferences.edit().putString(KEY_DEFINITION_LANGUAGE, language).apply()
    }

    fun clearApiKey() {
        preferences.edit().remove(KEY_OPENAI_API).apply()
    }

    companion object {
        private const val KEY_OPENAI_API = "openai_api_key"
        private const val KEY_DEFINITION_LANGUAGE = "definition_language"
        private const val DEFAULT_DEFINITION_LANGUAGE = "english"
    }
}
