package com.nuraya.app.qa

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackService
import com.nuraya.app.prayer.*
import com.nuraya.app.profile.ProfileStore
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.*
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Revision163Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun openApp(dark: Boolean = false, scale: Float = 1f): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        ProfileStore(app).save("Naufal", 2)
        // These UI checks exercise bundled artwork; remote catalog refresh is unrelated.
        app.getSharedPreferences("nuraya_banners", android.content.Context.MODE_PRIVATE).edit()
            .putLong("checked", System.currentTimeMillis()).commit()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure)
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, PlaybackService::class.java), Binder())
        LocationPromptStore(app).markIntroduced()
        PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kabupaten Tangerang", -6.23, 106.62, "Asia/Jakarta")))
        runBlocking { app.settings.setDarkMode(dark) }
        compose.setContent {
            val settings by app.settings.settings.collectAsState(initial = UserSettings(darkMode = dark))
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                NurayaTheme(settings.darkMode) { NurayaApp(app, settings) }
            }
        }
        await("home_menu")
        return app
    }
    private fun await(tag: String) = compose.waitUntil(25_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = org.robolectric.shadows.ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView ?: compose.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        File("/workspace/scratch/revision-163/screenshots/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @Test fun originalLandscapeAndBlueScreensStayReadable() {
        val app = openApp()
        fun hour(target: Int) {
            val offset = Math.floorMod(target - java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).hour + 12, 24) - 12
            PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kabupaten Tangerang", -6.23, 106.62, java.time.ZoneOffset.ofHours(offset).id)))
        }
        compose.runOnIdle { hour(13) }
        await("brand_image_day")
        screenshot("home-day")
        compose.runOnIdle { hour(21) }
        await("brand_image_night")
        screenshot("home-night")
        compose.onNodeWithTag("settings_button").performClick()
        await("settings_qari")
        screenshot("settings-blue")
        back(); await("home_menu")
        compose.onNodeWithTag("nav_quran").performClick()
        await("quran_modes")
        screenshot("quran-blue")
    }

    @Test fun indonesianQueriesOpenTheirOwnSurahsAndVoiceHasNoRecitationModelStep() {
        openApp()
        compose.onNodeWithTag("home_search_learn").performScrollTo().performClick()
        await("learning_screen")
        compose.onNodeWithTag("learning_voice_search").performScrollTo().performClick()
        await("voice_verse_search")
        compose.onNodeWithTag("voice_search_language").assertTextContains("bahasa Indonesia", substring = true)
        compose.onNodeWithText("Unduh model", substring = true).assertDoesNotExist()
        screenshot("indonesian-voice-search")
        compose.onNodeWithContentDescription("Tutup pencarian suara").performScrollTo().performClick()
        compose.onNodeWithTag("learning_query").performScrollTo().performTextInput("surat Yasim")
        compose.onNodeWithTag("learning_search").performScrollTo().performClick()
        await("learning_surah_36")
        compose.onNodeWithTag("learning_approximate").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("learning_surah_36").performScrollTo()
        screenshot("yasin-suggestion")
        compose.onNodeWithTag("learning_query").performScrollTo().performTextReplacement("surah enam puluh tujuh ayat satu")
        compose.onNodeWithTag("learning_search").performScrollTo().performClick()
        await("learning_open_67_1")
        compose.onNodeWithTag("learning_open_67_1").performScrollTo().performClick()
        await("mushaf_page_image")
        // The bitmap node is composed before its asynchronous layout is placed.
        // Wait for the exact page to occupy visible space, then assert visibility.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Mushaf Indonesia halaman 562").fetchSemanticsNodes()
                .any { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        }
        compose.onNodeWithContentDescription("Mushaf Indonesia halaman 562").assertIsDisplayed()
    }

    @Test fun leavingTheActualReaderStopsReadingAudioButNotMurottal() {
        val app = openApp()
        fun reader() {
            compose.onNodeWithTag("nav_quran").performClick()
            compose.onNodeWithTag("quran_mode_standard").performClick()
            compose.onNodeWithTag("surah_1").performClick()
            await("study_ready")
        }
        reader()
        compose.runOnIdle { app.playback.playVerseRange(1, 1, 7, "ar.alafasy") }
        assertTrue(app.playback.state.value.verseMode)
        back(); await("surah_1")
        compose.runOnIdle { assertEquals(0, app.playback.state.value.surahId) }
        compose.runOnIdle { app.playback.play(app.quran.surahs[35], app.quran.qaris.first()) }
        compose.onNodeWithTag("surah_1").performClick()
        await("study_ready")
        back(); await("surah_1")
        compose.runOnIdle {
            assertEquals(36, app.playback.state.value.surahId)
            assertFalse(app.playback.state.value.verseMode)
            app.playback.stop()
        }
    }
}
