package com.nuraya.app.data

/** Two presentation spans from one unchanged, pinned Tanzil source string. */
data class QuranOpeningText(val verseText: String, val openingBasmalah: String?)

/**
 * Tanzil's Uthmani export prefixes the unnumbered basmalah to 112 first-ayah
 * strings. Keep that span separate from the numbered verse in readers, search,
 * sharing and citations. Never renumber verses or normalize sacred text here.
 *
 * The raw asset stays verbatim. The two exact prefixes below preserve its
 * diacritics, including the source's extra shadda in the openings of 95 and 97.
 */
object QuranOpeningPolicy {
    private const val BASMALAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    private const val BASMALAH_WITH_SHADDA = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    private val prefixes = listOf(BASMALAH, BASMALAH_WITH_SHADDA)

    fun split(surah: Int, ayah: Int, rawArabic: String): QuranOpeningText {
        // Al-Fatihah's basmalah is 1:1 in this Hafs numbering. At-Tawbah has no
        // opening basmalah. An-Naml 27:30 and all other non-first ayat stay whole.
        if (surah !in 2..114 || surah == 9 || ayah != 1) {
            return QuranOpeningText(rawArabic, null)
        }
        val prefix = prefixes.firstOrNull { rawArabic.startsWith("$it ") }
            ?: return QuranOpeningText(rawArabic, null)
        val body = rawArabic.substring(prefix.length + 1)
        // Already-separated fixtures and unexpected input are not rewritten.
        // scripts/import_quran.py validates the pinned full corpus before build.
        if (body.isBlank()) return QuranOpeningText(rawArabic, null)
        return QuranOpeningText(body, prefix)
    }
}
