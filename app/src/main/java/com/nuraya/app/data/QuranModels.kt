package com.nuraya.app.data

data class Surah(
    val id: Int,
    val name: String,
    val arabicName: String,
    val englishName: String,
    val ayahCount: Int,
    val revelation: String,
)

data class Verse(
    val number: Int,
    val surah: Int,
    val ayah: Int,
    val arabic: String,
    val indonesian: String,
    val english: String,
    val juz: Int,
    val transliteration: String? = null,
    val indonesianFootnotes: String? = null,
    /** Unnumbered chapter opening, separated verbatim from the source's first-verse prefix. */
    val openingBasmalah: String? = null,
)

data class Qari(
    val id: String,
    val name: String,
    val description: String,
    val sourceUrl: String,
    val baseUrl: String,
) {
    /** Islamic Network's whole-surah files use an unpadded surah number. */
    fun audioUrl(surah: Int): String {
        require(surah in 1..114) { "Surah must be between 1 and 114" }
        require(baseUrl.startsWith("https://")) { "Audio requires HTTPS" }
        return "${baseUrl.trimEnd('/')}/$surah.mp3"
    }
}
