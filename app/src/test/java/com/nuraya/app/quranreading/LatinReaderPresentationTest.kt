package com.nuraya.app.quranreading

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.NurayaTheme
import org.junit.Assert.assertNull
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
class LatinReaderPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun latinPanelLoadsOfflineSourceWhenCanonicalVerseHasNoLatinField() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val verse = requireNotNull(app.quran.verse(2, 1))
        assertNull(verse.transliteration)
        compose.setContent { NurayaTheme { Surface {
            LatinTajwidPanel(verse, null, "id", false)
        } } }
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("latin_2_1").fetchSemanticsNode() }.isSuccess
        }
        compose.onNodeWithTag("latin_2_1").assertTextEquals("Alif lām mīm.").assertIsDisplayed()
    }
}
