package com.nuraya.app.prayer

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.ui.NurayaTheme
import java.io.File
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Exercises real Compose selection/navigation; it does not simulate physical compass readings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrayerPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun explicitCityChoiceEnablesTheCountdownAndQiblaBearing() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = PrayerPreferenceStore(context)
        store.preferences.edit().clear().commit()
        assertNull(store.read().location)
        compose.setContent { NurayaTheme { Surface { PrayerScreen("id") } } }
        compose.onNodeWithTag("prayer_countdown").assertDoesNotExist()
        compose.onNodeWithTag("prayer_city_search").performScrollTo().performTextReplacement("Jakarta")
        compose.onNodeWithTag("prayer_screen").performScrollToNode(hasTestTag("prayer_city_Jakarta"))
        compose.onNodeWithTag("prayer_city_Jakarta").performClick()
        compose.onNodeWithTag("prayer_countdown").performScrollTo().assertIsDisplayed()
        saveImage("prayer-schedule.png")
        compose.onNodeWithTag("prayer_tab_qibla").performScrollTo().performClick()
        compose.onNodeWithTag("qibla_bearing").performScrollTo().assertIsDisplayed()
            .assertTextContains("295° dari utara sejati")
        saveImage("prayer-qibla.png")
    }

    private fun saveImage(name: String) {
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
