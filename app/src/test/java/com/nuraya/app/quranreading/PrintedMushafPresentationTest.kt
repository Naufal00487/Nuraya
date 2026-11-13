package com.nuraya.app.quranreading

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.MadinahScreen
import com.nuraya.app.ui.NurayaTheme
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
class PrintedMushafPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun app(): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.nuraya.app.media.PlaybackService::class.java), android.os.Binder())
        return app
    }
    private fun open(edition: PrintedMushafEdition, mode: String, surah: Int = 1, ayah: Int = 1): NurayaApplication {
        val app = app()
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id", surah, ayah, app = app,
            edition = QuranReadingModes.pageEdition(mode), readingModeId = mode, printedEdition = edition) { _, _ -> } } } }
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("mushaf_page_image").fetchSemanticsNodes().isNotEmpty() }
        return app
    }
    private fun controls() {
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = org.robolectric.shadows.ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView ?: compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(System.getProperty("user.home"), "ui-160/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun perKataOriginalPageIsFullScreenAndLongPressSelectsThePrintedVerse() {
        val edition = PrintedMushafEdition.WORD_TAJWID
        val app = open(edition, "word_tajwid")
        compose.onNodeWithContentDescription("Per kata & tajwid halaman 1").assertIsDisplayed()
        compose.onNodeWithTag("mushaf_controls").assertDoesNotExist()
        screenshot("per-kata-full")
        val repo = MushafImages(app, printedEdition = edition)
        val region = repo.regionsFor(1).single { it.surah == 1 && it.ayah == 3 }.rectangles.first()
        compose.onNodeWithTag("mushaf_page_image").performTouchInput {
            val fit = MushafImagePlacement.fit(1080, 2202, width.toFloat(), height.toFloat())
            longClick(Offset(fit.left + (region.left + region.right) / 2f * fit.scale, fit.top + (region.top + region.bottom) / 2f * fit.scale))
        }
        compose.onNodeWithTag("mushaf_selected_verse").assertTextEquals("Al-Faatiha · 3")
        compose.onNodeWithTag("mushaf_highlight_1_3").assertExists()
        screenshot("per-kata-verse-actions")
        compose.onNodeWithText("Terjemahan & latin").performClick()
        compose.onNodeWithTag("mushaf_translation_panel").assertIsDisplayed()
        compose.onNodeWithText("Al-Faatiha · 3").assertExists()
        screenshot("per-kata-translation")
    }

    @Test fun farSurahDoesNotJumpToStaleBaqarahAudioAndCanPlayItsOwnVerse() {
        val app = app()
        app.playback.playVerseRange(2, 6, 7, "ar.alafasy")
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id", 114, 1, app = app,
            edition = MushafEdition.INDONESIA, readingModeId = "latin_tajwid", printedEdition = PrintedMushafEdition.LATIN_TAJWID) { _, _ -> } } } }
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("mushaf_page_image").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Latin & tajwid halaman 604").assertIsDisplayed()
        screenshot("latin-an-nas-full")
        controls()
        compose.onNodeWithTag("mushaf_page_number").assertTextEquals("Halaman 604 / 604")
        compose.onNodeWithTag("mushaf_play").performClick()
        compose.runOnIdle { assertEquals(114, app.playback.state.value.surahId); assertEquals(1, app.playback.state.value.ayahId); app.playback.stop() }
    }

    @Test fun madinahTajwidUsesTheActualColoredPageAndKeepsTheImageUnderItsControls() {
        open(PrintedMushafEdition.MADINAH_TAJWID, "madinah_tajwid", 2, 49)
        compose.onNodeWithContentDescription("Madinah & tajwid halaman 8").assertIsDisplayed()
        val before = compose.onNodeWithTag("mushaf_page_image").getUnclippedBoundsInRoot()
        screenshot("madinah-tajwid-8-full")
        controls()
        assertEquals(before, compose.onNodeWithTag("mushaf_page_image").getUnclippedBoundsInRoot())
        val bar = compose.onNodeWithTag("mushaf_controls").getUnclippedBoundsInRoot()
        assertTrue((bar.bottom - bar.top).value <= 60f)
        screenshot("madinah-tajwid-8-controls")
    }
    @Test fun audioSettingsValidateCrossSurahRangeAndKeepTheChosenReciter() {
        val app = open(PrintedMushafEdition.INDONESIA, "indonesia")
        controls()
        compose.onNodeWithTag("mushaf_qari").performClick()
        compose.onNodeWithTag("audio_start_surah").performTextReplacement("113")
        compose.onNodeWithTag("audio_start_ayah").performTextReplacement("4")
        compose.onNodeWithTag("audio_end_surah").performTextReplacement("114")
        compose.onNodeWithTag("audio_end_ayah").performTextReplacement("7")
        compose.onNodeWithTag("mushaf_audio_start").assertIsNotEnabled()
        compose.onNodeWithTag("audio_end_ayah").performTextReplacement("6")
        compose.onNodeWithTag("mushaf_audio_start").assertIsEnabled()
        screenshot("audio-cross-surah")
        compose.onNodeWithTag("mushaf_audio_start").performScrollTo().performClick()
        compose.runOnIdle {
            val state = app.playback.state.value
            assertEquals(113, state.surahId); assertEquals(4, state.ayahId)
            assertEquals(113, state.rangeStartSurah); assertEquals(114, state.rangeEndSurah); assertEquals(6, state.rangeEnd)
            assertEquals("ar.alafasy", state.qariId)
            app.playback.stop()
        }
    }

    @Test fun hafalanUsesItsOwnPageAndRecallHidesTheEntirePage() {
        open(PrintedMushafEdition.HAFALAN_8, "hafalan_8")
        compose.onNodeWithContentDescription("Hafalan 8 blok halaman 1").assertIsDisplayed()
        screenshot("hafalan-full")
        controls()
        compose.onNodeWithContentDescription("Menu mushaf").performClick()
        compose.onNodeWithText("Uji hafalan tanpa melihat").performClick()
        compose.onNodeWithTag("mushaf_recall_hidden").assertIsDisplayed()
        compose.onNodeWithText("Tampilkan halaman").performClick()
        compose.onNodeWithTag("mushaf_recall_hidden").assertDoesNotExist()
        compose.onNodeWithContentDescription("Hafalan 8 blok halaman 1").assertIsDisplayed()
    }

    @Test fun tikrarMenuCountsTheSelectedVerseWithoutChangingPrintedEdition() {
        val app = open(PrintedMushafEdition.INDONESIA_TAJWID, "tikrar")
        TikrarProgress(app).reset(1, 1)
        controls()
        compose.onNodeWithContentDescription("Menu mushaf").performClick()
        compose.onNodeWithText("Latihan Tikrar").performClick()
        compose.onNodeWithTag("tikrar_increment_1_1").performScrollTo().performClick()
        assertEquals(1, TikrarProgress(app).count(1, 1))
        screenshot("tikrar-practice")
    }

    @Test fun waqafMenuShowsTheGuideEvenForAVerseWithoutAnInternalStop() {
        open(PrintedMushafEdition.INDONESIA_TAJWID, "waqaf")
        controls()
        compose.onNodeWithContentDescription("Menu mushaf").performClick()
        compose.onNodeWithText("Panduan waqaf & ibtida").performClick()
        compose.onNodeWithText("Panduan semua tanda").performScrollTo().performClick()
        compose.onNode(hasText("Waqaf & ibtida") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        screenshot("waqaf-guide")
    }

}
