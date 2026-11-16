package com.nuraya.app.study

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class LearningProgressTest {
    @get:Rule val folder=TemporaryFolder()
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    private val quran by lazy{QuranRepository(context)}
    private val course by lazy{LearningCourse(context,quran)}
    private suspend fun fixture(test:suspend (PracticeRepository,()->Unit)->Unit) {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        var date=LocalDate.of(2026,11,16)
        val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={File(folder.root,"learning.preferences_pb")})
        val repo=PracticeRepository(store,{s,a->quran.verse(s,a)!=null},{date})
        try{test(repo){date=date.plusDays(1)}}finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
    }
    private suspend fun finish(repo:PracticeRepository,wrongFirst:Boolean=false):LearningResult {
        while(true){
            val session=repo.progress.first().learning.active?:break
            val e=course.exercises(session.lessonId,session.seed)[session.baseIndex]
            val answer=if(!e.scored)listOf(RecallRating.CONFIDENT.name) else if(wrongFirst&&session.step==0)listOf(e.options.first{it.id!=e.expected.single()}.id) else e.expected
            repo.submitLearningAnswer(session.id,session.step,e,answer)
            repo.advanceLesson(session.id,session.step)
        }
        return repo.progress.first().learning.result!!
    }
    @Test fun duplicateChecksAndFinishesCannotAwardExtraPointsOrAnswers()=runBlocking {
        fixture { repo,_ ->
            assertFalse(repo.progress.first().learning.goalEnabled)
            repo.startLesson("tajwid_ghunnah",5,7)
            val start=repo.progress.first().learning.active!!;val e=course.exercises(start.lessonId,start.seed).first()
            repo.submitLearningAnswer(start.id,0,e,e.expected);repo.submitLearningAnswer(start.id,0,e,e.expected)
            assertEquals(1,repo.progress.first().quizAnswered)
            val result=finish(repo)
            assertEquals(20,result.xp);assertEquals(20,repo.progress.first().learning.xp)
            assertEquals(5,repo.progress.first().quizAnswered)
            repeat(3){repo.advanceLesson(start.id,4)}
            assertEquals(20,repo.progress.first().learning.xp);assertEquals(1,repo.progress.first().learning.sessions)
            assertEquals(1,repo.progress.first().learning.countOn(LocalDate.of(2026,11,16)))
        }
    }
    @Test fun errorsAreRevisitedOnceWithoutLivesPenaltyOrBlockingOtherLessons()=runBlocking {
        fixture { repo,_ ->
            repo.startLesson("tajwid_ghunnah",5,8)
            val result=finish(repo,wrongFirst=true)
            assertEquals(4,result.correct);assertEquals(5,result.total);assertEquals(1,result.reviewed)
            assertEquals(20,result.xp);assertEquals(5,repo.progress.first().quizAnswered)
            repo.startLesson("tajwid_waqaf",5,3)
            assertEquals("tajwid_waqaf",repo.progress.first().learning.active!!.lessonId)
        }
    }
    @Test fun oldProgressMigratesWithoutGivingInventedXpOrDeletingRecallHistory()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val file=File(folder.root,"legacy.preferences_pb")
        val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={file})
        store.edit { it[stringPreferencesKey("practice_state_v1")]= """{"schemaVersion":1,"dailyGoal":9,"reviews":[{"surah":112,"ayah":1,"rating":"ASSISTED","reviews":2,"intervalDays":1,"reviewedOn":"2026-11-15","nextReviewOn":"2026-11-16"}],"dailyCounts":{"2026-11-15":2},"totalReviews":2,"completedSessions":1,"quizAnswered":10,"quizCorrect":8,"completedLessons":["ghunnah"]}""" }
        val repo=PracticeRepository(store,{s,a->quran.verse(s,a)!=null})
        try{
            val old=repo.progress.first();assertEquals(9,old.dailyGoal);assertEquals(2,old.totalReviews);assertEquals(8,old.quizCorrect)
            assertEquals(setOf("ghunnah"),old.completedLessons);assertEquals(0,old.learning.xp);assertFalse(old.learning.goalEnabled)
            repo.setLearningGoal(3,true);repo.startLesson("tajwid_ghunnah",5,0);finish(repo)
            val updated=repo.progress.first();assertEquals(old.reviews,updated.reviews);assertEquals(old.dailyCounts,updated.dailyCounts);assertEquals(old.completedLessons,updated.completedLessons)
            assertEquals(9,updated.dailyGoal);assertEquals(3,updated.learning.goal);assertTrue(updated.learning.goalEnabled)
        }finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
    }
    @Test fun interruptedSessionRestoresSeedChoicesAnswerFeedbackAndRetryQueue()=runBlocking {
        val file=File(folder.root,"resume.preferences_pb")
        var scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        var repo=PracticeRepository(PreferenceDataStoreFactory.create(scope=scope,produceFile={file}),{s,a->quran.verse(s,a)!=null})
        repo.startLesson("tajwid_ghunnah",5,88)
        val before=repo.progress.first().learning.active!!;val e=course.exercises(before.lessonId,before.seed).first()
        val wrong=listOf(e.options.first{it.id!=e.expected.single()}.id)
        repo.submitLearningAnswer(before.id,0,e,wrong)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        repo=PracticeRepository(PreferenceDataStoreFactory.create(scope=scope,produceFile={file}),{s,a->quran.verse(s,a)!=null})
        try{
            val restored=repo.progress.first().learning.active!!
            assertEquals(before.id,restored.id);assertEquals(88,restored.seed);assertTrue(restored.checked)
            assertEquals(listOf(0),restored.retryIndices);assertEquals(wrong,restored.answers.single().response)
            assertEquals(e,course.exercises(restored.lessonId,restored.seed).first())
            val result=finish(repo);assertEquals(20,result.xp);assertEquals(1,repo.progress.first().learning.sessions)
        }finally{scope.coroutineContext[Job]!!.cancelAndJoin()}
    }
    @Test fun completionUsesActualLocalDayAndAPracticeBreakNeverRemovesXp()=runBlocking {
        fixture { repo,nextDay ->
            repo.startLesson("tajwid_ghunnah",5,0);nextDay();finish(repo)
            var state=repo.progress.first();assertEquals(0,state.learning.countOn(LocalDate.of(2026,11,16)))
            assertEquals(1,state.learning.countOn(LocalDate.of(2026,11,17)));assertEquals(1,state.streakOn(LocalDate.of(2026,11,18)))
            repeat(5){nextDay()};repo.startLesson("tajwid_ghunnah",5,1);val result=finish(repo)
            state=repo.progress.first();assertEquals(5,result.xp);assertEquals(25,state.learning.xp)
            assertEquals(2,state.learning.sessions);assertEquals(0,state.streakOn(LocalDate.of(2026,11,20)))
        }
    }
    @Test fun shortSurahSessionsSaveSelfRatingsSeparatelyFromKnowledgeScore()=runBlocking {
        fixture { repo,_ ->
            repo.startLesson("hafalan_112",7,2);val result=finish(repo)
            val state=repo.progress.first();assertEquals(3,result.total);assertEquals(3,state.quizAnswered)
            assertEquals(4,state.reviews.size);assertEquals((1..4).map{"112:$it"}.toSet(),state.reviews.map{it.reference}.toSet())
            assertEquals(4,state.totalReviews);assertEquals(1,state.completedSessions)
        }
    }
    @Test fun incompleteOrDuplicateOrderAnswersCannotBeSubmitted()=runBlocking {
        fixture { repo,_ ->
            repo.startLesson("hafalan_112",7,4)
            val s=repo.progress.first().learning.active!!;val e=course.exercises(s.lessonId,s.seed)[2]
            for(response in listOf(emptyList(),listOf(e.expected.first()),listOf(e.expected.first(),e.expected.first(),e.expected.first()))) {
                try{repo.submitLearningAnswer(s.id,0,e,response);fail("invalid answer accepted")}catch(_:IllegalArgumentException){}
            }
            assertTrue(repo.progress.first().learning.active!!.answers.isEmpty())
        }
    }
    @Test fun targetsAreOptionalAndBadgesCountOnlyPersonalCompletedActivity()=runBlocking {
        fixture { repo,nextDay ->
            assertFalse(repo.progress.first().learning.goalEnabled)
            for(id in listOf("tajwid_ghunnah","tajwid_qalqalah","tajwid_waqaf")){repo.startLesson(id,5,1);finish(repo);nextDay()}
            val badges=repo.progress.first().learning.badges
            assertTrue(badges.containsAll(setOf("first","tajwid","days")));assertFalse("surahs" in badges)
            repo.setLearningGoal(2,true);repo.setLearningGoal(2,false)
            assertFalse(repo.progress.first().learning.goalEnabled);assertEquals(60,repo.progress.first().learning.xp)
        }
    }
}
