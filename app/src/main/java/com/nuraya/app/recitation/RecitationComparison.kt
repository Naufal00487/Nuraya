package com.nuraya.app.recitation

import java.text.Normalizer

enum class WordMatch { MATCHED, DIFFERENT, NOT_HEARD, PENDING }

data class ComparedWord(val text: String, val start: Int, val end: Int, val match: WordMatch)
data class RecitationComparison(
    val words: List<ComparedWord>,
    val extraWords: List<String>,
    val finalResult: Boolean,
) {
    val matchedCount: Int get() = words.count { it.match == WordMatch.MATCHED }
}

/** Text comparison only: normalization deliberately discards vowel/recitation information. */
object RecitationMatcher {
    private data class Token(val original: String, val normal: String, val start: Int, val end: Int, val accepted: Set<String>)
    private val tokenPattern = Regex("\\S+")

    fun normalizeWord(text: String): String = buildString {
        Normalizer.normalize(text, Normalizer.Form.NFKC).forEach { char ->
            when (char) {
                'أ', 'إ', 'آ', 'ٱ' -> append('ا')
                'ى' -> append('ي')
                'ؤ' -> append('و')
                'ئ' -> append('ي')
                'ـ' -> Unit
                else -> if (char in '\u0621'..'\u063A' || char in '\u0641'..'\u064A') append(char)
            }
        }
    }

    /** Dagger alef can be represented or omitted in ordinary orthography; ordinary alefs stay intact. */
    internal fun acceptedSpellings(source: String): Set<String> {
        var variants = setOf("")
        Normalizer.normalize(source, Normalizer.Form.NFKC).forEach { char ->
            if (char == '\u0670') {
                variants = (variants + variants.map { it + "ا" }).take(32).toSet()
            } else {
                val normalized = normalizeWord(char.toString())
                if (normalized.isNotEmpty()) variants = variants.map { it + normalized }.toSet()
            }
        }
        val plain = normalizeWord(source)
        // Conservative, explicit Uthmani spellings. Do not generally turn waw into alef:
        // in وَٰحِد it is a consonant, while in صَلَوٰة it represents the long vowel.
        val stems = mapOf("صلوة" to "صلاة", "زكوة" to "زكاة", "حيوة" to "حياة")
        val prefixes = listOf("", "ال", "و", "وال", "ب", "بال", "ل", "لل", "ف", "فال")
        if ('\u0670' in source) for ((uthmani, conventional) in stems) for (prefix in prefixes) {
            if (plain == prefix + uthmani) variants = variants + (prefix + conventional)
        }
        return variants
    }

    private fun tokenize(text: String, source: Boolean = false): List<Token> = tokenPattern.findAll(text).mapNotNull { match ->
        val normalized = normalizeWord(match.value)
        if (normalized.isEmpty()) null else Token(match.value, normalized, match.range.first, match.range.last + 1,
            if (source) acceptedSpellings(match.value) else setOf(normalized))
    }.toList()

    fun compare(expected: String, recognized: String, isFinal: Boolean): RecitationComparison {
        // Quran verses are finite; bound externally supplied recognition text to avoid excessive allocation.
        val target = tokenize(expected, source = true).take(300)
        val heard = tokenize(recognized.take(10_000)).take(350)
        val n = target.size
        val m = heard.size
        val cost = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) cost[i][0] = i
        for (j in 0..m) cost[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            val substitute = cost[i - 1][j - 1] + if (heard[j - 1].normal in target[i - 1].accepted) 0 else 1
            cost[i][j] = minOf(substitute, cost[i - 1][j] + 1, cost[i][j - 1] + 1)
        }
        val states = MutableList(n) { if (isFinal) WordMatch.NOT_HEARD else WordMatch.PENDING }
        val extra = mutableListOf<String>()
        // Partial recognition may only cover a prefix. Never count its unfinished tail as missing.
        var i = if (isFinal) n else (0..n).minByOrNull { cost[it][m] } ?: 0
        var j = m
        while (i > 0 || j > 0) {
            val equal = i > 0 && j > 0 && heard[j - 1].normal in target[i - 1].accepted
            when {
                i > 0 && j > 0 && cost[i][j] == cost[i - 1][j - 1] + (if (equal) 0 else 1) -> {
                    states[i - 1] = if (equal) WordMatch.MATCHED else if (isFinal) WordMatch.DIFFERENT else WordMatch.PENDING
                    i--; j--
                }
                i > 0 && cost[i][j] == cost[i - 1][j] + 1 -> { i-- }
                else -> { if (isFinal) extra += heard[j - 1].original; j-- }
            }
        }
        return RecitationComparison(target.mapIndexed { index, token ->
            ComparedWord(token.original, token.start, token.end, states[index])
        }, extra.asReversed(), isFinal)
    }
}
