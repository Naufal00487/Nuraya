package com.nuraya.app.quranreading

import android.content.ComponentName
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.*
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
class ReadingRegression153Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun app(): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, com.nuraya.app.media.PlaybackService::class.java), Binder())
        app.quran.surahs
        return app
    }

    @Test fun pausedBaqarahSixCannotOverrideAnyOfThe114SelectedSurahsOrAnyPageMapping() {
        val app = app()
        val old = PlaybackState(surahId = 2, ayahId = 6, verseMode = true, isPlaying = false)
        for (edition in MushafEdition.entries) {
            val images = MushafImages(app, edition)
            for (surah in app.quran.surahs) {
                assertFalse("${edition.id}/${surah.id}", shouldFollowBookAudio(surah.id, PlaybackState(), old, true))
                assertFalse(shouldFollowBookAudio(surah.id, old, old, true))
                val first = images.references.single { it.surah == surah.id && it.ayah == 1 }
                assertEquals(first.page, images.pageFor(surah.id, 1))
                for (verse in app.quran.verses(surah.id)) assertTrue(images.references.any { it.surah == verse.surah && it.ayah == verse.ayah && it.page == images.pageFor(verse.surah, verse.ayah) })
            }
        }
        val active = old.copy(isPlaying = true)
        assertFalse(shouldFollowBookAudio(67, old, active, true))
        assertTrue(shouldFollowBookAudio(2, old, active, true))
        assertFalse(shouldFollowBookAudio(2, active, active, true))
        assertTrue(shouldFollowBookAudio(2, active, active.copy(ayahId = 7), true))
        assertFalse(shouldFollowBookAudio(2, active, active.copy(ayahId = 7), false))
    }

    @Test fun everySurahActuallyRendersItsOwnFirstVerseWithOldAudioSelected() {
        val app = app()
        app.playback.playVerseRange(2, 6, 6, "ar.alafasy")
        var chosen by mutableIntStateOf(1)
        compose.setContent { NurayaTheme { Surface { key(chosen) {
            StudyBookScreen(app, UserSettings(), chosen, 1, modeId = "standard", onBack = {}, onOpenMushaf = { _, _ -> })
        } } } }
        for (id in 1..114) {
            compose.runOnIdle { chosen = id }
            compose.onNodeWithTag("study_reference").assertTextEquals("$id:1")
            compose.onNodeWithTag("verse_text_${id}_1").assertTextEquals(app.quran.verse(id, 1)!!.arabic)
            compose.onNodeWithTag("study_controls").assertDoesNotExist()
        }
    }

    @Test fun memorizationPlaysTheChosenBlockAndTikrarKeepsItsOwnCounter() {
        val app = app()
        var mode by mutableStateOf("hafalan_8")
        compose.setContent { NurayaTheme { Surface { key(mode) {
            StudyBookScreen(app, UserSettings(), 67, 1, modeId = mode, onBack = {}, onOpenMushaf = { _, _ -> })
        } } } }
        compose.onNodeWithTag("memorization_block_2").performClick()
        val block = memorizationBlocks(app.quran.verses(67))[1]
        compose.onNodeWithTag("study_reference").assertTextEquals("Blok 2 / 8 · ${block.reference}")
        compose.onNodeWithTag("recall_toggle").performClick()
        compose.onNodeWithTag("recall_hidden").assertExists()
        compose.onNodeWithTag("verse_text_67_${block.start.ayah}").assertDoesNotExist()
        compose.onNodeWithTag("recall_toggle").performClick()
        compose.onNodeWithTag("verse_text_67_${block.start.ayah}").assertExists()
        compose.onNodeWithTag("study_listen").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(67, app.playback.state.value.surahId)
            assertEquals(block.start.ayah, app.playback.state.value.rangeStart)
            assertEquals(block.end.ayah, app.playback.state.value.rangeEnd)
            mode = "tikrar"
        }
        compose.onNodeWithTag("tikrar_target_5").performClick()
        compose.onNodeWithTag("tikrar_increment_67_1").performScrollTo().performClick()
        compose.onNodeWithTag("tikrar_count_67_1").assertTextEquals("Latihan mandiri: 1 / 5 kali")
        assertEquals(1, TikrarProgress(app).count(67, 1))
        compose.onNodeWithTag("study_listen").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(67, app.playback.state.value.surahId)
            assertEquals(1, app.playback.state.value.rangeStart)
            assertEquals(1, app.playback.state.value.rangeEnd)
        }
    }

    @Test fun everyVisibleReciterHasAnExplicitSourcePortraitAndNoDuplicatePerson() {
        val app = app()
        val portraits = QariPortraitCatalog(app)
        val chapters = app.quran.qariChoices
        val verses = app.playback.verseReciters
        assertEquals(5, chapters.size)
        assertEquals(7, verses.size)
        for (ids in listOf(chapters.map { it.id }, verses.map { it.id })) {
            val mapped = ids.map { id -> requireNotNull(portraits.find(id)) }
            assertTrue(mapped.all { it.sourceName == "Quran.com" })
            assertEquals(mapped.size, mapped.map { it.id }.toSet().size)
        }
        assertFalse(verses.any { it.id == "ar.parhizgar" })
        assertFalse(chapters.any { it.id == "ar.mahmoodalrifai" })
    }

    @Test fun farSurahInImageReaderKeepsItsPageAndCompanionInsteadOfOldAudio() {
        val app = app()
        app.playback.playVerseRange(2, 6, 6, "ar.alafasy")
        var chosen by mutableIntStateOf(18)
        compose.setContent { NurayaTheme { Surface { key(chosen) {
            MadinahScreen("id", chosen, 1, app = app, edition = MushafEdition.INDONESIA) { _, _ -> }
        } } } }
        val pages = MushafImages(app, MushafEdition.INDONESIA)
        for (id in listOf(18, 36, 67, 112, 114)) {
            compose.runOnIdle { chosen = id }
            compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { click(center) }
            compose.mainClock.advanceTimeBy(400)
            compose.onNodeWithTag("mushaf_page_number").assertTextEquals("Halaman ${pages.pageFor(id, 1)} / 604")
            compose.onNodeWithTag("mushaf_translation").performClick()
            compose.onNodeWithText("${app.quran.surahs[id - 1].name} · 1").assertExists()
            compose.onNodeWithContentDescription("Tutup panel arti").performClick()
        }
    }
}
