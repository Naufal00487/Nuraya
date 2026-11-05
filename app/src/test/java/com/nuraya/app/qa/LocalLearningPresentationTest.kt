package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.nuraya.app.NurayaApplication
import com.nuraya.app.learning.LearningScreen
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalLearningPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun everydayQuestionShowsSourcedLessonAndOpensItsActualVerse() {
        var opened: Pair<Int, Int>? = null
        compose.setContent { NurayaTheme { Surface {
            LearningScreen("id", onBack = {}, onRead = { surah, ayah -> opened = surah to ayah })
        } } }
        compose.onNodeWithTag("learning_query").performScrollTo().performTextReplacement("hukum nun mati ketemu ba")
        compose.onNodeWithTag("learning_search").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching { compose.onNodeWithText("Iqlab sebelum ba").fetchSemanticsNode() }.isSuccess
        }
        compose.onNodeWithText("Iqlab sebelum ba").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("About Tajweed · Changing (iqlaab) rule of the noon saakinah and tanween").assertExists()
        val directory = File(System.getProperty("user.home"), "study-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(directory, "local-learning-iqlab.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        compose.onNodeWithTag("learning_open_2_10").performScrollTo().performClick()
        assertEquals(2 to 10, opened)
        compose.onNodeWithText("Asisten AI belum tersambung").assertDoesNotExist()
    }
}
