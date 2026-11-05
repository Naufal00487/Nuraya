package com.nuraya.app.qa

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaTheme
import com.nuraya.app.ui.ReaderScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Real reader UI with explicit player state; this does not claim to play or hear audio. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuranRecitationPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun openingReaderDuringPlaybackFollowsTheActiveVerseAndItsButtonPauses() {
        var toggles = 0
        openReader(follow = true, onToggle = { toggles++ })

        compose.onNodeWithTag("verse_2_255").assertIsDisplayed()
        compose.onNodeWithTag("play_verse_2_255").performClick()
        assertEquals("The active verse button must toggle the existing audio", 1, toggles)
        compose.onNodeWithTag("basmalah_text_2").assertDoesNotExist()
    }

    @Test fun disablingFollowKeepsRequestedVerseAndStartsItsOwnAudio() {
        var requested: Pair<Int, Int>? = null
        openReader(follow = false, onPlay = { surah, ayah -> requested = surah to ayah })

        compose.onNodeWithTag("verse_text_2_1").assertTextEquals("الٓمٓ").assertIsDisplayed()
        compose.onNodeWithTag("play_verse_2_1").performClick()
        assertEquals(2 to 1, requested)
        compose.onNodeWithTag("basmalah_text_2").assertExists()
    }

    @Test fun changingReciterStartsSelectedVoiceInsteadOfPausingTheOldVoice() {
        var requested: Pair<Int, Int>? = null
        var toggles = 0
        openReader(follow = true, selectedQari = "ar.husary", onToggle = { toggles++ },
            onPlay = { surah, ayah -> requested = surah to ayah })
        compose.onNodeWithTag("play_verse_2_255").performClick()
        assertEquals(2 to 255, requested)
        assertEquals(0, toggles)
    }

    @Test fun currentWordHighlightUsesPackagedOffsetsEvenWithTajwidColorsOff() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val study = requireNotNull(app.study.verse(1, 1))
        val word = study.words.single { it.position == 2 }
        assertNotNull(word.start)
        assertNotNull(word.end)
        compose.setContent {
            NurayaTheme {
                Surface {
                    ReaderScreen(
                        app = app, settings = UserSettings(tajwidColors = false),
                        surahId = 1, ayahId = 1, juzId = 0, entries = emptyList(),
                        perform = {}, onRead = { _, _ -> },
                        playback = PlaybackState(surahId = 1, ayahId = 1, verseMode = true,
                            isPlaying = true, wordPosition = 2, wordTimingAvailable = true),
                    )
                }
            }
        }
        // Study loading uses IO in the real reader, so wait for the actual text decoration.
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                compose.onNodeWithTag("verse_text_1_1").fetchSemanticsNode()
                    .config[SemanticsProperties.Text].single().spanStyles.isNotEmpty()
            }.getOrDefault(false)
        }
        val text = compose.onNodeWithTag("verse_text_1_1").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single()
        assertEquals(app.quran.verse(1, 1)!!.arabic, text.text)
        assertEquals(1, text.spanStyles.size)
        assertEquals(word.start, text.spanStyles.single().start)
        assertEquals(word.end, text.spanStyles.single().end)
        assertEquals(word.arabic, text.text.substring(word.start!!, word.end!!))
    }

    private fun openReader(
        follow: Boolean,
        selectedQari: String = "ar.alafasy",
        onToggle: () -> Unit = {},
        onPlay: (Int, Int) -> Unit = { _, _ -> },
    ) {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        compose.setContent {
            NurayaTheme {
                Surface {
                    ReaderScreen(
                        app = app, settings = UserSettings(followRecitation = follow, readerQariId = selectedQari),
                        surahId = 2, ayahId = 1, juzId = 0, entries = emptyList(),
                        perform = {}, onRead = { _, _ -> },
                        playback = PlaybackState(surahId = 2, ayahId = 255, verseMode = true, isPlaying = true, qariId = "ar.alafasy"),
                        onPlayVerse = onPlay, onToggleAudio = onToggle,
                        reciterChoices = listOf("ar.alafasy" to "Mishary Rashid Alafasy"),
                    )
                }
            }
        }
        compose.waitForIdle()
    }
}
