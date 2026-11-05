package com.nuraya.app.learning

import android.content.Context
import com.nuraya.app.data.QuranRepository
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LearningRepository(context: Context, private val quran: QuranRepository) {
    private val baseUrl = runCatching {
        context.assets.open("ai_config.json").bufferedReader().use { reader ->
            JSONObject(reader.readText()).optString("baseUrl").trim().trimEnd('/')
        }
    }.getOrDefault("")

    val configured: Boolean get() = HttpsUrlPolicy.accepts(baseUrl)

    /** No key is stored or sent by the Android app. A deployment must protect its backend. */
    suspend fun ask(question: String, language: String): LearningAnswer = withContext(Dispatchers.IO) {
        require(configured) { "unconfigured" }
        require(question.trim().length in 5..2000) { "question_length" }
        val connection = URL("$baseUrl/v1/learn").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val payload = JSONObject().put("question", question.trim())
                .put("language", if (language == "en") "en" else "id").toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            when (connection.responseCode) {
                200 -> Unit
                401, 403 -> throw IOException("access_denied")
                429 -> throw IOException("rate_limited")
                503 -> throw IOException("unconfigured")
                else -> throw IOException("server_error")
            }
            val bytes = connection.inputStream.use { readBounded(it) }
            parseAnswer(String(bytes, Charsets.UTF_8), quran)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Does not depend on Java 9 InputStream methods absent on older Android devices. */
        internal fun readBounded(input: InputStream, limit: Int = 1_048_576): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) return output.toByteArray()
                if (output.size() + read > limit) throw IOException("invalid_response")
                output.write(buffer, 0, read)
            }
        }

        internal fun parseAnswer(json: String, quran: QuranRepository): LearningAnswer {
            val body = JSONObject(json)
            if (body.opt("answer") !is String) throw IOException("invalid_response")
            val answer = body.getString("answer").trim()
            if (answer.isEmpty() || answer.length > 14000) throw IOException("invalid_response")
            val rawCitations = body.optJSONArray("citations") ?: JSONArray()
            if (rawCitations.length() > 12) throw IOException("invalid_response")
            val references = (0 until rawCitations.length()).map { index ->
                val item = rawCitations.getJSONObject(index)
                val surah = item.opt("surah")
                val ayah = item.opt("ayah")
                if (surah !is Int || ayah !is Int) throw IOException("invalid_citation")
                QuranCitation(surah, ayah).also {
                    if (!it.structurallyValid() || quran.verse(it.surah, it.ayah) == null) {
                        throw IOException("invalid_citation")
                    }
                }
            }.distinct()
            // Generated quotation and source URL fields are deliberately ignored. The reader
            // renders source text from the bundled, attributed Quran corpus for each valid ID.
            return LearningAnswer(answer, references)
        }
    }
}
