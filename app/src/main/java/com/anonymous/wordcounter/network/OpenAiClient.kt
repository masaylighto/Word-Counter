package com.anonymous.wordcounter.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object OpenAiClient {
    private const val OPENAI_URL = "https://api.openai.com/v1/responses"
    private const val MODEL = "gpt-4.1-mini"

    fun generateDefinition(word: String, apiKey: String): String {
        val payload = JSONObject().apply {
            put("model", MODEL)
            put(
                "input",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a concise bilingual dictionary assistant.")
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put(
                                "content",
                                "Define the German word \"$word\". Return exactly in this format with no extra text:\n" +
                                    "German:\n<1-2 concise sentences in German>\n\n" +
                                    "English:\n<1-2 concise sentences in English>",
                            )
                        }
                    )
                }
            )
        }

        val connection = (URL(OPENAI_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        connection.outputStream.use { out ->
            out.write(payload.toString().toByteArray(Charsets.UTF_8))
        }

        val status = connection.responseCode
        val body = readResponseBody(connection, status in 200..299)

        if (status !in 200..299) {
            val message = runCatching {
                JSONObject(body).optJSONObject("error")?.optString("message")
            }.getOrNull().orEmpty().ifBlank { "HTTP $status" }
            throw IllegalStateException("OpenAI request failed: $message")
        }

        val json = JSONObject(body)
        val outputText = json.optString("output_text").trim()
        if (outputText.isNotBlank()) {
            return outputText
        }

        val output = json.optJSONArray("output")
        val parts = mutableListOf<String>()
        if (output != null) {
            for (i in 0 until output.length()) {
                val outputItem = output.optJSONObject(i) ?: continue
                val content = outputItem.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val contentItem = content.optJSONObject(j) ?: continue
                    val text = contentItem.optString("text").trim()
                    if (text.isNotBlank()) {
                        parts += text
                    }
                }
            }
        }

        val merged = parts.joinToString("\n").trim()
        if (merged.isBlank()) {
            throw IllegalStateException("No definition text returned from OpenAI.")
        }

        return merged
    }

    private fun readResponseBody(connection: HttpURLConnection, useInputStream: Boolean): String {
        val stream = if (useInputStream) connection.inputStream else connection.errorStream
        if (stream == null) {
            return ""
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
            buildString {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    append(line)
                }
            }
        }
    }
}
