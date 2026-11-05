package com.nuraya.app.study

import java.time.LocalDate

enum class RecallRating { AGAIN, ASSISTED, CONFIDENT }

data class MemorizationReview(
    val surah: Int,
    val ayah: Int,
    val rating: RecallRating,
    val reviews: Int,
    val intervalDays: Int,
    val reviewedOn: String,
    val nextReviewOn: String,
) { val reference: String get() = "$surah:$ayah" }

data class PracticeProgress(
    val dailyGoal: Int = 5,
    val reviews: List<MemorizationReview> = emptyList(),
    val dailyCounts: Map<String, Int> = emptyMap(),
    val totalReviews: Int = 0,
    val completedSessions: Int = 0,
    val quizAnswered: Int = 0,
    val quizCorrect: Int = 0,
    val completedLessons: Set<String> = emptySet(),
) {
    fun countOn(date: LocalDate): Int = dailyCounts[date.toString()] ?: 0
    fun dueOn(date: LocalDate): List<MemorizationReview> = reviews.filter { LocalDate.parse(it.nextReviewOn) <= date }
        .sortedWith(compareBy({ it.nextReviewOn }, { it.surah }, { it.ayah }))
    fun streakOn(date: LocalDate): Int {
        var cursor = if (countOn(date) > 0) date else date.minusDays(1)
        var count = 0
        while (countOn(cursor) > 0) { count++; cursor = cursor.minusDays(1) }
        return count
    }
}

data class TajwidLesson(
    val id: String,
    val titleId: String,
    val titleEn: String,
    val explanationId: String,
    val explanationEn: String,
    val rule: String,
    val colorHex: String,
    val surah: Int,
    val ayah: Int,
    val excerpt: String,
    val sourceTitle: String,
    val sourceUrl: String,
)

data class TajwidQuestion(
    val id: String,
    val lessonId: String,
    val questionId: String,
    val questionEn: String,
    val optionsId: List<String>,
    val optionsEn: List<String>,
    val correctIndex: Int,
    val explanationId: String,
    val explanationEn: String,
)
