package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.study.PracticeScreen
import com.nuraya.app.ui.NurayaTheme
import com.nuraya.app.ui.ReaderScreen
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Real host-rendered study interactions; no microphone, device playback, or network claims. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudyFeaturePresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun memorizationRangeAndRevealedVerseSurviveVisitingProgress() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        var audioRange: List<Int>? = null
        compose.setContent {
            NurayaTheme {
                Surface {
                    PracticeScreen(app, "id", onRead = { _, _ -> }, onPracticeRange = { surah, start, end, repeats ->
                        audioRange = listOf(surah, start, end, repeats)
                    })
                }
            }
        }
        compose.onNodeWithTag("practice_start").performTextReplacement("2")
        compose.onNodeWithText("Dengarkan rentang").performScrollTo().performClick()
        assertEquals(listOf(1, 2, 3, 3), audioRange)

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithTag("memorization_hidden").assertExists()
        compose.onNodeWithTag("memorization_text").assertDoesNotExist()
        compose.onNodeWithTag("memorization_reveal").performScrollTo().performClick()
        compose.onNodeWithTag("memorization_text").performScrollTo()
            .assertTextEquals(app.quran.verse(1, 2)!!.arabic)
        saveHostImage("practice-revealed-verse.png")

        compose.onNodeWithTag("practice_tab_3").performClick()
        compose.onNodeWithText("Perjalanan belajar Anda").assertIsDisplayed()
        compose.onNodeWithTag("practice_tab_0").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithTag("memorization_text").assertTextEquals(app.quran.verse(1, 2)!!.arabic)
        compose.onNodeWithTag("memorization_hidden").assertDoesNotExist()

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        compose.onNodeWithTag("practice_start").assertTextContains("2")
        compose.onNodeWithTag("practice_end").assertTextContains("3")
    }

    @Test fun readerShowsSourceColorsWordMeaningsAndTheFullProvidedTafsir() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val study = requireNotNull(app.study.verse(1, 1))
        compose.setContent {
            NurayaTheme {
                Surface {
                    ReaderScreen(app, UserSettings(), 1, 1, 0, emptyList(), perform = {}, onRead = { _, _ -> })
                }
            }
        }
        val firstVerse = hasAnyAncestor(hasTestTag("verse_1_1"))
        // The real reader loads study data on Dispatchers.IO. Wait for its study
        // control, then inspect the settled text; do not swallow semantics/assertion
        // errors while polling span decoration, which hides the cause of a timeout.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasText("Arti per kata") and firstVerse)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        val displayed = compose.onNodeWithTag("verse_text_1_1").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single()
        assertEquals(study.arabic, displayed.text)
        assertEquals(study.tajwidSpans.size, displayed.spanStyles.size)

        compose.onNode(hasText("Arti per kata") and firstVerse).performScrollTo().performClick()
        compose.onNode(hasText(study.words.first().indonesian!!) and firstVerse).assertIsDisplayed()
        saveHostImage("reader-tajwid-word-meanings.png")

        compose.onNode(hasText("Tafsir") and firstVerse).performScrollTo().performClick()
        compose.onNode(hasText(study.tafsirSource!!) and firstVerse).assertExists()
        compose.onNode(hasText(study.tafsir!!) and firstVerse).assertExists()
    }

    private fun saveHostImage(name: String) {
        val output = File(System.getProperty("user.home"), "study-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(output, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
}
