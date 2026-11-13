package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.profile.ProfileStore
import com.nuraya.app.prayer.*
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OnboardingLayout153Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun app(): NurayaApplication {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure) androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        org.robolectric.Shadows.shadowOf(app).setComponentNameAndServiceForBindService(android.content.ComponentName(app, com.nuraya.app.media.PlaybackService::class.java), android.os.Binder())
        LocationPromptStore(app).markIntroduced()
        return app
    }
    @Test fun freshInstallRequiresANameBeforeHomeAndKeepsTheProfileLocally() {
        val app = app(); val store = ProfileStore(app)
        store.preferences.edit().clear().commit()
        assertFalse(store.isComplete())
        compose.setContent { NurayaTheme { NurayaApp(app, UserSettings()) } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("profile_onboarding").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home_menu").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_continue").assertIsNotEnabled()
        compose.onNodeWithTag("onboarding_name").performTextInput("   ")
        compose.onNodeWithTag("onboarding_continue").assertIsNotEnabled()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("profile_onboarding").assertExists()
        screenshot("profile-required.png")
        compose.onNodeWithTag("onboarding_name").performTextReplacement("  Naufal  ")
        compose.onNodeWithTag("onboarding_continue").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_menu").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(ProfileStore(app).isComplete())
        assertEquals("Naufal", ProfileStore(app).read().nickname)
    }
    @Test fun settingsSectionsDoNotOverlapAtLargeTextSize() {
        val app = app()
        compose.setContent { val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                NurayaTheme { Surface { SettingsScreen(app, UserSettings(), {}, {}) } }
            }
        }
        val title = compose.onNodeWithText("Atur kenyamanan membaca").getUnclippedBoundsInRoot()
        val subtitle = compose.onNodeWithText("Profil pribadi · tersimpan di HP ini").getUnclippedBoundsInRoot()
        assertTrue(title.bottom <= subtitle.top)
        val language = compose.onNodeWithText("Bahasa antarmuka").getUnclippedBoundsInRoot()
        val choice = compose.onNodeWithTag("language_id").getUnclippedBoundsInRoot()
        assertTrue(language.bottom <= choice.top)
        screenshot("settings-large-text.png")
        compose.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("tajwid_colors_switch"))
        compose.onNodeWithTag("tajwid_colors_switch").assertIsDisplayed()
        screenshot("settings-toggles-large-text.png")
    }
    @Test fun prayerArtworkAndCalendarAreSeparateAndAllTimesStayReadable() {
        val app = app()
        PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kelapa Dua", -6.23, 106.62, "Asia/Jakarta")))
        compose.setContent { val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.5f)) { NurayaTheme { Surface { PrayerScreen("id") } } }
        }
        val artwork = compose.onNodeWithTag("artwork_prayer").getUnclippedBoundsInRoot()
        val calendar = compose.onNodeWithTag("prayer_calendar").getUnclippedBoundsInRoot()
        assertTrue(artwork.bottom <= calendar.top)
        screenshot("prayer-large-text.png")
        compose.onNodeWithTag("prayer_screen").performScrollToNode(hasTestTag("reminder_fajr"))
        compose.onNodeWithTag("reminder_fajr").assertIsDisplayed()
        screenshot("prayer-times-large-text.png")
        compose.onNodeWithTag("prayer_screen").performScrollToNode(hasTestTag("reminder_isha"))
        compose.onNodeWithTag("reminder_isha").assertIsDisplayed()
    }
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File(System.getProperty("user.home"), "revision-153/$name").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
}
