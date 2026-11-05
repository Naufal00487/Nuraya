package com.nuraya.app.qa

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserEntry
import com.nuraya.app.ui.LibraryScreen
import com.nuraya.app.ui.NurayaTheme
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
class LibraryVerseHistoryPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun historyTapUsesTheStoredVerseAndQariInsteadOfTreatingTheAyahAsASurah() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val valid = UserEntry("verse_audio_history:ar.husary:2:255", "verse_audio_history", "ar.husary:2:255", "Al-Baqarah · 255", extra = "ar.husary")
        val invalid = valid.copy(id = "verse_audio_history:ar.husary:255:2", key = "ar.husary:255:2", title = "Invalid older reference")
        var played: Triple<Int, Int, String>? = null
        var read: Pair<Int, Int>? = null
        compose.setContent { NurayaTheme { Surface {
            LibraryScreen(app, "id", listOf(valid, invalid), emptyList(), perform = {}, onRead = { s, a -> read = s to a },
                onPlayVerseHistory = { s, a, q -> played = Triple(s, a, q) })
        } } }
        compose.onNodeWithTag("library_filter_history").performClick()
        compose.onNodeWithTag("library_entry_${invalid.id}").performScrollTo().performClick()
        compose.onNodeWithTag("library_entry_unavailable").assertExists()
        assertNull(played)
        compose.onNodeWithTag("library_entry_${valid.id}").performScrollTo().performClick()
        assertEquals(Triple(2, 255, "ar.husary"), played)
        assertNull(read)
        compose.onNodeWithTag("library_entry_unavailable").assertDoesNotExist()
    }
}
