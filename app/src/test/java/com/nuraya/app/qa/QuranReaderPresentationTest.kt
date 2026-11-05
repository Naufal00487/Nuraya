package com.nuraya.app.qa

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import android.graphics.Bitmap
import java.io.File
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.NurayaTheme
import com.nuraya.app.ui.QuranArabicFont
import com.nuraya.app.ui.ReaderScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * Exercises the real reader composition, its corpus, and its offline font on the host.
 * These tests verify presentation/verse lookup; they do not claim device audio coverage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuranReaderPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun openReader(surah: Int, ayah: Int = 1, juz: Int = 0) {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        compose.setContent {
            NurayaTheme {
                Surface {
                    ReaderScreen(
                        app = app,
                        settings = UserSettings(),
                        surahId = surah,
                        ayahId = ayah,
                        juzId = juz,
                        entries = emptyList(),
                        // Avoid persistence side effects while still running the real reader.
                        perform = {},
                        onRead = { _, _ -> },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun baqarahShowsUnnumberedOpeningBeforeActualFirstVerseUsingQuranFont() {
        openReader(2)

        compose.onNodeWithTag("basmalah_text_2").assertTextEquals(BASMALAH)
        compose.onNodeWithTag("verse_text_2_1").assertTextEquals("الٓمٓ").assertIsDisplayed()
        compose.onNodeWithText("2:1").assertIsDisplayed()
        compose.onNode(
            hasText(BASMALAH) and hasAnyAncestor(hasTestTag("verse_2_1")),
            useUnmergedTree = true,
        ).assertDoesNotExist()

        val intro = compose.onNodeWithTag("surah_basmalah_2").fetchSemanticsNode().boundsInRoot
        val firstVerse = compose.onNodeWithTag("verse_2_1").fetchSemanticsNode().boundsInRoot
        assertTrue("The unnumbered opening must precede the numbered verse card", intro.bottom <= firstVerse.top)

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("verse_text_2_1").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue(it(layouts))
        }
        assertEquals(1, layouts.size)
        assertEquals("Arabic reader must use the bundled Quran font", QuranArabicFont, layouts.single().layoutInput.style.fontFamily)
        assertTrue("Arabic text must have measured glyphs", layouts.single().size.width > 0)
        saveReaderImage("al-baqarah-first-verse.png")
    }

    @Test fun fatihahKeepsBasmalahAsNumberedVerseOneWithoutDuplicateOpening() {
        openReader(1)

        compose.onNodeWithTag("surah_basmalah_1").assertDoesNotExist()
        compose.onNodeWithTag("verse_text_1_1").assertTextEquals(BASMALAH).assertIsDisplayed()
        compose.onNodeWithText("1:1").assertIsDisplayed()
        saveReaderImage("al-fatihah-first-verse.png")
    }

    @Test fun tawbahStartsWithItsOwnFirstVerseAndNoOpeningBasmalah() {
        openReader(9)

        compose.onNodeWithTag("surah_basmalah_9").assertDoesNotExist()
        compose.onNodeWithTag("verse_text_9_1").assertTextEquals(
            "بَرَآءَةٌ مِّنَ ٱللَّهِ وَرَسُولِهِۦٓ إِلَى ٱلَّذِينَ عَٰهَدتُّم مِّنَ ٱلْمُشْرِكِينَ",
        ).assertExists()
        compose.onNodeWithText("9:1").assertIsDisplayed()
        saveReaderImage("at-tawbah-first-verse.png")
    }

    @Test fun juzReaderSeparatesOpeningAndKeepsTheSurahVerseReference() {
        openReader(surah = 78, juz = 30)

        compose.onNodeWithTag("basmalah_text_78").assertTextEquals(BASMALAH)
        compose.onNodeWithTag("verse_text_78_1").assertTextEquals("عَمَّ يَتَسَآءَلُونَ")
        compose.onNodeWithText("78:1").assertIsDisplayed()
        compose.onNode(
            hasText(BASMALAH) and hasAnyAncestor(hasTestTag("verse_78_1")),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test fun openingAStoredVerseKeepsItsOriginalScrollPosition() {
        openReader(surah = 2, ayah = 255)
        compose.onNodeWithTag("verse_2_255").assertIsDisplayed()
        compose.onNodeWithText("2:255").assertIsDisplayed()
        compose.onNodeWithTag("basmalah_text_2").assertDoesNotExist()
    }

    private fun saveReaderImage(name: String) {
        val output = File(System.getProperty("user.home"), "reader-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            File(output, name).outputStream().use {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            image.recycle()
        }
    }

    companion object {
        private const val BASMALAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    }
}
