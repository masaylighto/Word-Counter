package com.anonymous.wordcounter.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("word_counter_settings", Context.MODE_PRIVATE)

    fun loadApiKey(): String = preferences.getString(KEY_OPENAI_API, "").orEmpty()

    fun saveApiKey(apiKey: String) {
        preferences.edit().putString(KEY_OPENAI_API, apiKey).apply()
    }

    fun clearApiKey() {
        preferences.edit().remove(KEY_OPENAI_API).apply()
    }

    companion object {
        private const val KEY_OPENAI_API = "openai_api_key"
    }
}
