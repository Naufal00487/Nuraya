package com.nuraya.app.media

import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Verse

/** Canonical global numbers preserve chapter boundaries without inserting an extra basmalah. */
fun validVerseSpan(quran: QuranRepository, startSurah: Int, startAyah: Int, endSurah: Int, endAyah: Int,
                   repeatEach: Int, passes: Int): Boolean {
    val first = quran.verse(startSurah, startAyah) ?: return false
    val last = quran.verse(endSurah, endAyah) ?: return false
    return last.number >= first.number && repeatEach in 1..20 && passes in 1..20 && repeatEach * passes <= 20 &&
        (last.number - first.number + 1) * repeatEach * passes <= 12_500
}

fun verseSpanSequence(quran: QuranRepository, startSurah: Int, startAyah: Int, endSurah: Int, endAyah: Int,
                      repeatEach: Int, passes: Int): List<Verse> {
    require(validVerseSpan(quran, startSurah, startAyah, endSurah, endAyah, repeatEach, passes))
    val first = requireNotNull(quran.verse(startSurah, startAyah))
    val last = requireNotNull(quran.verse(endSurah, endAyah))
    val pass = quran.allVerses.subList(first.number - 1, last.number).flatMap { verse -> List(repeatEach) { verse } }
    return List(passes) { pass }.flatten()
}
