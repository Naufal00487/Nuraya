package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaTheme
import com.nuraya.app.ui.ReaderScreen
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingModesPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun choosingABlockHidesAndRestoresItsActualVerses() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        compose.setContent { NurayaTheme { Surface {
            ReaderScreen(app, UserSettings(), 2, 1, 0, emptyList(), perform = {}, onRead = { _, _ -> }, readingModeId = "hafalan_8")
        } } }
        val list = compose.onNodeWithTag("reader_verses")
        list.performScrollToNode(hasTestTag("memorization_block_2"))
        compose.onNodeWithTag("memorization_block_2").performClick()
        compose.onNodeWithTag("memorization_block_reference").assertTextEquals("Blok 2: 2:3–2:4")
        list.performScrollToNode(hasTestTag("recall_toggle"))
        compose.onNodeWithTag("recall_toggle").performScrollTo().performClick()
        list.performScrollToNode(hasTestTag("recall_hidden_2_3"))
        compose.onNodeWithTag("verse_text_2_3").assertDoesNotExist()
        compose.onNodeWithTag("recall_hidden_2_3").assertExists()
        list.performScrollToNode(hasTestTag("recall_toggle"))
        compose.onNodeWithTag("recall_toggle").performScrollTo().performClick()
        list.performScrollToNode(hasTestTag("verse_text_2_3"))
        compose.onNodeWithTag("verse_text_2_3").assertTextEquals(app.quran.verse(2, 3)!!.arabic)
        save("hafalan-eight-blocks.png")
    }

    @Test fun tikrarRequestsTheChosenRepeatCountForTheExactVerse() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        var request: List<Int>? = null
        compose.setContent { NurayaTheme { Surface {
            ReaderScreen(app, UserSettings(), 1, 1, 0, emptyList(), perform = {}, onRead = { _, _ -> }, readingModeId = "tikrar",
                onPracticeRange = { surah, start, end, repeats -> request = listOf(surah, start, end, repeats) })
        } } }
        val list = compose.onNodeWithTag("reader_verses")
        list.performScrollToNode(hasTestTag("tikrar_target_5"))
        compose.onNodeWithTag("tikrar_target_5").performClick()
        list.performScrollToNode(hasTestTag("play_verse_1_1"))
        compose.onNodeWithTag("play_verse_1_1").performScrollTo().performClick()
        assertEquals(listOf(1, 1, 1, 5), request)
        list.performScrollToNode(hasTestTag("tikrar_increment_1_1"))
        compose.onNodeWithTag("tikrar_increment_1_1").performScrollTo().performClick()
        compose.onNodeWithTag("tikrar_count_1_1").assertTextEquals("Latihan mandiri: 1 / 5 kali")
    }

    @Test fun waqafModeShowsSourcedStopSignsWithoutChangingTheCanonicalVerse() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val original = app.quran.verse(2, 2)!!.arabic
        compose.setContent { NurayaTheme { Surface {
            ReaderScreen(app, UserSettings(), 2, 2, 0, emptyList(), perform = {}, onRead = { _, _ -> }, readingModeId = "waqaf")
        } } }
        compose.onNodeWithTag("reader_verses").performScrollToNode(hasTestTag("verse_text_2_2"))
        compose.onNodeWithTag("verse_text_2_2").performScrollTo().assertTextContains("ۛ", substring = true)
        assertEquals(original, app.quran.verse(2, 2)!!.arabic)
        save("waqaf-reading.png")
    }

    @Test fun restartingTikrarForTheSameVerseUsesTheNewRepeatTarget() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val settings = UserSettings()
        var request: List<Int>? = null
        var toggled = false
        compose.setContent { NurayaTheme { Surface {
            ReaderScreen(app, settings, 1, 1, 0, emptyList(), perform = {}, onRead = { _, _ -> }, readingModeId = "tikrar",
                playback = PlaybackState(verseMode = true, surahId = 1, ayahId = 1, qariId = settings.readerQariId, isPlaying = false),
                onToggleAudio = { toggled = true },
                onPracticeRange = { surah, start, end, repeats -> request = listOf(surah, start, end, repeats) })
        } } }
        val list = compose.onNodeWithTag("reader_verses")
        list.performScrollToNode(hasTestTag("tikrar_target_5"))
        compose.onNodeWithTag("tikrar_target_5").performClick()
        list.performScrollToNode(hasTestTag("play_verse_1_1"))
        compose.onNodeWithTag("play_verse_1_1").performScrollTo().performClick()
        assertEquals(listOf(1, 1, 1, 5), request)
        assertFalse(toggled)
    }

    private fun save(name: String) {
        val directory = File(System.getProperty("user.home"), "ui-130").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            File(directory, name).outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
    }
}
