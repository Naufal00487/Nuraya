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
class Revision162Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun openApp(dark: Boolean = false, scale: Float = 1f): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        ProfileStore(app).save("Naufal", 2)
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
        File("/workspace/scratch/revision-162/screenshots/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @Test fun homeSearchesOpenActualVerseAndHadithReaders() {
        openApp()
        screenshot("home-light")
        compose.onNodeWithTag("home_search_hadith").performScrollTo().performClick()
        await("hadith_query")
        compose.onNodeWithTag("hadith_query").performTextInput("Bukhari 1")
        compose.onNodeWithTag("hadith_search").performClick()
        await("hadith_result_bukhari_1")
        screenshot("hadith-results")
        compose.onNodeWithTag("hadith_result_bukhari_1").performScrollTo().performClick()
        await("hadith_arabic")
        screenshot("hadith-reader")
        compose.onNodeWithTag("hadith_indonesian").performScrollTo().assertTextContains("niat", substring = true, ignoreCase = true)
        back(); await("hadith_search_screen")
        back(); await("home_menu")
        compose.onNodeWithTag("home_search_learn").performScrollTo().performClick()
        await("learning_screen")
    }
    @Test fun settingsPanelsAndPrayerRemainReadableWithLargeDarkText() {
        openApp(true, 1.3f)
        screenshot("home-dark-large")
        compose.onNodeWithTag("settings_button").performClick()
        await("settings_qari")
        screenshot("settings-dark-large")
        val qari = compose.onNodeWithTag("settings_qari").getUnclippedBoundsInRoot()
        val translation = compose.onNodeWithTag("settings_translation").getUnclippedBoundsInRoot()
        assertTrue(qari.bottom <= translation.top)
        compose.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("settings_language"))
        compose.onNodeWithTag("settings_language").performClick()
        await("language_id")
        compose.onNodeWithTag("language_id").assertIsSelected()
        screenshot("settings-language")
        compose.onNodeWithContentDescription("Tutup pengaturan").performClick()
        back(); await("home_menu")
        compose.onNodeWithTag("nav_prayer").performClick()
        await("prayer_calendar")
        val date = compose.onNodeWithTag("prayer_hijri_date").getUnclippedBoundsInRoot()
        val strip = compose.onNodeWithTag("prayer_times_strip").getUnclippedBoundsInRoot()
        assertTrue(date.bottom <= strip.top)
        screenshot("prayer-dark-large")
        compose.onNodeWithTag("prayer_screen").performScrollToNode(hasTestTag("reminder_isha"))
        compose.onNodeWithTag("reminder_isha").assertIsDisplayed()
    }
    @Test fun selectingMurottalQariPersistsTheMatchingQuranVoice() {
        val app = openApp()
        compose.onNodeWithTag("nav_murottal").performClick()
        compose.onNodeWithTag("qari_ar.abdulbasitmurattal").performScrollTo().performClick()
        compose.waitUntil(15_000) { runBlocking { app.settings.settings.first().readerQariId } == "ar.abdulsamad" }
        compose.onNodeWithTag("settings_button").performClick()
        compose.onNodeWithTag("settings_qari").assertTextContains("Abdul Basit Abdus Samad", substring = true)
        compose.onNodeWithTag("settings_qari").performClick()
        await("settings_panel_qari")
        screenshot("qari-picker")
        compose.onNodeWithContentDescription("Tutup pengaturan").performClick()
        back()
        compose.onNodeWithTag("nav_quran").performClick()
        compose.onNodeWithTag("quran_mode_standard").performClick()
        compose.onNodeWithTag("surah_1").performClick()
        await("study_ready")
        compose.onNodeWithTag("study_open_menu").performScrollTo().performClick()
        compose.onNodeWithTag("study_qari_choose").performScrollTo().performClick()
        compose.onNodeWithTag("study_qari_search").performScrollTo().performTextInput("Matrood")
        compose.onNodeWithTag("study_qari_ar.abdullahalmatrood").performScrollTo().performClick()
        compose.waitUntil(15_000) { runBlocking { app.settings.settings.first().readerQariId } == "ar.abdullahalmatrood" }
        compose.onNodeWithTag("study_qari_search").assertDoesNotExist()
        compose.onNodeWithTag("study_qari_choose").assertTextContains("Abdullah Al-Matrood", substring = true)
    }
}
