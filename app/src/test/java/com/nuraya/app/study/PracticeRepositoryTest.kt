package com.nuraya.app.study

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PracticeRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun reviewsScheduleFromActualReviewDateAndFinalVerseCompletesSessionAtomically() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "reviews.preferences_pb") })
        var date = LocalDate.of(2026, 10, 24)
        val repository = PracticeRepository(store, { s, a -> s == 1 && a in 1..7 }, { date })
        try {
            repository.reviewVerse(1, 2, RecallRating.CONFIDENT, finishSession = true)
            var state = repository.progress.first()
            assertEquals(1, state.completedSessions)
            assertEquals(1, state.totalReviews)
            assertEquals(3, state.reviews.single().intervalDays)
            assertEquals("2026-10-27", state.reviews.single().nextReviewOn)
            assertTrue(state.dueOn(date).isEmpty())

            date = date.plusDays(3)
            assertEquals("1:2", state.dueOn(date).single().reference)
            repository.reviewVerse(1, 2, RecallRating.CONFIDENT)
            state = repository.progress.first()
            assertEquals(6, state.reviews.single().intervalDays)
            assertEquals(2, state.totalReviews)
            assertEquals(1, state.reviews.size)

            repository.reviewVerse(1, 2, RecallRating.AGAIN)
            state = repository.progress.first()
            assertEquals(0, state.reviews.single().intervalDays)
            assertEquals(date.toString(), state.reviews.single().nextReviewOn)
            assertEquals(2, state.countOn(date))
            assertEquals(1, state.completedSessions)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun goalsQuizLessonsAndReviewRecordsSurviveClosingDataStore() = runBlocking {
        val file = File(folder.root, "persistent.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        var repository = PracticeRepository(store, { s, a -> s == 1 && a in 1..7 })
        repository.setDailyGoal(9)
        repository.reviewVerse(1, 1, RecallRating.ASSISTED)
        repository.recordQuizAnswer(true)
        repository.recordQuizAnswer(false)
        repository.completeLesson("ghunnah")
        repository.completeLesson("ghunnah")
        scope.coroutineContext[Job]!!.cancelAndJoin()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        repository = PracticeRepository(store, { s, a -> s == 1 && a in 1..7 })
        try {
            val restored = repository.progress.first()
            assertEquals(9, restored.dailyGoal)
            assertEquals(2, restored.quizAnswered)
            assertEquals(1, restored.quizCorrect)
            assertEquals(setOf("ghunnah"), restored.completedLessons)
            assertEquals("1:1", restored.reviews.single().reference)
            assertEquals(1, restored.reviews.single().intervalDays)
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun invalidVerseOrGoalNeverWritesProgress() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder.root, "invalid.preferences_pb") })
        val repository = PracticeRepository(store, { s, a -> s == 1 && a in 1..7 })
        try {
            try { repository.reviewVerse(1, 8, RecallRating.CONFIDENT); fail("Invalid verse accepted") } catch (_: IllegalArgumentException) { }
            try { repository.setDailyGoal(0); fail("Invalid goal accepted") } catch (_: IllegalArgumentException) { }
            assertEquals(PracticeProgress(), repository.progress.first())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }

    @Test fun dailyStreakUsesCalendarDaysAndAllowsTodayNotYetStarted() {
        val today = LocalDate.of(2026, 10, 24)
        val state = PracticeProgress(dailyCounts = mapOf("2026-10-22" to 2, "2026-10-23" to 1))
        assertEquals(2, state.streakOn(today))
        assertEquals(0, state.streakOn(today.plusDays(1)))
        assertEquals(3, state.copy(dailyCounts = state.dailyCounts + (today.toString() to 1)).streakOn(today))
    }

    @Test fun bundledLessonsQuoteActualVersesAndEveryQuestionHasAnAttributedLesson() {
        val quran = QuranRepository(context)
        val content = PracticeContent(context, quran)
        assertTrue(content.lessons.size >= 8)
        assertTrue(content.questions.size >= 8)
        assertEquals(content.lessons.size, content.lessons.map { it.id }.distinct().size)
        assertEquals(content.questions.size, content.questions.map { it.id }.distinct().size)
        for (lesson in content.lessons) {
            assertTrue(quran.verse(lesson.surah, lesson.ayah)!!.arabic.contains(lesson.excerpt))
            assertTrue(lesson.sourceUrl.startsWith("https://"))
            assertTrue(lesson.sourceTitle.isNotBlank())
            assertTrue(lesson.explanationId.isNotBlank() && lesson.explanationEn.isNotBlank())
        }
        for (question in content.questions) {
            assertTrue(content.lessons.any { it.id == question.lessonId })
            assertTrue(question.correctIndex in question.optionsId.indices)
            assertEquals(question.optionsId.size, question.optionsEn.size)
            assertEquals(question.optionsId.size, question.optionsId.distinct().size)
            assertTrue(question.explanationId.isNotBlank() && question.explanationEn.isNotBlank())
        }
    }
}
