package com.nuraya.app.study

/** Display ranges use Kotlin/Compose UTF-16 indices into [StudyVerse.arabic]. */
data class TajwidSpan(val start: Int, val end: Int, val rule: String)

data class StudyWord(
    val position: Int,
    val arabic: String,
    val indonesian: String?,
    val english: String?,
    /** Null means the source word could not be mapped exactly to the reader text. */
    val start: Int? = null,
    val end: Int? = null,
)

data class StudyVerse(
    val surah: Int,
    val ayah: Int,
    val arabic: String,
    val tajwidSpans: List<TajwidSpan>,
    val words: List<StudyWord>,
    val tafsir: String?,
    val tafsirSource: String?,
    val tafsirLanguage: String?,
    val wordsSource: String? = null,
    val tajwidSource: String? = null,
)
