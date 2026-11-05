package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaApp
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

/** App navigation with real source data. No audio, sensor or microphone hardware is claimed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SeniorNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun openApp(fontScale: Float = 1f, firstLaunch: Boolean = false): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        // Robolectric does not run AndroidX Startup's WorkManager initializer.
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) {
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        }
        // A real component name avoids Robolectric's null service binding. The
        // inert binder is intentional: this test does not exercise audio output.
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(
            android.content.ComponentName(app, com.nuraya.app.media.PlaybackService::class.java),
            android.os.Binder(),
        )
        app.getSharedPreferences("nuraya_prayer", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("nuraya_location_prompt", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        if (!firstLaunch) com.nuraya.app.prayer.LocationPromptStore(app).markIntroduced()
        // Load the same corpus used by the app before its asynchronous loading boundary.
        app.quran.surahs
        app.quran.verses(1)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                NurayaTheme { NurayaApp(app, UserSettings()) }
            }
        }
        awaitTag("home_menu")
        return app
    }

    @Test fun firstLaunchRequestsLocationWithoutGuessingCoordinatesAndStillAllowsReading() {
        val app = openApp(firstLaunch = true)
        awaitTag("location_setup_dialog")
        compose.onNodeWithTag("location_setup_dialog").assertIsDisplayed()
        compose.onNodeWithTag("device_location_request").assertIsDisplayed()
        assertEquals(null, com.nuraya.app.prayer.PrayerPreferenceStore(app).read().location)
        saveHostImage("first-launch-location.png", dialog = true)
        compose.onNodeWithTag("device_location_later").performClick()
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_modes")
        compose.onNodeWithTag("quran_mode_standard").performClick()
        awaitTag("surah_1")
        compose.onNodeWithTag("surah_1").performClick()
        awaitTag("verse_text_1_1")
        compose.onNodeWithTag("verse_text_1_1").assertIsDisplayed()
        assertTrue(!com.nuraya.app.prayer.LocationPromptStore(app).shouldIntroduce(false))
    }

    @Test fun fourMainDestinationsKeepChosenReadingModeAndRemoveVideo() {
        openApp()
        listOf("home", "quran", "murottal", "prayer").forEach { compose.onNodeWithTag("nav_$it").assertIsDisplayed() }
        compose.onNodeWithTag("nav_video").assertDoesNotExist()
        compose.onNodeWithText("Video").assertDoesNotExist()
        compose.onNodeWithTag("home_recitation").assertIsDisplayed()
        saveHostImage("senior-home.png")

        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_modes")
        saveHostImage("reading-modes.png")
        compose.onNodeWithTag("quran_modes").performScrollToNode(hasTestTag("quran_mode_indonesia"))
        compose.onNodeWithTag("quran_mode_indonesia").performClick()
        awaitTag("surah_1")
        compose.onNodeWithTag("surah_1").performClick()
        awaitTag("reader_mode")
        compose.onNodeWithTag("reader_mode").assertTextContains("Al-Qur’an Indonesia")
        compose.onNodeWithTag("back_button").performClick()
        awaitTag("quran_search")
        compose.onNodeWithTag("nav_prayer").performClick()
        awaitTag("prayer_screen")
        compose.onNodeWithTag("prayer_tab_location").assertIsDisplayed()
        compose.onNodeWithTag("prayer_countdown").assertDoesNotExist()
        saveHostImage("prayer-choose-location.png")
        compose.onNodeWithTag("nav_video").assertDoesNotExist()
    }

    @Test fun largerTextStillLetsTheUserReachAiWithoutStartingRecording() {
        val app = openApp(fontScale = 1.3f)
        val preferences = app.getSharedPreferences("recitation_practice", android.content.Context.MODE_PRIVATE)
        val attempts = preferences.getInt("completed_attempts", 0)
        compose.onNodeWithTag("home_menu").performScrollToNode(hasTestTag("home_recitation"))
        compose.onNodeWithTag("home_recitation").assertIsDisplayed().performClick()
        awaitTag("quran_ai_model")
        compose.onNodeWithTag("quran_ai_model").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("recitation_practice_screen").performScrollToNode(hasTestTag("recitation_microphone"))
        compose.onNodeWithTag("recitation_microphone").assertIsNotEnabled()
        compose.onNodeWithText("Selesai membaca").assertDoesNotExist()
        assertEquals(attempts, preferences.getInt("completed_attempts", 0))
        compose.onNodeWithTag("back_button").performClick()
        awaitTag("home_menu")
        compose.onNodeWithTag("nav_prayer").assertIsDisplayed()
        compose.onNodeWithTag("nav_video").assertDoesNotExist()
    }

    @Test fun madinahOpensActualPagesAndBahriyahClearlyRequiresItsOwnPdf() {
        openApp()
        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_modes")
        compose.onNodeWithTag("quran_modes").performScrollToNode(hasTestTag("quran_mode_madinah_tajwid"))
        compose.onNodeWithTag("quran_mode_madinah_tajwid").performClick()
        awaitTag("surah_1")
        compose.onNodeWithTag("surah_1").performClick()
        awaitTag("mushaf_page_image")
        compose.onNodeWithTag("mushaf_page_number").assertTextContains("Halaman 1 / 604")
        compose.onNodeWithTag("mushaf_next").performClick()
        awaitTag("mushaf_page_image")
        compose.onNodeWithTag("mushaf_page_number").assertTextContains("Halaman 2 / 604")
        compose.onNodeWithText("Baca dengan tajwid berwarna").performClick()
        awaitTag("reader_mode")
        compose.onNodeWithTag("reader_mode").assertTextContains("Madinah & tajwid")
        compose.onNodeWithTag("back_button").performClick()
        awaitTag("mushaf_page_image")
        compose.onNodeWithTag("mushaf_page_number").assertTextContains("Halaman 2 / 604")
        compose.onNodeWithTag("back_button").performClick()
        awaitTag("quran_search")
        compose.onNodeWithTag("change_reading_mode").performClick()
        awaitTag("quran_modes")
        compose.onNodeWithTag("quran_modes").performScrollToNode(hasTestTag("quran_mode_bahriyah"))
        compose.onNodeWithTag("quran_mode_bahriyah").performClick()
        awaitTag("bahriyah_pdf_screen")
        compose.onNodeWithTag("madinah_screen").assertDoesNotExist()
        saveHostImage("bahriyah-import.png")
    }

    @Test fun reopeningAShortSurahDoesNotRestoreAnotherSurahsReadingRange() {
        openApp()
        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_modes")
        compose.onNodeWithTag("quran_mode_standard").performClick()
        awaitTag("quran_search")
        compose.onNodeWithTag("quran_search").performTextInput("2:255")
        awaitTag("search_verse_2_255")
        compose.onNodeWithTag("search_verse_2_255").performClick()
        awaitTag("reader_screen")
        compose.onNodeWithTag("back_button").performClick()
        awaitTag("quran_search")
        compose.onNodeWithTag("quran_search").performTextClearance()
        awaitTag("surah_1")
        compose.onNodeWithTag("surah_1").performClick()
        awaitTag("verse_text_1_1")
        compose.onNodeWithTag("verse_text_1_1").assertIsDisplayed()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 30_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun saveHostImage(name: String, dialog: Boolean = false) {
        val folder = File(System.getProperty("user.home"), "ui-131").apply { mkdirs() }
        compose.runOnIdle {
            // Compose dialogs have their own Window: drawing the Activity alone
            // would misleadingly show only the home screen behind the prompt.
            val view = if (dialog) {
                val activeDialog = requireNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
                assertTrue("The captured dialog must still be showing", activeDialog.isShowing)
                requireNotNull(activeDialog.window).decorView
            } else compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(folder, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
}
