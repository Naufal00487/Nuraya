package com.nuraya.app.qa

import android.app.NotificationManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.NurayaApplication
import com.nuraya.app.banners.BannerCarousel
import com.nuraya.app.media.PlaybackService
import com.nuraya.app.prayer.*
import com.nuraya.app.profile.ProfileStore
import com.nuraya.app.reminders.*
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.*
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=NurayaApplication::class,qualifiers="w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Revision164Test {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<NurayaApplication>()
    private fun await(tag:String)=compose.waitUntil(30_000){compose.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    private fun screenshot(name:String)=compose.runOnIdle {
        val view=hostWindow(compose.activity)
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
        File("/workspace/scratch/revision-164/screenshots/$name.png").apply{parentFile!!.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun openApp(savedMode:String?=null) {
        ProfileStore(app).save("Naufal",2);LocationPromptStore(app).markIntroduced()
        PrayerPreferenceStore(app).save(PrayerPreferences(PrayerLocation("Kabupaten Tangerang",-6.23,106.62,"Asia/Jakarta")))
        app.getSharedPreferences("nuraya_banners",0).edit().putLong("checked",System.currentTimeMillis()).commit()
        if(runCatching{androidx.work.WorkManager.getInstance(app)}.isFailure)androidx.work.WorkManager.initialize(app,androidx.work.Configuration.Builder().build())
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app,PlaybackService::class.java),Binder())
        runBlocking{app.settings.setDarkMode(false);if(savedMode!=null)app.library.save("read_history","112:1","Al-Ikhlas",extra="604:$savedMode")}
        compose.setContent{NurayaTheme(false){NurayaApp(app,UserSettings())}}
        await("home_menu")
    }
    @Test fun reminderSettingsChangeMinutesAndQuranSwitchWithoutStartingAudio() {
        compose.setContent{NurayaTheme(false){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)){ReminderSettings("id")}}}
        await("reminder_settings")
        compose.onNodeWithTag("quran_reminder_toggle").assertIsOn().performClick().assertIsOff()
        assertFalse(QuranReminderScheduler.enabled(app))
        compose.onNodeWithTag("reminder_minutes_30").performScrollTo().performClick().assertIsSelected()
        assertEquals(30,AdhanScheduler.beforeMinutes(app))
        compose.onNodeWithTag("adhan_alarm_toggle").performScrollTo().assertIsOff()
        assertFalse(AdhanScheduler.enabled(app));screenshot("reminder-settings")
    }
    @Test fun firstLocationNoticeIsCompactAndCanBeDismissedBeforeWeeklyNotificationNotice() {
        ProfileStore(app).save("Naufal",0)
        Shadows.shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        var locationVisible by mutableStateOf(true)
        compose.setContent{NurayaTheme(false){
            Button(onClick={}){Text("Konten tetap tersedia")}
            LocationOnboarding("id",onVisibility={locationVisible=it})
            NotificationPermissionNotice("id",allowedToShow=!locationVisible)
        }}
        await("location_setup_dialog")
        compose.onNodeWithTag("notification_permission_notice").assertDoesNotExist()
        assertTrue(PermissionPromptStore(app).notificationDue())
        val location=compose.onNodeWithTag("location_setup_dialog").getUnclippedBoundsInRoot()
        assertTrue(location.bottom-location.top<550.dp)
        screenshot("location-permission-card")
        compose.onNodeWithTag("device_location_later").performClick()
        await("notification_permission_notice")
        assertFalse(PermissionPromptStore(app).notificationDue())
        screenshot("notification-permission-card")
        compose.onNodeWithTag("notification_permission_later").performClick()
        compose.onNodeWithTag("notification_permission_notice").assertDoesNotExist()
        compose.onNodeWithText("Konten tetap tersedia").performClick()
        compose.onNodeWithTag("notification_permission_notice").assertDoesNotExist()
    }
    @Test fun contextualQiblaPromptStillAppearsAfterInitialLocationDismissal() {
        LocationPromptStore(app).markIntroduced()
        compose.setContent{NurayaTheme(false){QiblaScreen("id")}}
        await("location_setup_dialog")
        compose.onNodeWithTag("device_location_later").performClick()
        compose.onNodeWithText("Tentukan lokasi terlebih dahulu").assertIsDisplayed()
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
    }
    @Test fun qiblaUsesBlueAndAboutIncludesAcademicStatementAndSources() {
        openApp()
        compose.onNodeWithTag("home_menu").performScrollToNode(hasTestTag("home_qibla"))
        compose.onNodeWithTag("home_qibla").performClick();await("qibla_panel")
        compose.onNodeWithTag("device_location_later").performClick()
        screenshot("qibla-blue")
        compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()};await("home_menu")
        compose.onNodeWithTag("home_profile").performClick();await("profile_screen")
        compose.onNodeWithTag("profile_screen").performScrollToNode(hasText("Tentang Nuraya"))
        compose.onNodeWithText("Tentang Nuraya").performClick();await("about_screen")
        compose.onNodeWithTag("about_academic").assertTextContains("tanpa menerima imbalan finansial",substring=true)
        compose.onNodeWithTag("about_screen").performScrollToNode(hasTestTag("about_sources"))
        compose.onNodeWithTag("about_sources").assertTextContains("Tanzil",substring=true)
        screenshot("about-sources")
    }
    @Test fun QuranNotificationFromHomeOpensItsOwnVerseAndPrayerNotificationOpensIbadah() {
        openApp(savedMode="standard")
        compose.runOnIdle{ReminderNavigation.accept(Shadows.shadowOf(ReminderNavigation.pending(app,"test",112,1)).savedIntent)}
        await("mushaf_page_image")
        compose.onNodeWithContentDescription("Mushaf Indonesia halaman 604").assertIsDisplayed()
        assertNull(ReminderNavigation.requests.value)
        compose.runOnIdle{ReminderNavigation.accept(Shadows.shadowOf(ReminderNavigation.pending(app,"prayer")).savedIntent)}
        await("prayer_screen")
    }
    @Test fun bannerSlidesAutomaticallyAndManualSwipeStillWorks() {
        app.getSharedPreferences("nuraya_banners",0).edit().putLong("checked",System.currentTimeMillis()).commit()
        var destination=""
        compose.setContent{NurayaTheme(false){BannerCarousel("id",onNavigate={destination=it})}}
        await("home_banner_nuraya-learn")
        compose.mainClock.autoAdvance=false
        compose.mainClock.advanceTimeBy(6_500)
        compose.waitForIdle()
        compose.onNodeWithTag("home_banner_nuraya-explore").assertIsDisplayed()
        compose.onNodeWithTag("home_banners").performTouchInput{swipeRight()}
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("home_banner_nuraya-learn").assertIsDisplayed().performClick()
        assertEquals("learn",destination)
    }
    @Test fun largeFontPermissionCardKeepsItsActionsVisibleAndOfflineCardDismisses() {
        // Exercise a fresh prompt, independently of the weekly-dismissal tests.
        app.getSharedPreferences("nuraya_permission_prompts", 0).edit().clear().commit()
        assertTrue(PermissionPromptStore(app).notificationDue())
        Shadows.shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        var showNotification by mutableStateOf(true)
        compose.setContent{NurayaTheme(false){CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.5f)){
            if(showNotification)NotificationPermissionNotice("id",true) else OfflineNotice("id")
        }}}
        await("notification_permission_notice")
        compose.onNodeWithTag("notification_permission_enable").assertIsDisplayed()
        screenshot("notification-permission-large-font")
        compose.onNodeWithTag("notification_permission_later").assertIsDisplayed().performClick()
        compose.runOnIdle{showNotification=false;NetworkNotice.warnIfOffline(app)}
        await("offline_notice")
        screenshot("offline-card")
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithTag("offline_notice").assertDoesNotExist()
    }
}
