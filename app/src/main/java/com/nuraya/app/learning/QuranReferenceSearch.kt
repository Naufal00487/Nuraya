package com.nuraya.app.learning

import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Surah

data class QuranReferenceResult(val chapters: List<Surah> = emptyList(), val ayah: Int? = null,
    val approximate: Boolean = false, val invalid: Boolean = false)

/** Resolve Indonesian names/numbers before relevance ranking. Never changes source text. */
class QuranReferenceSearch(private val quran: QuranRepository) {
    private val indonesianNames = listOf(
        "fatihah", "baqarah|baqoroh|baqaroh", "ali imran|aliimron", "nisa", "maidah", "anam", "araf", "anfal", "taubah|tawbah", "yunus",
        "hud", "yusuf", "rad", "ibrahim", "hijr", "nahl", "isra", "kahfi", "maryam", "taha|thaha", "anbiya", "hajj|haj", "muminun", "nur", "furqan",
        "syuara", "naml", "qasas|qashash", "ankabut", "rum", "luqman|lukman", "sajdah", "ahzab", "saba", "fatir|fathir", "yasin|ya sin", "saffat|shaffat", "sad|shad",
        "zumar", "gafir|ghafir|mumin", "fussilat|fushshilat", "syura", "zukhruf", "dukhan", "jasiyah|jatsiyah", "ahqaf", "muhammad", "fath", "hujurat", "qaf",
        "zariyat|dzariyat", "tur|thur", "najm", "qamar", "rahman|rohman", "waqiah", "hadid", "mujadilah", "hasyr", "mumtahanah", "saff|shaff", "jumuah",
        "munafiqun", "tagabun|taghabun", "talaq|thalaq", "tahrim", "mulk", "qalam", "haqqah", "maarij", "nuh", "jinn|jin", "muzzammil", "muddassir|muddatstsir",
        "qiyamah", "insan|dahr", "mursalat", "naba", "naziat", "abasa", "takwir", "infitar|infithar", "mutaffifin|muthaffifin", "insyiqaq", "buruj", "tariq|thariq",
        "ala", "gasyiyah|ghasyiyah", "fajr", "balad", "syams", "lail", "duha|dhuha", "syarh|insyirah", "tin", "alaq", "qadr", "bayyinah", "zalzalah|zilzal",
        "adiyat", "qariah", "takasur|takatsur", "asr|ashr", "humazah", "fil", "quraisy|quraish", "maun", "kausar|kautsar", "kafirun", "nasr|nashr", "lahab|masad", "ikhlas", "falaq", "nas"
    )
    private val prefixes = listOf("al", "an", "at", "as", "asy", "ash", "az", "ad", "ar")
    private fun key(value: String) = QuranRepository.normalizeSearch(value).replace(Regex("[^\\p{L}\\p{N}]"), "")
        .replace("ee", "i").replace("oo", "u").replace(Regex("([aiu])\\1+"), "$1").removeSuffix("h")
    private val aliases by lazy {
        check(indonesianNames.size == 114)
        quran.surahs.associateWith { surah ->
            val source = LocalLearningRepository.normalize(surah.name)
            val core = source.replace(Regex("^(al|an|at|as|ash|az|ad|ar) "), "")
            val words = indonesianNames[surah.id - 1].split('|') + core
            (words.flatMap { word -> listOf(word) + prefixes.map { "$it $word" } } + surah.name + surah.arabicName).map(::key).toSet()
        }
    }
    fun find(query: String): QuranReferenceResult? {
        if (Regex("(?:^|\\b(?:surat|surah|ayat)\\s+|:)\\s*-\\s*\\d", RegexOption.IGNORE_CASE).containsMatchIn(query))
            return QuranReferenceResult(invalid = true)
        val text = LocalLearningRepository.normalize(query)
        if (Regex("\\bayat(?:ul)? kursi\\b").containsMatchIn(text)) return QuranReferenceResult(listOf(quran.surahs[1]), 255)
        Regex("(?<!\\d)(\\d{1,4})\\s*[:.]\\s*(\\d{1,4})(?!\\d)").find(query)?.let {
            return numbered(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }
        val verseMarker = Regex("\\b(?:ayat|ayah|verse)\\b").find(text)
        val requestedAyah = verseMarker?.let { spokenNumber(text.substring(it.range.last + 1).trim().split(' ')) }
        val subject = text.substring(0, verseMarker?.range?.first ?: text.length)
        val explicit = Regex("\\b(surat|surah|suroh|surot|chapter|سورة)\\b").containsMatchIn(subject)
        val cleaned = subject.split(' ').filterNot { it in setOf("tolong", "cari", "carikan", "buka", "bukakan", "surat", "surah", "suroh", "surot", "chapter", "سورة", "nomor", "ke", "jelaskan", "tafsir") }.joinToString(" ")
        val number = spokenNumber(cleaned.split(' '))
        if (number != null && (explicit || cleaned.matches(Regex("\\d{1,4}")))) {
            if (verseMarker != null && requestedAyah == null) return QuranReferenceResult(invalid = true)
            return numbered(number, requestedAyah)
        }
        val name = key(cleaned)
        val exact = aliases.filterValues { name in it }.keys.toList()
        if (exact.isNotEmpty()) return validated(exact, requestedAyah, false, verseMarker != null && requestedAyah == null)
        // Do not turn common topic words such as "sabar" into a guessed surah.
        val topic = isTopic(text)
        if (!topic && name.length <= 60 && (explicit || verseMarker != null || name.length >= 6 && cleaned.split(' ').size <= 3)) {
            val scores = aliases.map { (surah, names) -> surah to names.minOf { distance(name, it) } }
            val best = scores.minOf { it.second }
            val limit = if (name.length >= 7) 2 else 1
            if (name.length >= 4 && best <= limit) return validated(scores.filter { it.second == best }.map { it.first }, requestedAyah, true, verseMarker != null && requestedAyah == null)
        }
        return if (explicit && !topic) QuranReferenceResult(invalid = number != null) else null
    }
    private fun numbered(surah: Int, ayah: Int?) = if (surah in 1..114) validated(listOf(quran.surahs[surah - 1]), ayah, false, false) else QuranReferenceResult(invalid = true)
    private fun validated(chapters: List<Surah>, ayah: Int?, approximate: Boolean, invalid: Boolean): QuranReferenceResult {
        val valid = chapters.filter { ayah == null || ayah in 1..it.ayahCount }
        return QuranReferenceResult(valid, ayah, approximate, invalid || valid.isEmpty())
    }
    fun preferredTranscript(alternatives: List<String>): String {
        val first = alternatives.firstOrNull().orEmpty()
        // A lower-confidence surah name must never replace a clear study-topic question.
        if (isTopic(LocalLearningRepository.normalize(first))) return first
        return alternatives.take(5).firstOrNull {
            find(it)?.let { result -> result.chapters.isNotEmpty() && !result.invalid && !result.approximate } == true
        } ?: first
    }

    private fun isTopic(text: String) = Regex("\\b(tentang|hukum|apa|bagaimana|belajar|makna|arti|sabar|kesabaran|salat|shalat|sholat|syukur|sedekah|hijrah|puasa|zakat|nikah|iman|rezeki|doa|waris|riba|ikhlas|taubat)\\b").containsMatchIn(text)

    companion object {
        private val units = mapOf("nol" to 0, "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4, "lima" to 5, "enam" to 6, "tujuh" to 7, "delapan" to 8, "sembilan" to 9)
        internal fun spokenNumber(words: List<String>): Int? {
            val parts = words.filter(String::isNotBlank)
            if (parts.isEmpty() || parts.any { !it.matches(Regex("\\d{1,4}")) && it !in units && it !in setOf("sepuluh", "sebelas", "belas", "puluh", "ratus", "seratus") }) return null
            if (parts.all { it in units || it.matches(Regex("\\d")) }) return parts.joinToString("") { (units[it] ?: it.toInt()).toString() }.toIntOrNull()
            var value = 0
            for (part in parts) when (part) {
                "seratus" -> value += 100
                "ratus" -> value = value.coerceAtLeast(1) * 100
                "puluh" -> value = value / 100 * 100 + value % 100 * 10
                "belas" -> value += 10
                "sepuluh" -> value += 10
                "sebelas" -> value += 11
                else -> value += units[part] ?: part.toInt()
            }
            return value
        }
        private fun distance(a: String, b: String): Int {
            var row = IntArray(b.length + 1) { it }
            for ((i, char) in a.withIndex()) {
                val next = IntArray(b.length + 1); next[0] = i + 1
                for (j in b.indices) next[j + 1] = minOf(next[j] + 1, row[j + 1] + 1, row[j] + if (char == b[j]) 0 else 1)
                row = next
            }
            return row.last()
        }
    }
}
