package com.nuraya.app.qa

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackService
import com.nuraya.app.study.*
import com.nuraya.app.ui.NurayaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import org.robolectric.util.ReflectionHelpers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=NurayaApplication::class,qualifiers="w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearningPresentationTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<NurayaApplication>()
    private val repo get()=PracticeRepository(app,app.quran)
    private val course by lazy{LearningCourse(app,app.quran)}
    @Before fun cleanPracticeState() = runBlocking<Unit> {
        ReflectionHelpers.getField<DataStore<Preferences>>(repo,"store").edit { it.clear() }
    }
    private fun state()=runBlocking{repo.progress.first()}
    private fun await(tag:String)=compose.waitUntil(30_000){compose.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    private fun open(tab:Int=1,scale:Float=1f,dark:Boolean=false) {
        if(runCatching{androidx.work.WorkManager.getInstance(app)}.isFailure)androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app,PlaybackService::class.java),Binder())
        runBlocking{app.settings.setDarkMode(dark)}
        compose.setContent { NurayaTheme(dark){CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)){
            Surface{PracticeScreen(app,"id",onRead={_,_->},initialTab=tab)}
        }} }
        await(if(tab==3)"learning_progress" else "learning_path")
    }
    private fun screenshot(name:String)=compose.runOnIdle {
        val view=hostWindow(compose.activity)
        val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(image))
        File("/workspace/scratch/study-redesign/screenshots/$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
    }
    private fun start(id:String) {
        compose.onNodeWithTag("learning_path").performScrollToNode(hasTestTag("learning_lesson_$id"))
        compose.onNodeWithTag("learning_lesson_$id").assertIsEnabled().performClick();await("lesson_preview")
        compose.onNodeWithTag("lesson_start").performScrollTo().performClick();await("lesson_session")
    }
    private fun choose(session:LearningSession,wrong:Boolean=false) {
        val e=course.exercises(session.lessonId,session.seed)[session.baseIndex]
        fun tap(tag:String){compose.onNodeWithTag("lesson_content").performScrollToNode(hasTestTag(tag));compose.onNodeWithTag(tag).performClick()}
        when(e.kind) {
            ExerciseKind.CHOICE -> tap("answer_"+if(wrong)e.options.first{it.id!=e.expected.single()}.id else e.expected.single())
            ExerciseKind.ORDER -> e.expected.forEach{tap("answer_$it")}
            ExerciseKind.MATCH -> e.left.forEachIndexed { index,left -> tap("match_left_$index");tap("match_right_${left.id}") }
            ExerciseKind.RECALL -> {tap("lesson_reveal");tap("recall_CONFIDENT")}
        }
        compose.onNodeWithTag("lesson_check").assertIsEnabled().performClick()
        compose.waitUntil(10_000){state().learning.active?.checked==true}
        await("lesson_feedback")
    }
    private fun advance(session:LearningSession) {
        compose.onNodeWithTag("lesson_next").performClick()
        compose.waitUntil(10_000){val s=state().learning.active;s==null||s.step!=session.step}
        compose.waitForIdle()
    }
    @Test fun allLevelsAreOpenImmediatelyAndAdvancedPreviewKeepsTheNurayaTheme() {
        open();screenshot("path-basic")
        compose.onNodeWithTag("learning_level_2").assertIsEnabled().performClick()
        compose.onNodeWithTag("learning_path").performScrollToNode(hasTestTag("learning_lesson_tajwid_waqaf"))
        compose.onNodeWithTag("learning_lesson_tajwid_waqaf").assertIsEnabled().performClick()
        await("lesson_preview");screenshot("advanced-preview")
        compose.onNodeWithTag("lesson_start").performScrollTo().performClick();await("lesson_session")
        assertEquals("tajwid_waqaf",state().learning.active!!.lessonId)
        compose.onNodeWithTag("lesson_check").assertIsNotEnabled();screenshot("question-blue")
    }
    @Test fun quizPausesWithFeedbackResumesAndReviewsMistakesBeforeSingleXpAward() {
        open();start("tajwid_ghunnah")
        val first=state().learning.active!!;choose(first,wrong=true);screenshot("answer-explanation")
        compose.onNodeWithTag("lesson_pause").performClick();await("learning_resume")
        assertEquals(0,state().learning.xp)
        compose.onNodeWithTag("learning_resume").performClick();await("lesson_feedback")
        assertEquals(first.id,state().learning.active!!.id)
        advance(first)
        var guard=0
        while(state().learning.active!=null&&guard++<10) {
            val session=state().learning.active!!;choose(session)
            if(course.exercises(session.lessonId,session.seed)[session.baseIndex].kind==ExerciseKind.MATCH)screenshot("match-exercise")
            advance(session)
        }
        await("lesson_result");compose.onNodeWithTag("lesson_xp").assertTextEquals("+20 XP")
        assertEquals(20,state().learning.xp);assertEquals(1,state().learning.sessions)
        assertEquals(4,state().learning.result!!.correct);screenshot("completed-lesson")
        compose.onNodeWithTag("lesson_done").performScrollTo().performClick();await("learning_path")
        compose.onNodeWithTag("practice_tab_3").performClick();await("learning_progress")
        compose.onNodeWithTag("learning_goal_enabled").performScrollTo().assertIsOff();screenshot("personal-progress")
    }
    @Test fun shortSurahPracticeUsesActualAyatAndRequiresRevealBeforeSelfRating() {
        open(tab=0);start("hafalan_112")
        var guard=0
        while(state().learning.active!=null&&guard++<10) {
            val s=state().learning.active!!;val e=course.exercises(s.lessonId,s.seed)[s.baseIndex]
            if(e.kind==ExerciseKind.RECALL) {
                compose.onNodeWithTag("lesson_check").assertIsNotEnabled()
                compose.onNodeWithTag("learning_verse_${e.surah}_${e.ayah}").assertDoesNotExist()
            }
            choose(s)
            if(e.kind==ExerciseKind.ORDER)screenshot("verse-order")
            if(e.kind==ExerciseKind.RECALL)screenshot("recall-self-check")
            advance(s)
        }
        await("lesson_result");assertEquals(3,state().learning.result!!.total)
        assertEquals((1..4).map{"112:$it"}.toSet(),state().reviews.map{it.reference}.toSet())
    }
    @Test @Config(qualifiers="w320dp-h640dp") fun largeTextKeepsLevelChoicesAndLessonActionsReachable() {
        open(scale=1.6f)
        compose.onNodeWithTag("learning_path").performScrollToNode(hasTestTag("learning_level_2"))
        compose.onNodeWithTag("learning_level_2").assertIsDisplayed().performClick()
        start("tajwid_waqaf")
        compose.onNodeWithTag("lesson_pause").assertIsDisplayed()
        compose.onNodeWithTag("lesson_check").assertIsDisplayed()
        choose(state().learning.active!!);compose.onNodeWithTag("lesson_next").assertIsDisplayed()
        screenshot("large-font-feedback")
    }
    @Test fun optionalGoalCanBeEnabledChangedAndDisabledWithoutChangingXp() {
        open(tab=3,dark=true)
        compose.onNodeWithTag("learning_goal_enabled").performScrollTo().assertIsOff().performClick()
        // DataStore writes and the following UI collection complete asynchronously.
        await("learning_goal_3")
        compose.onNodeWithTag("learning_goal_3").performScrollTo().performClick()
        compose.waitUntil(10_000){compose.onAllNodes(hasTestTag("learning_goal_3") and SemanticsMatcher.expectValue(SemanticsProperties.Selected,true)).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("learning_goal_3").assertIsSelected()
        assertEquals(3,state().learning.goal)
        screenshot("progress-dark")
        compose.onNodeWithTag("learning_goal_enabled").performScrollTo().performClick()
        compose.waitUntil(10_000){compose.onAllNodes(hasTestTag("learning_goal_enabled") and SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState,ToggleableState.Off)).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("learning_goal_enabled").assertIsOff()
        assertEquals(0,state().learning.xp)
    }
}
