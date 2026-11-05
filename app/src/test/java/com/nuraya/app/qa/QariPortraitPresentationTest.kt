package com.nuraya.app.qa

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.ui.MurottalScreen
import com.nuraya.app.ui.NurayaTheme
import java.io.File
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
class QariPortraitPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun portraitIdentifiesTheQariAndSelectingItKeepsTheCorrectAudioEdition() {
        val app = ApplicationProvider.getApplicationContext<NurayaApplication>()
        var selected: String? = null
        compose.setContent { NurayaTheme { Surface {
            MurottalScreen(app, "id", "", { selected = it }, emptyList(), emptyList(), PlaybackState(), {})
        } } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Foto Mishary Rashid Alafasy. Sumber Quran.com.", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("qari_ar.alafasy").performClick()
        assertEquals("ar.alafasy", selected)
        compose.runOnIdle {
            val directory = File(System.getProperty("user.home"), "ui-130").apply { mkdirs() }
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            File(directory, "qari-portraits.png").outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
    }
}
