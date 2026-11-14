package com.nuraya.app.qa

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackService
import com.nuraya.app.prayer.*
import com.nuraya.app.profile.ProfileAvatar
import com.nuraya.app.profile.ProfileStore
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Revision161Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun openApp() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        ProfileStore(app).save("Naufal", 2)
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure)
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, PlaybackService::class.java), Binder())
        LocationPromptStore(app).markIntroduced()
        PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kelapa Dua", -6.23, 106.62, "Asia/Jakarta")))
        compose.setContent { NurayaTheme { NurayaApp(app, UserSettings()) } }
        await("home_menu")
    }
    private fun await(tag: String) = compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = org.robolectric.shadows.ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView ?: compose.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        File(System.getProperty("user.home"), "ui-161/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun suppliedBannersDisplayAtOriginalAspectAndOpenTheirDestinations() {
        openApp()
        compose.onNodeWithTag("home_menu").performScrollToNode(hasTestTag("home_banners"))
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Bacalah, dengarkan, hafalkan · AI belajar", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        val bounds = compose.onNodeWithTag("home_banner_nuraya-learn").getUnclippedBoundsInRoot()
        assertEquals(5300f / 1331f, (bounds.right - bounds.left) / (bounds.bottom - bounds.top), .03f)
        screenshot("home-banner-learn")
        compose.onNodeWithTag("home_banner_nuraya-learn").performClick()
        await("learning_screen")
        compose.onNodeWithTag("search_semantic").assertIsSelected()
        back(); await("home_menu")
        compose.onNodeWithTag("home_menu").performScrollToNode(hasTestTag("home_banners"))
        compose.onNodeWithTag("home_banners").performTouchInput { swipeLeft() }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Jelajahi fitur Nuraya", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home_banner_nuraya-explore").assertIsDisplayed()
        screenshot("home-banner-explore")
        compose.onNodeWithTag("home_banner_nuraya-explore").performClick()
        await("quran_modes")
    }

    @Test fun quranSearchOpensLocalVoiceSheetAndReturnsToQuranWithoutRecording() {
        openApp()
        compose.onNodeWithTag("nav_quran").performClick()
        compose.onNodeWithTag("quran_modes").performScrollToNode(hasTestTag("quran_smart_search"))
        compose.onNodeWithTag("quran_smart_search").performClick()
        await("learning_screen")
        compose.onNodeWithTag("learning_voice_search").performScrollTo().performClick()
        await("voice_verse_search")
        compose.onNodeWithTag("voice_search_record").performScrollTo().assertIsNotEnabled()
        screenshot("voice-search")
        compose.onNodeWithContentDescription("Tutup pencarian suara").performScrollTo().performClick()
        compose.onNodeWithTag("voice_verse_search").assertDoesNotExist()
        compose.onNodeWithTag("learning_query").performScrollTo().performTextInput("Al-Fatihah ayat 3")
        compose.onNodeWithTag("learning_search").performScrollTo().performClick()
        await("learning_open_1_3")
        compose.onNodeWithTag("learning_open_1_3").performScrollTo().performClick()
        await("mushaf_page_image")
        compose.onNodeWithContentDescription("Mushaf Indonesia halaman 1").assertIsDisplayed()
        back(); await("learning_screen")
        back(); await("quran_modes")
        compose.onNodeWithTag("quran_modes").performScrollToNode(hasTestTag("quran_mode_madinah_tajwid"))
        await("brand_image_covers/madinah_tajwid")
        screenshot("madinah-covers")
        compose.onNodeWithText("Mushaf Bahriyah/Pojok").assertDoesNotExist()
    }

    @Test fun everyNewAvatarIsAvailableAndSavingKeepsTheSameLocalProfile() {
        openApp()
        compose.onNodeWithTag("home_profile").performClick()
        await("profile_screen")
        compose.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("profile_avatar_4"))
        for (index in 0..4) compose.onNodeWithTag("profile_avatar_$index").assertExists()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Avatar 5", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("profile_avatar_4").performClick()
        screenshot("profile-avatars")
        compose.onNodeWithTag("profile_save").performScrollTo().performClick()
        val saved = ProfileStore(ApplicationProvider.getApplicationContext()).read()
        assertEquals("Naufal", saved.nickname); assertEquals(4, saved.avatar)
    }

    @Test @Config(sdk = [29])
    fun systemBarsStayTransparentAndIconsFollowTheCurrentScreen() {
        var screen by mutableStateOf("quran")
        var color: Int? = null
        compose.setContent { NurayaTheme(true) { ScreenColors(screen, dark = true) {
            color = MaterialTheme.colorScheme.background.toArgb()
            Text("Screen")
        } } }
        compose.runOnIdle {
            val window = compose.activity.window
            assertEquals(0, window.statusBarColor); assertEquals(0, window.navigationBarColor)
            assertFalse(window.isStatusBarContrastEnforced); assertFalse(window.isNavigationBarContrastEnforced)
            assertFalse(WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars)
            screen = "mushaf"
        }
        compose.runOnIdle {
            val window = compose.activity.window
            assertEquals(MushafColors.background.toArgb(), color)
            assertTrue(WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars)
            assertEquals(1f, MushafColors.surfaceContainer.alpha)
            screen = "qibla"
        }
        compose.runOnIdle {
            val window = compose.activity.window
            assertEquals(0, window.navigationBarColor)
            assertFalse(WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars)
        }
    }
}
