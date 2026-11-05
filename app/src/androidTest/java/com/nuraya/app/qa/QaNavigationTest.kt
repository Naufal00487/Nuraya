package com.nuraya.app.qa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nuraya.app.MainActivity
import com.nuraya.app.NurayaApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device tests exercise the APK UI; building this target alone is not a test pass. */
@RunWith(AndroidJUnit4::class)
class QaNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun resetLanguage() {
        val app = compose.activity.application as NurayaApplication
        runBlocking { app.settings.setLanguage("id") }
        awaitTag("nav_home")
        if (compose.onAllNodesWithTag("location_setup_dialog").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("device_location_later").performClick()
        }
    }

    @After fun restoreLanguage() {
        val app = compose.activity.application as NurayaApplication
        runBlocking { app.settings.setLanguage("id") }
    }

    @Test fun coldLaunchAndAllMainDestinationsRemainUsable() {
        listOf("nav_home", "nav_quran", "nav_murottal", "nav_prayer", "nav_home")
            .forEach { tag ->
                compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
                compose.waitForIdle()
            }
        compose.onNodeWithTag("settings_button").assertIsDisplayed().performClick()
        awaitTag("language_en")
    }

    @Test fun offlineBundledQuranOpensReader() {
        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_mode_standard")
        compose.onNodeWithTag("quran_mode_standard").performClick()
        awaitTag("surah_1")
        compose.onNodeWithTag("surah_1").performClick()
        awaitTag("reader_screen")
        compose.onNodeWithTag("reader_screen").assertIsDisplayed()
    }

    @Test fun baqarahOpeningAndJumpKeepCorrectVerseNumbers() {
        compose.onNodeWithTag("nav_quran").performClick()
        awaitTag("quran_mode_standard")
        compose.onNodeWithTag("quran_mode_standard").performClick()
        awaitTag("surah_2")
        compose.onNodeWithTag("surah_2").performClick()
        awaitTag("verse_text_2_1")
        compose.onNodeWithTag("verse_text_2_1").assertTextEquals("الٓمٓ")
        compose.onNodeWithTag("surah_basmalah_2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Lompat ke ayat").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("255")
        compose.onNodeWithText("Buka").performClick()
        awaitTag("verse_2_255")
        compose.onNodeWithText("2:255").assertIsDisplayed()
    }

    @Test fun languageCanChangeAndSurviveActivityRecreation() {
        compose.onNodeWithTag("settings_button").performClick()
        awaitTag("language_en")
        compose.onNodeWithTag("language_en").performClick()
        compose.waitForIdle()
        val app = compose.activity.application as NurayaApplication
        compose.waitUntil(timeoutMillis = 10_000) {
            runBlocking { app.settings.settings.first().language == "en" }
        }
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        awaitTag("language_id")
        assertEquals("en", runBlocking { app.settings.settings.first().language })
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithTag("language_id").performClick()
        compose.waitForIdle()
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
