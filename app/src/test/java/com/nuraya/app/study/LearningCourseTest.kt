package com.nuraya.app.study

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class LearningCourseTest {
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    @Test fun allTwentyFiveLessonsAreAvailableAtEveryLevelWithoutProgressOrAccount() {
        val course=LearningCourse(context,QuranRepository(context))
        assertEquals(25,course.lessons.size)
        assertEquals(12,course.lessons.count{it.kind==LearningKind.TAJWID})
        assertEquals(13,course.lessons.count{it.kind==LearningKind.MEMORIZE})
        for(kind in LearningKind.entries)for(level in 0..2)assertTrue(course.lessons.any{it.level==level&&it.kind==kind})
        course.lessons.forEach{assertEquals(course.stepCount(it.id),course.exercises(it.id,9).size)}
    }
    @Test fun seededLessonsResumeExactlyAndNewSeedsActuallyVaryTheChoices() {
        val course=LearningCourse(context,QuranRepository(context))
        for(lesson in course.lessons) {
            assertEquals(course.exercises(lesson.id,876),course.exercises(lesson.id,876))
            assertTrue((0..5).map{course.exercises(lesson.id,it)}.distinct().size>1)
        }
    }
    @Test fun everyArabicChoiceAndVerseOrderComesFromTheCanonicalSurah() {
        val quran=QuranRepository(context);val course=LearningCourse(context,quran)
        for(lesson in course.lessons)for(seed in 0..25) {
            for(exercise in course.exercises(lesson.id,seed)) {
                if(exercise.surah!=null)assertNotNull(quran.verse(exercise.surah,exercise.ayah!!))
                if(exercise.arabicOptions) {
                    assertEquals("Ambiguous choices in ${lesson.id} at seed $seed", exercise.options.size, exercise.options.map{it.text}.distinct().size)
                    exercise.options.forEach{assertEquals(quran.verse(lesson.surah,it.id.toInt())!!.arabic,it.text)}
                    if(exercise.kind==ExerciseKind.ORDER)assertEquals(exercise.expected.map{it.toInt()}.sorted(),exercise.expected.map{it.toInt()})
                    if(exercise.kind==ExerciseKind.CHOICE)assertEquals(exercise.ayah!!+1,exercise.expected.single().toInt())
                }
                if(exercise.scored)assertTrue(exercise.correct(exercise.expected))
                assertEquals(exercise.options.size,exercise.options.map{it.id}.distinct().size)
                if(exercise.kind==ExerciseKind.MATCH)assertEquals(exercise.left.map{it.id},exercise.expected)
            }
        }
    }
    @Test fun recallIsAnExplicitSelfRatingAndHasNoAutomaticCorrectnessScore() {
        val course=LearningCourse(context,QuranRepository(context))
        val exercises=course.exercises("hafalan_112",0)
        assertEquals(4,exercises.count{!it.scored})
        course.lessons.filter{it.kind==LearningKind.MEMORIZE}.forEach{lesson ->
            assertEquals(QuranRepository(context).verses(lesson.surah).map{it.ayah},course.exercises(lesson.id,0).filter{it.kind==ExerciseKind.RECALL}.map{it.ayah})
        }
        exercises.filter{it.kind==ExerciseKind.RECALL}.forEach{e->
            for(r in RecallRating.entries)assertTrue(e.correct(listOf(r.name)))
            assertFalse(e.correct(listOf("automatically perfect")))
        }
    }
}
