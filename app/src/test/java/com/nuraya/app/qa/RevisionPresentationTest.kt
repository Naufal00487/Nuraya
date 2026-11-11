package com.nuraya.app.qa

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackService
import com.nuraya.app.prayer.LocationPromptStore
import com.nuraya.app.prayer.PrayerLocation
import com.nuraya.app.prayer.PrayerPreferences
import com.nuraya.app.prayer.PrayerPreferenceStore
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.LaunchArtwork
import com.nuraya.app.ui.NurayaApp
import com.nuraya.app.ui.NurayaTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=NurayaApplication::class,qualifiers="w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RevisionPresentationTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun openApp(dark:Boolean=false){
        val app=ApplicationProvider.getApplicationContext<NurayaApplication>()
        if(runCatching{androidx.work.WorkManager.getInstance(app)}.isFailure)androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app,PlaybackService::class.java),Binder())
        LocationPromptStore(app).markIntroduced()
        PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kelapa Dua",-6.23,106.62,"Asia/Jakarta")))
        compose.setContent{NurayaTheme(dark){NurayaApp(app,UserSettings(darkMode=dark))}}
        compose.waitUntil(15_000){compose.onAllNodesWithTag("home_menu").fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun splashIsAStandaloneSceneWithoutApplicationNavigation(){
        compose.setContent{NurayaTheme{LaunchArtwork()}}
        compose.onNodeWithTag("launch_artwork").assertIsDisplayed()
        compose.onNodeWithTag("nav_home").assertDoesNotExist()
        compose.onNodeWithTag("mini_player").assertDoesNotExist()
        screenshot("launch.png")
    }
    @Test fun coversPrayerSwitchesAndSettingsGearRemainReachableAboveThreeButtonInset(){
        openApp()
        compose.runOnIdle {
            fun composeView(view:View):View? {
                if(view.javaClass.name.contains("AndroidComposeView"))return view
                if(view is ViewGroup)for(i in 0 until view.childCount)composeView(view.getChildAt(i))?.let{return it}
                return null
            }
            val view=requireNotNull(composeView(compose.activity.window.decorView))
            ViewCompat.dispatchApplyWindowInsets(view,WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(),Insets.of(0,24,0,48)).build())
        }
        compose.waitForIdle()
        val nav=compose.onNodeWithTag("nav_prayer").fetchSemanticsNode().boundsInRoot
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Bottom navigation must respect a 48 px system bar",nav.bottom<=root.bottom-48)
        compose.onNodeWithTag("nav_quran").performClick()
        compose.onNodeWithTag("artwork_quran").assertIsDisplayed()
        compose.onNodeWithTag("settings_button").assertContentDescriptionEquals("Pengaturan")
        awaitImage("quran_light"); awaitImage("covers/standard")
        screenshot("quran-light.png")
        compose.onNodeWithTag("nav_prayer").performClick()
        compose.onNodeWithTag("artwork_prayer").assertIsDisplayed()
        awaitImage("prayer_landscape")
        screenshot("prayer-header.png")
        compose.onNodeWithTag("reminder_fajr").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("prayer_screen").performScrollToNode(hasTestTag("reminder_isha"))
        compose.onNodeWithTag("reminder_isha").assertIsOff()
        screenshot("prayer-switches.png")
        compose.onNodeWithTag("nav_murottal").performClick()
        compose.onNodeWithTag("artwork_murottal").assertIsDisplayed()
        awaitImage("murottal_light")
        screenshot("murottal-light.png")
    }
    @Test fun darkArtworkUsesTheSuppliedDarkThemeImages(){
        openApp(true)
        compose.onNodeWithTag("nav_quran").performClick()
        compose.onNodeWithTag("artwork_quran").assertIsDisplayed()
        awaitImage("quran_dark"); awaitImage("covers/standard")
        screenshot("quran-dark.png")
        compose.onNodeWithTag("nav_murottal").performClick()
        compose.onNodeWithTag("artwork_murottal").assertIsDisplayed()
        awaitImage("murottal_dark")
        screenshot("murottal-dark.png")
    }
    private fun awaitImage(name:String) {
        compose.waitUntil(10_000){compose.onAllNodesWithTag("brand_image_$name",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("brand_image_$name",useUnmergedTree=true).assertIsDisplayed()
    }
    private fun screenshot(name:String){
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val file=File(System.getProperty("user.home"),"revision-150/$name").apply{parentFile!!.mkdirs()}
            file.outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
        }
    }
}
