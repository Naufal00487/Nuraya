package com.nuraya.app.learning

import android.content.Context
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Verse
import com.nuraya.app.study.PracticeContent
import com.nuraya.app.study.StudyRepository
import com.nuraya.app.study.TajwidLesson
import java.io.DataInputStream
import java.text.Normalizer
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.ln
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Retrieved source passages, never a generated religious answer or a correctness verdict. */
data class LocalSourceMatch(val verse: Verse, val tafsirExcerpt: String?, val tafsirSource: String?, val excerptTruncated: Boolean)
data class LocalLearningResponse(
    val matches: List<LocalSourceMatch> = emptyList(),
    val lessons: List<TajwidLesson> = emptyList(),
    val status: String = "matches",
)

/** Uses a compact, reproducible index of all 6,236 translations and Kemenag tafsir entries. */
class LocalLearningRepository(
    context: Context,
    private val quran: QuranRepository,
    private val study: StudyRepository,
) {
    private val semanticIndex by lazy { SemanticVerseIndex(context.applicationContext) }
    private val spokenIndex by lazy { SpokenVerseSearch(quran) }
    private val assets = context.applicationContext.assets
    private val lessons by lazy { PracticeContent(context.applicationContext, quran).lessons }
    private data class Index(val lengths: IntArray, val postings: Map<String, IntArray>)
    private val index by lazy {
        DataInputStream(assets.open("learning_index.bin").buffered()).use { input ->
            require(input.readInt() == 0x4e4c4931) { "Invalid learning index" }
            val verses = input.readUnsignedShort()
            val terms = input.readInt()
            require(verses == 6236 && terms in 1..100000)
            val lengths = IntArray(verses) { input.readUnsignedShort() }
            val postings = HashMap<String, IntArray>(terms)
            repeat(terms) {
                val bytes = ByteArray(input.readUnsignedShort().also { require(it in 1..1024) })
                input.readFully(bytes)
                val count = input.readUnsignedShort().also { require(it in 1..verses) }
                val records = IntArray(count) {
                    val number = input.readUnsignedShort().also { require(it in 1..verses) }
                    val frequency = input.readUnsignedByte().also { require(it > 0) }
                    (number shl 8) or frequency
                }
                require(postings.put(String(bytes, Charsets.UTF_8), records) == null)
            }
            require(input.read() == -1)
            Index(lengths, postings)
        }
    }

    suspend fun find(question: String, semantic: Boolean = false): LocalLearningResponse = withContext(Dispatchers.IO) {
        require(question.trim().length in 2..1000) { "question_length" }
        if (question.any { it in 'ء'..'ي' }) return@withContext response(spokenIndex.find(question), emptyList(), emptyList())
        val normalized = normalize(question)
        val words = tokens(normalized)
        if (words.any { it in outsideScope }) return@withContext LocalLearningResponse(status = "outside_scope")

        val numeric = Regex("\\b(\\d{1,3})\\s*[:.]\\s*(\\d{1,3})\\b").find(question)
        if (numeric != null) {
            val verse = quran.verse(numeric.groupValues[1].toInt(), numeric.groupValues[2].toInt())
                ?: return@withContext LocalLearningResponse(status = "invalid_reference")
            return@withContext response(listOf(verse), emptyList(), words)
        }
        val named = namedReferences(normalized)
        if (named != null) return@withContext if (named.isEmpty()) LocalLearningResponse(status = "invalid_reference")
            else response(named, emptyList(), words)

        val requestedLessons = lessonMatches(normalized, words)
        if (requestedLessons.isNotEmpty()) {
            return@withContext response(requestedLessons.mapNotNull { quran.verse(it.surah, it.ayah) }.distinctBy { it.number }, requestedLessons, words)
        }
        val queryWords = (if (semantic) semanticIndex.queryTerms(question) else words.filterNot { it in stopWords }).distinct().take(16)
        if (queryWords.isEmpty()) return@withContext LocalLearningResponse(status = "no_results")
        val groups = queryWords.map { aliases[it] ?: setOf(it) }.distinct()
        val data = index
        val scores = DoubleArray(6236)
        val matches = IntArray(6236)
        val meanLength = data.lengths.average().coerceAtLeast(1.0)
        for (group in groups) {
            coroutineContext.ensureActive()
            val groupScores = DoubleArray(6236)
            for (term in group) {
                val postings = data.postings[term] ?: continue
                val idf = ln(1.0 + (6236 - postings.size + .5) / (postings.size + .5))
                for (record in postings) {
                    val position = (record ushr 8) - 1
                    val tf = (record and 255).toDouble()
                    val score = idf * (tf * 2.2) / (tf + 1.2 * (.25 + .75 * data.lengths[position] / meanLength))
                    groupScores[position] = maxOf(groupScores[position], score)
                }
            }
            for (position in groupScores.indices) if (groupScores[position] > 0) {
                matches[position]++
                scores[position] += groupScores[position]
            }
        }
        val required = if (groups.size <= 2) groups.size else ceil(groups.size * .67).toInt()
        if (semantic) {
            val meaning = semanticIndex.scores(question)
            val maxScore = scores.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val ranked = scores.indices.filter { scores[it] > 0 || meaning[it] >= .5f }.map { position ->
                val coverage = matches[position].toDouble() / groups.size
                val verse = quran.allVerses[position]
                val translation = tokens(normalize(verse.indonesian + " " + verse.english)).toSet()
                val direct = groups.count { group -> group.any { it in translation } }.toDouble() / groups.size
                verse to (scores[position] / maxScore * .55 + meaning[position] * .35 + coverage * .15 + direct * .45)
            }.sortedWith(compareByDescending<Pair<Verse, Double>> { it.second }.thenBy { it.first.number }).take(5).map { it.first }
            return@withContext response(ranked, emptyList(), queryWords)
        }
        val ranked = scores.indices.filter { matches[it] >= required && scores[it] > 0 }.map { position ->
            val verse = quran.allVerses[position]
            val translationWords = tokens(normalize(verse.indonesian + " " + verse.english)).toSet()
            val direct = groups.count { group -> group.any { it in translationWords } }
            verse to (scores[position] + direct * 3.0)
        }.sortedWith(compareByDescending<Pair<Verse, Double>> { it.second }.thenBy { it.first.number }).take(4).map { it.first }
        response(ranked, emptyList(), groups.flatten())
    }

    private suspend fun response(verses: List<Verse>, foundLessons: List<TajwidLesson>, terms: List<String>): LocalLearningResponse {
        val sources = verses.take(8).map { verse ->
            coroutineContext.ensureActive()
            val details = study.verse(verse.surah, verse.ayah)
            val full = details?.tafsir
            val excerpt = full?.let { selectExcerpt(it, terms) }
            LocalSourceMatch(verse, excerpt, details?.tafsirSource, excerpt != null && excerpt != full)
        }
        return LocalLearningResponse(sources, foundLessons, if (sources.isEmpty() && foundLessons.isEmpty()) "no_results" else "matches")
    }

    private fun namedReferences(query: String): List<Verse>? {
        if (Regex("\\bayat kursi\\b|\\bayatul kursi\\b").containsMatchIn(query)) return listOfNotNull(quran.verse(2, 255))
        // The corpus uses forms such as Al-Baqara/Al-Faatiha, while Indonesian
        // readers commonly type Al-Baqarah/Al-Fatihah. Preserve verse numbers.
        val padded = " ${surahSpelling(query)} "
        val chapter = quran.surahs.firstOrNull { surah ->
            val name = surahSpelling(surah.name)
            val variants = setOf(name, name.removePrefix("al "), name.removePrefix("an "), name.removePrefix("at "),
                name.removePrefix("as "), name.removePrefix("az "), name.removePrefix("ar "))
            variants.any { variant -> variant.length >= 3 && padded.contains(" $variant ") }
        } ?: return null
        val requested = Regex("\\b(?:ayat|ayah|verse) (\\d{1,3})\\b").find(query)?.groupValues?.get(1)?.toInt()
        return if (requested != null) listOfNotNull(quran.verse(chapter.id, requested)) else quran.verses(chapter.id).take(5)
    }

    private fun surahSpelling(value: String): String = normalize(value).split(' ').joinToString(" ") {
        it.replace(Regex("([aiu])\\1+"), "$1").removeSuffix("h")
    }

    private fun lessonMatches(query: String, words: List<String>): List<TajwidLesson> {
        val result = linkedSetOf<String>()
        val set = words.toSet()
        if (set.any { it in setOf("iqlab", "iqlap") }) result += "iqlab"
        if (set.any { it in setOf("ikhfa", "ikhfaa", "ikfa") }) result += "ikhfa"
        if (set.any { it in setOf("qalqalah", "qolqolah", "kalkalah", "pantulan") }) result += "qalqalah"
        if (set.any { it in setOf("mad", "madd", "harakat", "lengthening") } || query.contains("panjang pendek")) result += "madd_natural"
        if (set.any { it in setOf("ghunnah", "gunnah", "dengung", "nasal") } && !set.any { it in setOf("idgham", "idgam", "bilaghunnah", "bighunnah") }) result += "ghunnah"
        if (set.any { it in setOf("syamsiyah", "syamsiyyah", "shamsiyyah") }) result += "lam_shamsiyyah"
        if (set.any { it in setOf("idgham", "idgam") }) {
            if (query.contains("tanpa dengung") || query.contains("without ghunnah") || set.contains("bilaghunnah")) result += "idgham_no_ghunnah"
            else if (query.contains("dengan dengung") || set.contains("bighunnah")) result += "idgham_ghunnah"
            else result += listOf("idgham_ghunnah", "idgham_no_ghunnah")
        }
        if ((set.contains("nun") && set.any { it in setOf("mati", "sukun", "sakinah") } || set.contains("tanwin")) && set.any { it in setOf("ketemu", "bertemu", "diikuti", "sebelum", "meets", "before") }) {
            when {
                set.contains("ba") -> result += "iqlab"
                set.any { it in setOf("ya", "mim", "waw") } -> result += "idgham_ghunnah"
                set.any { it in setOf("lam", "ra") } -> result += "idgham_no_ghunnah"
            }
        }
        return lessons.filter { it.id in result }.take(3)
    }

    companion object {
        internal fun normalize(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
        private fun tokens(text: String) = text.split(' ').filter { it.length >= 2 }
        internal fun selectExcerpt(full: String, terms: List<String>): String {
            if (full.length <= 900) return full
            val paragraphs = full.split("\n\n").filter(String::isNotBlank)
            val best = paragraphs.maxByOrNull { paragraph ->
                val words = tokens(normalize(paragraph)).toSet()
                terms.distinct().count { it in words }
            } ?: full
            if (best.length <= 900) return best
            // Keep an exact contiguous source substring; the UI marks it as an excerpt.
            val end = best.lastIndexOf(' ', 900).takeIf { it >= 500 } ?: 900
            return best.substring(0, end)
        }
        private val stopWords = setOf("apa", "apakah", "itu", "yang", "dan", "atau", "untuk", "dari", "dalam", "dengan", "tentang", "bagaimana", "kenapa", "mengapa", "saya", "aku", "gw", "gua", "ingin", "mau", "tolong", "bisa", "carikan", "cari", "jelaskan", "penjelasan", "belajar", "pelajaran", "alquran", "quran", "al", "ayat", "surah", "surat", "tafsir", "arti", "makna", "menurut", "ada", "di", "ke", "ini", "adalah", "the", "what", "is", "are", "about", "how", "can", "please", "find", "explain", "of", "in", "and", "or", "to", "me", "on", "verse", "verses", "qur", "an", "does", "say")
        private val outsideScope = setOf("python", "javascript", "typescript", "password", "sandi", "credentials", "cuaca", "weather", "resep", "recipe", "football", "sepakbola", "casino", "bitcoin", "forex", "togel", "pornografi")
        private val aliasGroups = listOf(
            setOf("sabar", "bersabar", "kesabaran", "patient", "patience"),
            setOf("salat", "shalat", "sholat", "sembahyang", "prayer", "prayers"),
            setOf("sedekah", "bersedekah", "charity", "charitable"),
            setOf("syukur", "bersyukur", "gratitude", "grateful"),
            setOf("tobat", "taubat", "bertobat", "bertaubat", "repentance", "repent"),
            setOf("cobaan", "ujian", "musibah", "trial", "trials"),
            setOf("doa", "berdoa", "supplication"),
            setOf("orangtua", "parents"),
            setOf("susah", "kesusahan", "sulit", "kesulitan", "kesukaran", "kesempitan"),
            setOf("gosip", "ghibah", "menggunjing"),
            setOf("miskin", "kemiskinan"),
            setOf("sedih", "kesedihan", "bersedih", "sorrow", "grief"),
        )
        private val aliases = aliasGroups.flatMap { group -> group.map { it to group } }.toMap()
    }
}
