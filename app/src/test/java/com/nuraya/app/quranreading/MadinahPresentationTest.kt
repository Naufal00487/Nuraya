package com.nuraya.app.quranreading

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.MadinahScreen
import com.nuraya.app.ui.NurayaTheme
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MadinahPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun ornamentalIndonesiaPagesNavigateToTheCorrectVerseAndIdentifyTheirEdition() {
        var opened: Pair<Int, Int>? = null
        compose.setContent { NurayaTheme { Surface {
            MadinahScreen("id", 2, 1, true, edition = MushafEdition.INDONESIA) { surah, ayah -> opened = surah to ayah }
        } } }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("indonesia_screen").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mushaf Indonesia halaman 2").assertIsDisplayed()
        saveScreenshot("indonesia-page-two.png")
        showControls()
        compose.onNodeWithTag("mushaf_next").performClick()
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithContentDescription("Mushaf Indonesia halaman 3").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithText("Baca dengan tajwid berwarna").performClick()
        assertEquals(2 to 6, opened)
        saveScreenshot("indonesia-page-three.png")
        showControls()
        compose.onNodeWithContentDescription("Menu mushaf").performClick()
        compose.onNodeWithText("Info tajwid & sumber").performClick()
        compose.onNodeWithText("Ini bukan edisi terjemah per kata Islampedia", substring = true).assertExists()
    }

    @Test fun genuineBundledPageLoadsAndNavigationKeepsVerseCompanionInSync() {
        var opened: Pair<Int, Int>? = null
        compose.setContent { NurayaTheme { Surface {
            MadinahScreen("id", 2, 1, true) { surah, ayah -> opened = surah to ayah }
        } } }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        saveScreenshot("madinah-initial.png")
        showControls()
        compose.onNodeWithTag("mushaf_page_number").assertTextEquals("Halaman 2 / 604")
        compose.onNodeWithTag("mushaf_page_image").assertIsDisplayed()
        val initialWidth = compose.onNodeWithTag("mushaf_page_image").getUnclippedBoundsInRoot().let { it.right.value - it.left.value }
        compose.onNodeWithText("+ Besarkan").performClick()
        assertTrue(compose.onNodeWithTag("mushaf_page_image").getUnclippedBoundsInRoot().let { it.right.value - it.left.value } > initialWidth)
        compose.onNodeWithText("− Kecilkan").assertIsEnabled().performClick()
        compose.onNodeWithText("Baca dengan tajwid berwarna").performClick()
        assertEquals(2 to 1, opened)
        val screenshot = File(System.getProperty("user.home"), "ui-130/madinah-page.png").apply { parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            screenshot.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
        showControls()
        compose.onNodeWithTag("mushaf_previous").performClick()
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        saveScreenshot("madinah-page-one.png")
        compose.onNodeWithTag("mushaf_page_image").assertIsDisplayed()
        compose.onNodeWithTag("mushaf_page_number").assertTextEquals("Halaman 1 / 604")
        compose.onNodeWithTag("mushaf_previous").assertIsNotEnabled()
        compose.onNodeWithText("Baca dengan tajwid berwarna").performClick()
        assertEquals(1 to 1, opened)
    }

    @Test fun largeTextKeepsThePageVisibleAndCompanionButtonReachable() {
        var opened = false
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                NurayaTheme { Surface { MadinahScreen("id", 1, 1, true) { _, _ -> opened = true } } }
            }
        }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_page_image").assertIsDisplayed()
        saveScreenshot("madinah-large-text.png")
        showControls()
        compose.onNodeWithTag("mushaf_page_image").assertIsDisplayed()
        compose.onNodeWithText("Baca dengan tajwid berwarna").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test fun pageTapHidesControlsAndDoubleTapRequestsLandscape() {
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id") { _, _ -> } } } }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_controls").assertDoesNotExist()
        showControls()
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onNodeWithTag("mushaf_previous").assertDoesNotExist()
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { doubleClick(center) }
        compose.runOnIdle { assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, compose.activity.requestedOrientation) }
        saveScreenshot("madinah-controls.png")
    }

    @Test fun jumpRejectsInvalidPagesAndOpensAnExactBundledPage() {
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id") { _, _ -> } } } }
        showControls()
        compose.onNodeWithContentDescription("Pilih halaman, surah atau juz").performClick()
        compose.onNodeWithTag("mushaf_jump_number").performTextReplacement("605")
        compose.onNodeWithText("Buka").assertIsNotEnabled()
        compose.onNodeWithTag("mushaf_jump_number").performTextReplacement("604")
        compose.onNodeWithText("Buka").performClick()
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_page_number").assertTextEquals("Halaman 604 / 604")
        compose.onNodeWithTag("mushaf_next").assertIsNotEnabled()
    }

    @Test fun companionTranslationAndSearchableQariPickerStayInsideMushaf() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.nuraya.app.media.PlaybackService::class.java), android.os.Binder())
        app.quran.surahs
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id", app = app) { _, _ -> } } } }
        showControls()
        compose.onNodeWithTag("mushaf_translation").performClick()
        compose.onNodeWithText("Kementerian Agama RI · QuranEnc v1.0.1").assertExists()
        saveScreenshot("madinah-translation.png")
        compose.onNodeWithText("Ayat berikutnya").performClick()
        compose.onNodeWithText("Al-Faatiha · 2").assertExists()
        compose.onNodeWithContentDescription("Tutup panel arti").performClick()
        showControls()
        compose.onNodeWithText("Qari & audio").performClick()
        compose.onNodeWithText("Cari nama qari").performTextInput("Basfar")
        compose.onNodeWithText("Abdullah Basfar").assertIsDisplayed()
        compose.onNodeWithText("Alafasy").assertDoesNotExist()
        saveScreenshot("madinah-qari.png")
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeKeepsThePageReadableWithScrollableHeight() {
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id") { _, _ -> } } } }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithTag("mushaf_page_image").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_page_image").assertIsDisplayed()
        val image = compose.onNodeWithTag("mushaf_page_image").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("mushaf_page_viewport").getUnclippedBoundsInRoot()
        assertTrue(image.bottom.value - image.top.value > viewport.bottom.value - viewport.top.value)
        showControls()
        compose.onNodeWithTag("mushaf_next").assertIsDisplayed()
        saveScreenshot("madinah-landscape.png")
    }

    @Test fun bookFillsScreenAndOverlayNeverShrinksItWhileSwipesPreservePageOrder() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.nuraya.app.media.PlaybackService::class.java), android.os.Binder())
        app.quran.surahs
        compose.setContent { NurayaTheme { Surface { MadinahScreen("id", app = app, edition = MushafEdition.INDONESIA, readingModeId = "indonesia_tajwid") { _, _ -> } } } }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithContentDescription("Mushaf Indonesia halaman 1").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_controls").assertDoesNotExist()
        compose.onNodeWithTag("back_button").assertDoesNotExist()
        val fullPage = compose.onNodeWithTag("mushaf_page_viewport").getUnclippedBoundsInRoot()
        assertEquals(compose.onNodeWithTag("indonesia_screen").getUnclippedBoundsInRoot(), fullPage)
        saveScreenshot("indonesia-fullscreen.png")
        showControls()
        assertEquals(fullPage, compose.onNodeWithTag("mushaf_page_viewport").getUnclippedBoundsInRoot())
        compose.onNodeWithTag("mushaf_translation").assertIsDisplayed()
        saveScreenshot("indonesia-overlay.png")
        compose.onNodeWithContentDescription("Tutup menu baca").performClick()
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Mushaf Indonesia halaman 1").assertIsDisplayed()
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { swipeRight() }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithContentDescription("Mushaf Indonesia halaman 2").fetchSemanticsNode() }.isSuccess }
        compose.onNodeWithTag("mushaf_controls").assertDoesNotExist()
        saveScreenshot("indonesia-fullscreen-page-two.png")
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { swipeLeft() }
        compose.waitUntil(30_000) { runCatching { compose.onNodeWithContentDescription("Mushaf Indonesia halaman 1").fetchSemanticsNode() }.isSuccess }
    }

    private fun showControls() {
        if (compose.onAllNodesWithTag("mushaf_controls").fetchSemanticsNodes().isNotEmpty()) return
        compose.onNodeWithTag("mushaf_page_viewport").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(350)
        compose.waitForIdle()
        compose.onNodeWithTag("mushaf_controls").assertExists()
    }

    private fun saveScreenshot(name: String) {
        val screenshot = File(System.getProperty("user.home"), "ui-130/$name").apply { parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            screenshot.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
    }
}
