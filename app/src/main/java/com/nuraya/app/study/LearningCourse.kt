package com.nuraya.app.study

import android.content.Context
import com.nuraya.app.data.QuranRepository
import org.json.JSONObject
import kotlin.random.Random

enum class LearningKind { TAJWID, MEMORIZE }
enum class ExerciseKind { CHOICE, MATCH, ORDER, RECALL }
data class LessonQuestion(val prompt: String, val options: List<String>, val correct: Int, val explanation: String)
data class LearningLesson(val id: String, val title: String, val summary: String, val level: Int, val kind: LearningKind,
    val explanation: String, val surah: Int, val ayah: Int, val sourceTitle: String, val sourceUrl: String,
    val questions: List<LessonQuestion>)
data class ExerciseOption(val id: String, val text: String)
data class LearningExercise(val id: String, val kind: ExerciseKind, val prompt: String,
    val options: List<ExerciseOption> = emptyList(), val expected: List<String> = emptyList(),
    val left: List<ExerciseOption> = emptyList(), val explanation: String,
    val surah: Int? = null, val ayah: Int? = null, val arabicOptions: Boolean = false) {
    fun correct(response: List<String>): Boolean = if (kind == ExerciseKind.RECALL)
        response.size == 1 && response.single() in RecallRating.entries.map { it.name }
        else response == expected
    val scored get() = kind != ExerciseKind.RECALL
}

/** Lessons and choices are local. Quran text is always taken from the unchanged canonical corpus. */
class LearningCourse(context: Context, private val quran: QuranRepository) {
    val lessons: List<LearningLesson>
    init {
        val root = context.assets.open("learning_course.json").bufferedReader().use { JSONObject(it.readText()) }
        require(root.getInt("schemaVersion") == 1)
        val array = root.getJSONArray("lessons")
        lessons = List(array.length()) { index ->
            val row = array.getJSONObject(index); val questions = row.getJSONArray("questions")
            LearningLesson(row.getString("id"), row.getString("title"), row.getString("summary"), row.getInt("level"),
                if(row.getString("kind") == "tajwid") LearningKind.TAJWID else LearningKind.MEMORIZE,
                row.getString("explanation"), row.getInt("surah"), row.getInt("ayah"), row.getString("sourceTitle"), row.getString("sourceUrl"),
                List(questions.length()) { q -> questions.getJSONObject(q).let { question ->
                    val options = question.getJSONArray("options")
                    LessonQuestion(question.getString("prompt"), List(options.length()) { options.getString(it) }, question.getInt("correct"), question.getString("explanation"))
                } })
        }
        require(lessons.map { it.id }.distinct().size == lessons.size)
        lessons.forEach { lesson ->
            require(lesson.level in 0..2 && quran.verse(lesson.surah, lesson.ayah) != null)
            require(lesson.id.matches(Regex("[a-z0-9_]{1,60}")))
            if (lesson.kind == LearningKind.TAJWID) require(lesson.questions.size == 4)
            else require(quran.verses(lesson.surah).size >= 3)
            lesson.questions.forEach { require(it.correct in it.options.indices && it.options.distinct().size == it.options.size) }
        }
    }
    fun lesson(id: String): LearningLesson = requireNotNull(lessons.firstOrNull { it.id == id })

    fun stepCount(id: String): Int = lesson(id).let { if(it.kind == LearningKind.TAJWID) 5 else 3 + quran.verses(it.surah).size }

    /** A seed keeps a saved session's shuffled choices stable after rotation or process restart. */
    fun exercises(lessonId: String, seed: Int): List<LearningExercise> {
        val lesson = lesson(lessonId); val random = Random(seed)
        return if (lesson.kind == LearningKind.TAJWID) {
            val questions = lesson.questions.mapIndexed { index, q ->
                LearningExercise("${lesson.id}_$index", ExerciseKind.CHOICE, q.prompt,
                    q.options.mapIndexed { i, text -> ExerciseOption(i.toString(), text) }.shuffled(random),
                    listOf(q.correct.toString()), explanation=q.explanation,
                    surah=if(index==2||q.prompt.contains("contoh ayat"))lesson.surah else null, ayah=if(index==2||q.prompt.contains("contoh ayat"))lesson.ayah else null)
            }
            val peers = (listOf(lesson) + lessons.filter { it.kind == LearningKind.TAJWID && it.id != lesson.id }.shuffled(random).take(2)).shuffled(random)
            val match = LearningExercise("${lesson.id}_match", ExerciseKind.MATCH, "Pasangkan nama hukum dengan cirinya.",
                peers.map { ExerciseOption(it.id, it.summary) }.shuffled(random), peers.map { it.id },
                peers.map { ExerciseOption(it.id, it.title) }, "Kenali ciri setiap hukum, lalu perhatikan penerapannya ketika membaca.")
            listOf(questions[0], questions[1], questions[2], match, questions[3])
        } else {
            val verses = quran.verses(lesson.surah)
            val starts = (0 until verses.lastIndex).shuffled(random).take(2)
            val next = starts.mapIndexed { index, start ->
                val cue = verses[start]; val expected = verses[start+1]
                val choices = (listOf(expected) + verses.filter { it.arabic != expected.arabic && it.ayah != cue.ayah }.distinctBy { it.arabic }.shuffled(random).take(2)).shuffled(random)
                LearningExercise("${lesson.id}_next_$index", ExerciseKind.CHOICE, "Ayat mana yang dibaca setelah ayat ini?",
                    choices.map { ExerciseOption(it.ayah.toString(),it.arabic) }, listOf(expected.ayah.toString()),
                    explanation="Setelah ayat ${cue.ayah}, lanjutkan dengan ayat ${expected.ayah} pada surah ${lesson.title}.",
                    surah=lesson.surah, ayah=cue.ayah, arabicOptions=true)
            }
            // Repeated ayat (for example 109:3 and 109:5) must never become
            // visually identical choices with different hidden answer IDs.
            val ordered = verses.windowed(3).filter { window -> window.map { it.arabic }.distinct().size == 3 }.random(random)
            val order = LearningExercise("${lesson.id}_order", ExerciseKind.ORDER, "Ketuk tiga ayat ini sesuai urutan bacaannya.",
                ordered.map { ExerciseOption(it.ayah.toString(),it.arabic) }.shuffled(random), ordered.map { it.ayah.toString() },
                explanation="Urutannya adalah ayat ${ordered.joinToString(", ") { it.ayah.toString() }}. Semua teks diambil dari surah ${lesson.title}.", arabicOptions=true)
            val recalls = verses.mapIndexed { index, verse ->
                LearningExercise("${lesson.id}_recall_$index",ExerciseKind.RECALL,"Coba baca ayat ${verse.ayah} dari ingatan.",
                    explanation="Periksa dengan teks asli, lalu catat penilaianmu. Ini catatan pribadi, bukan penilaian otomatis suara.",surah=lesson.surah,ayah=verse.ayah)
            }
            next + order + recalls
        }
    }
}

data class LearningAnswer(val correct: Boolean, val response: List<String>, val scored: Boolean = true)
data class LearningSession(val id: String, val lessonId: String, val seed: Int, val baseCount: Int,
    val step: Int = 0, val answers: List<LearningAnswer> = emptyList(), val retryIndices: List<Int> = emptyList()) {
    val totalSteps get() = baseCount + retryIndices.size
    val checked get() = answers.size > step
    val baseIndex get() = if(step < baseCount) step else retryIndices[step-baseCount]
    val reviewing get() = step >= baseCount
}
data class LessonRecord(val lessonId: String, val attempts: Int, val bestCorrect: Int, val total: Int, val lastDate: String)
data class LearningResult(val sessionId: String, val lessonId: String, val correct: Int, val total: Int, val xp: Int, val reviewed: Int)
data class LearningProgress(val xp: Int = 0, val goal: Int = 1, val goalEnabled: Boolean = false,
    val dailyLessons: Map<String,Int> = emptyMap(), val records: List<LessonRecord> = emptyList(),
    val active: LearningSession? = null, val result: LearningResult? = null) {
    fun countOn(date: java.time.LocalDate) = dailyLessons[date.toString()] ?: 0
    val sessions get() = records.sumOf { it.attempts }
    val badges: Set<String> get() = buildSet {
        if(sessions>=1)add("first")
        if(records.count { it.lessonId.startsWith("tajwid_") }>=3)add("tajwid")
        if(records.count { it.lessonId.startsWith("hafalan_") }>=3)add("surahs")
        if(dailyLessons.count { it.value>0 }>=3)add("days")
        if(xp>=100)add("xp")
        if(records.any { it.attempts>=3 })add("review")
    }
}
