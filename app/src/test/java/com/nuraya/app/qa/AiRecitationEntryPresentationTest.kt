package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.recitation.RecitationPracticeScreen
import com.nuraya.app.ui.HomeScreen
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

/** Checks discoverability and the first-use boundary; does not claim native/device inference. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = NurayaApplication::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AiRecitationEntryPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun aiEntryIsVisibleWithoutScrollingAndFirstUseDoesNotActivateMicrophone() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        val preferences = app.getSharedPreferences("recitation_practice", android.content.Context.MODE_PRIVATE)
        val previousAttempts = preferences.getInt("completed_attempts", 0)
        compose.setContent { NurayaTheme { Surface {
            var destination by remember { mutableStateOf("home") }
            if (destination == "home") HomeScreen(app, "id", emptyList(), onRead = { _, _ -> },
                onNavigate = { destination = it })
            else RecitationPracticeScreen(app, "id", onRead = { _, _ -> })
        } } }
        // This regression directly covers the user's "where is the AI?" report.
        compose.onNodeWithText("AI bacaan Al-Qur’an").assertIsDisplayed().performClick()
        compose.onNodeWithText("AI Quran di perangkat").assertIsDisplayed()
        compose.onNodeWithTag("quran_ai_model").performScrollTo().assertIsDisplayed()
        saveHostImage("ai-recitation-first-use.png")
        compose.onNodeWithTag("recitation_microphone").performScrollTo().assertIsNotEnabled()
        // Capture diagnostics remain available before downloading a model/native engine support.
        compose.onNodeWithTag("recitation_check_microphone").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Selesai membaca").assertDoesNotExist()
        assertEquals(previousAttempts, preferences.getInt("completed_attempts", 0))
    }

    private fun saveHostImage(name: String) {
        val directory = File(System.getProperty("user.home"), "study-ui").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(directory, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
}
