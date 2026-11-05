package com.nuraya.app.study

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nuraya.app.data.QuranRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.practiceDataStore by preferencesDataStore(name = "nuraya_practice")

/** Explicit self-assessment and quiz activity, stored locally and separately from reader preferences. */
class PracticeRepository internal constructor(
    private val store: DataStore<Preferences>,
    private val isVerse: (Int, Int) -> Boolean,
    private val today: () -> LocalDate = { LocalDate.now() },
) {
    constructor(context: Context, quran: QuranRepository = QuranRepository(context.applicationContext)) : this(context.applicationContext.practiceDataStore,
        { s, a -> quran.verse(s, a) != null })

    private val stateKey = stringPreferencesKey("practice_state_v1")
    val progress: Flow<PracticeProgress> = store.data.map { decode(it[stateKey]) }

    suspend fun setDailyGoal(goal: Int) {
        require(goal in 1..50)
        update { it.copy(dailyGoal = goal) }
    }

    suspend fun reviewVerse(surah: Int, ayah: Int, rating: RecallRating, finishSession: Boolean = false) {
        require(isVerse(surah, ayah)) { "Unknown Quran verse" }
        val date = today()
        update { previous ->
            val old = previous.reviews.firstOrNull { it.surah == surah && it.ayah == ayah }
            val interval = when (rating) {
                RecallRating.AGAIN -> 0
                RecallRating.ASSISTED -> 1
                RecallRating.CONFIDENT -> if (old?.rating == RecallRating.CONFIDENT) (old.intervalDays * 2).coerceIn(3, 60) else 3
            }
            val record = MemorizationReview(surah, ayah, rating, (old?.reviews ?: 0) + 1, interval,
                date.toString(), date.plusDays(interval.toLong()).toString())
            val counts = previous.dailyCounts.toMutableMap().apply { put(date.toString(), (get(date.toString()) ?: 0) + 1) }
                .filterKeys { LocalDate.parse(it) >= date.minusDays(365) }
            previous.copy(reviews = previous.reviews.filterNot { it.reference == record.reference } + record,
                dailyCounts = counts, totalReviews = previous.totalReviews + 1,
                completedSessions = previous.completedSessions + if (finishSession) 1 else 0)
        }
    }

    suspend fun finishSession() = update { it.copy(completedSessions = it.completedSessions + 1) }

    suspend fun completeLesson(lessonId: String) {
        require(lessonId.matches(Regex("[a-z0-9_]{1,60}")))
        update { it.copy(completedLessons = it.completedLessons + lessonId) }
    }

    /** Call once when the learner locks an answer; changing a choice does not create extra scores. */
    suspend fun recordQuizAnswer(correct: Boolean) = update {
        it.copy(quizAnswered = it.quizAnswered + 1, quizCorrect = it.quizCorrect + if (correct) 1 else 0)
    }

    private suspend fun update(transform: (PracticeProgress) -> PracticeProgress) {
        store.edit { prefs -> prefs[stateKey] = encode(transform(decode(prefs[stateKey]))) }
    }

    private fun decode(value: String?): PracticeProgress {
        if (value == null) return PracticeProgress()
        val data = JSONObject(value)
        require(data.getInt("schemaVersion") == 1)
        val rows = data.getJSONArray("reviews")
        val reviews = List(rows.length()) { i -> rows.getJSONObject(i).let {
            MemorizationReview(it.getInt("surah"), it.getInt("ayah"), RecallRating.valueOf(it.getString("rating")),
                it.getInt("reviews"), it.getInt("intervalDays"), it.getString("reviewedOn"), it.getString("nextReviewOn"))
        } }
        val days = data.getJSONObject("dailyCounts")
        val lessons = data.getJSONArray("completedLessons")
        return PracticeProgress(data.getInt("dailyGoal"), reviews,
            days.keys().asSequence().associateWith { days.getInt(it) }, data.getInt("totalReviews"),
            data.getInt("completedSessions"), data.getInt("quizAnswered"), data.getInt("quizCorrect"),
            (0 until lessons.length()).map { lessons.getString(it) }.toSet())
    }

    private fun encode(state: PracticeProgress): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("dailyGoal", state.dailyGoal)
        put("reviews", JSONArray().apply { state.reviews.forEach { review -> put(JSONObject().apply {
            put("surah", review.surah); put("ayah", review.ayah); put("rating", review.rating.name)
            put("reviews", review.reviews); put("intervalDays", review.intervalDays)
            put("reviewedOn", review.reviewedOn); put("nextReviewOn", review.nextReviewOn)
        }) } })
        put("dailyCounts", JSONObject(state.dailyCounts))
        put("totalReviews", state.totalReviews); put("completedSessions", state.completedSessions)
        put("quizAnswered", state.quizAnswered); put("quizCorrect", state.quizCorrect)
        put("completedLessons", JSONArray(state.completedLessons.sorted()))
    }.toString()
}

class PracticeContent(context: Context, private val quran: QuranRepository) {
    private val root = context.assets.open("practice_lessons.json").bufferedReader().use { JSONObject(it.readText()) }
    val lessons: List<TajwidLesson> = root.getJSONArray("lessons").let { array -> List(array.length()) { i ->
        array.getJSONObject(i).let { row ->
            TajwidLesson(row.getString("id"), row.getString("titleId"), row.getString("titleEn"),
                row.getString("explanationId"), row.getString("explanationEn"), row.getString("rule"), row.getString("colorHex"),
                row.getInt("surah"), row.getInt("ayah"), row.getString("excerpt"), row.getString("sourceTitle"), row.getString("sourceUrl"))
        }.also { require(quran.verse(it.surah, it.ayah)?.arabic?.contains(it.excerpt) == true) { "Lesson excerpt differs from Quran source" } }
    } }
    val questions: List<TajwidQuestion> = root.getJSONArray("questions").let { array -> List(array.length()) { i ->
        array.getJSONObject(i).let { row ->
            fun strings(key: String): List<String> = row.getJSONArray(key).let { a -> List(a.length()) { a.getString(it) } }
            TajwidQuestion(row.getString("id"), row.getString("lessonId"), row.getString("questionId"), row.getString("questionEn"),
                strings("optionsId"), strings("optionsEn"), row.getInt("correctIndex"), row.getString("explanationId"), row.getString("explanationEn"))
        }.also { require(it.correctIndex in it.optionsId.indices && it.optionsId.size == it.optionsEn.size && lessons.any { lesson -> lesson.id == it.lessonId }) }
    } }
}
