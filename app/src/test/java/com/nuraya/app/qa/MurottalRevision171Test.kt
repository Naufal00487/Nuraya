package com.nuraya.app.qa

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.*
import com.nuraya.app.prayer.LocationPromptStore
import com.nuraya.app.profile.ProfileStore
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MurottalRevision171Test {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<NurayaApplication>()
    private val track get() = PlaybackState(title = "Ar-Rahman", surahId = 55, qariId = "ar.mahermuaiqly", qari = "Maher Al-Muaiqly", isPlaying = true, positionMs = 65_000, durationMs = 510_000)
    @Before fun setup() {
        if (runCatching { androidx.work.WorkManager.getInstance(app) }.isFailure)
            androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app, PlaybackService::class.java), Binder())
    }
    private fun await(tag: String) = compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun state(value: PlaybackState) { ReflectionHelpers.getField<MutableStateFlow<PlaybackState>>(app.playback, "mutableState").value = value }
    private fun screenshot(name: String) = compose.runOnIdle {
        val view = hostWindow(compose.activity)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("/workspace/scratch/revision-171/screenshots/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun fullScreenPlayerFromMiniPlayerKeepsAudioWhenCollapsedAndStopsExplicitly() {
        ProfileStore(app).save("Penguji", 0); LocationPromptStore(app).markIntroduced()
        app.getSharedPreferences("nuraya_banners", 0).edit().putLong("checked", System.currentTimeMillis()).commit()
        state(track)
        compose.setContent { NurayaTheme { NurayaApp(app, UserSettings()) } }
        await("home_menu")
        compose.onNodeWithContentDescription("Buka pemutar").performClick()
        await("full_player")
        val bounds = compose.onNodeWithTag("full_player").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left >= 400.dp); assertTrue(bounds.bottom - bounds.top >= 800.dp)
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Foto Maher Al-Muaiqly", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("player_toggle").assertIsDisplayed()
        compose.onNodeWithText("1:05").assertIsDisplayed()
        compose.onNodeWithText("8:30").assertIsDisplayed()
        compose.onNodeWithText("Quran.com", substring = true).assertDoesNotExist()
        screenshot("player-blue")
        compose.onNodeWithTag("player_collapse").performClick()
        compose.onNodeWithTag("full_player").assertDoesNotExist()
        assertTrue(app.playback.state.value.isPlaying)
        assertEquals(55, app.playback.state.value.surahId)
        compose.onNodeWithContentDescription("Buka pemutar").performClick()
        compose.onNodeWithContentDescription("Pilihan pemutar").performClick()
        compose.onNodeWithTag("player_stop").performClick()
        assertEquals(PlaybackState(), app.playback.state.value)
    }
    @Test @Config(qualifiers = "w320dp-h640dp")
    fun smallScreenLargeFontDarkPlayerKeepsEveryControlReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.6f)) {
                NurayaTheme(dark = true) { FullPlayer(app, "id", track.copy(qari = "Muhammad Siddiq Al-Minshawi", qariId = "ar.muhammadsiddiqalminshawimujawwad"), emptyList(), emptyList(), {}, {}) }
            }
        }
        await("full_player")
        for (tag in listOf("player_timeline", "player_previous", "player_toggle", "player_next", "player_repeat", "player_speed", "player_favorite", "player_download", "player_collection", "player_read_from_start")) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag left", bounds.left >= 0.dp); assertTrue("$tag right", bounds.right <= 320.dp)
        }
        screenshot("player-dark-large-font")
        compose.onNodeWithTag("player_speed").performScrollTo().performClick()
        compose.onNodeWithText("0.75×").assertIsDisplayed()
        compose.onNodeWithText("2.0×").assertIsDisplayed()
    }
    @Test fun readFromStartRetainsMatchingReaderVoiceAndSurah() {
        var read: Pair<Int, Int>? = null
        compose.setContent { NurayaTheme { FullPlayer(app, "id", track.copy(qariId = "ar.ahmedalajmi", qari = "Ahmed Al-Ajmi"), emptyList(), emptyList(), { runBlocking { it() } }, {}, { s, a -> read = s to a }) } }
        compose.onNodeWithTag("player_read_from_start").performScrollTo().performClick()
        assertEquals(55 to 1, read)
        assertEquals("ar.ahmedajamy", app.playback.state.value.qariId)
        assertTrue(app.playback.state.value.verseMode)
        assertEquals(78, app.playback.state.value.rangeEnd)
    }
    @Test fun errorsStayActionableAndUnknownDurationCannotBeSeeked() {
        compose.setContent { NurayaTheme { FullPlayer(app, "id", track.copy(isPlaying = false, durationMs = 0, positionMs = 0, error = "Periksa koneksi internet / Check your connection"), emptyList(), emptyList(), {}, {}) } }
        compose.onNodeWithText("Periksa koneksi internet").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Coba lagi").assertIsDisplayed()
        compose.onNodeWithTag("player_timeline").performScrollTo().assertIsNotEnabled()
        screenshot("player-error")
    }
    @Test fun suppliedPortraitAliasesCoverEveryRetainedVoiceAndOnlyTwoRequestedRecitersDisappear() {
        val catalog = QariPortraitCatalog(app)
        assertEquals(28, app.quran.qariChoices.size)
        assertEquals(28, app.playback.verseReciters.size)
        for (selection in CuratedReciters.selections) {
            val portrait = catalog.find(selection.chapterId)
            assertNotNull(selection.name, portrait)
            assertEquals(portrait, catalog.find(selection.verseId))
            assertEquals(selection.name, portrait!!.name)
        }
        for (id in listOf("ar.alijaber", "ar.abdullahalmatrood")) {
            assertFalse(app.quran.qariChoices.any { it.id == id })
            assertFalse(app.playback.verseReciters.any { it.id == id })
            assertNull(CuratedReciters.verseIdFor(id))
        }
    }
    @Test fun reciterDetailOpensPlayerWithoutSourceClutter() {
        var opened = false
        compose.setContent { NurayaTheme { MurottalScreen(app, "id", "ar.mahermuaiqly", {}, emptyList(), emptyList(), PlaybackState(), {}, { opened = true }) } }
        compose.onNodeWithText("Foto:", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("qari_about").assertDoesNotExist()
        compose.onAllNodesWithText("Dengarkan").onFirst().performClick()
        assertTrue(opened)
        assertEquals("ar.mahermuaiqly", app.playback.state.value.qariId)
    }
    @Test fun sourcesShowCorrectDesignAndPhotoCredits() {
        compose.setContent { NurayaTheme { SettingsScreen(app, UserSettings(), {}, {}) } }
        compose.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag("settings_sources"))
        compose.onNodeWithTag("settings_sources").performClick()
        compose.onNodeWithTag("design_credit").performScrollTo().assertTextContains("Naufal Irfan Ramadhan", substring = true)
        screenshot("design-attribution")
        compose.onNodeWithText("Foto qari: 8 profil", substring = true).performScrollTo().assertIsDisplayed()
    }
}
