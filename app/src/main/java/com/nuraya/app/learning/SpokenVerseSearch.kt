package com.nuraya.app.learning

import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Verse
import com.nuraya.app.recitation.RecitationMatcher

/** Finds a spoken fragment anywhere in a verse; repeated Quran phrases remain separate references. */
class SpokenVerseSearch(private val quran: QuranRepository) {
    private val source by lazy { quran.allVerses.map { verse ->
        verse.arabic.split(Regex("\\s+")).map { RecitationMatcher.acceptedSpellings(it).map { spelling -> spelling.replace("ءا", "ا") }.toSet() }.filter { words -> words.any(String::isNotEmpty) }
    } }
    fun find(transcript: String): List<Verse> {
        val heard = transcript.take(2000).split(Regex("\\s+")).map { RecitationMatcher.normalizeWord(it).replace("ءا", "ا") }.filter(String::isNotBlank).take(60)
        if (heard.size < 2) return emptyList()
        val ranked = source.mapIndexedNotNull { index, tokens ->
            // Semi-global edit distance: the fragment may start/end anywhere in the verse.
            var previous = IntArray(tokens.size + 1)
            for ((i, word) in heard.withIndex()) {
                val current = IntArray(tokens.size + 1); current[0] = i + 1
                for (j in tokens.indices) current[j + 1] = minOf(previous[j + 1] + 1, current[j] + 1, previous[j] + if (word in tokens[j]) 0 else 1)
                previous = current
            }
            val distance = previous.minOrNull() ?: heard.size
            val score = 1f - distance.toFloat() / heard.size
            val required = if (heard.size < 5) 1f else .72f
            if (score < required) null else Triple(quran.allVerses[index], score, tokens.size)
        }.sortedWith(compareByDescending<Triple<Verse, Float, Int>> { it.second }.thenBy { it.third }.thenBy { it.first.number })
        return ranked.take(8).map { it.first }
    }
}
