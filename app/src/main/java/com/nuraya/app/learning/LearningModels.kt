package com.nuraya.app.learning

import java.net.URI

/** All remotely configured links must preserve HTTPS and cannot contain credentials. */
object HttpsUrlPolicy {
    fun accepts(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        val host = uri.host?.lowercase() ?: return false
        val privateLiteral = host.matches(Regex("^(10|127)\\..*")) ||
            host.matches(Regex("^192\\.168\\..*")) || host.matches(Regex("^169\\.254\\..*")) ||
            host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")) ||
            host == "0.0.0.0" || host.startsWith("[")
        uri.scheme == "https" && uri.userInfo == null && uri.fragment == null && !privateLiteral &&
            host != "localhost" && !host.endsWith(".localhost") &&
            host != "127.0.0.1" && host != "[::1]" && host.contains('.') &&
            (uri.port == -1 || uri.port == 443)
    }.getOrDefault(false)
}

data class QuranCitation(val surah: Int, val ayah: Int) {
    val sourceUrl: String get() = "https://quran.com/$surah/$ayah"
    fun structurallyValid(): Boolean = surah in 1..114 && ayah in 1..286
}

data class LearningAnswer(val explanation: String, val citations: List<QuranCitation>)
