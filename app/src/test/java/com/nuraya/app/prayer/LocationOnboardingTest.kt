package com.nuraya.app.prayer

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.ui.NurayaTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w411dp-h891dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocationOnboardingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val manager get() = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Before fun reset() {
        app.getSharedPreferences("nuraya_location_prompt", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("nuraya_prayer", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        ShadowSystemClock.advanceBy(Duration.ofMinutes(5))
    }

    @Test fun firstLaunchRequestsBothForegroundPermissionsAndDenialKeepsManualReadingAvailable() {
        var manual = false
        compose.setContent { NurayaTheme { LocationOnboarding("id", onManualLocation = { manual = true }) } }
        compose.onNodeWithTag("location_setup_dialog").assertIsDisplayed()
        compose.onNodeWithTag("device_location_request").performClick()
        val requested = requireNotNull(shadowOf(compose.activity).lastRequestedPermission)
        assertEquals(setOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), requested.requestedPermissions.toSet())
        compose.runOnIdle {
            compose.activity.onRequestPermissionsResult(requested.requestCode, requested.requestedPermissions,
                IntArray(requested.requestedPermissions.size) { PackageManager.PERMISSION_DENIED })
        }
        compose.onNodeWithTag("device_location_manual").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(manual)
        assertNull(PrayerPreferenceStore(app).read().location)
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
    }

    @Test fun dismissingTheIntroductionDoesNotLoopOrRegisterLocationListeners() {
        val mounted = mutableStateOf(true)
        compose.setContent { NurayaTheme { if (mounted.value) LocationOnboarding("id") } }
        compose.onNodeWithTag("device_location_later").performClick()
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
        compose.runOnIdle { mounted.value = false }
        compose.waitForIdle()
        compose.runOnIdle { mounted.value = true }
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
        assertFalse(LocationPromptStore(app).shouldIntroduce(false))
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun acceptedFreshApproximateLocationSavesCoordinatesWithoutReplacingScheduleCorrections() {
        val store = PrayerPreferenceStore(app)
        store.save(PrayerPreferences(hijriAdjustment = 1, minuteAdjustment = 5))
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER, Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = -7.25; longitude = 112.75; accuracy = 800f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(); time = System.currentTimeMillis()
        })
        compose.setContent { NurayaTheme { LocationOnboarding("id") } }
        compose.onNodeWithTag("device_location_request").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("device_location_done").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Menggunakan izin lokasi perkiraan.").assertExists()
        val updated = store.read()
        assertEquals(-7.25, requireNotNull(updated.location).latitude, .00001)
        assertEquals(112.75, requireNotNull(updated.location).longitude, .00001)
        assertEquals(1, updated.hijriAdjustment)
        assertEquals(5, updated.minuteAdjustment)
        compose.onNodeWithTag("device_location_done").performClick()
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
    }

    @Test fun existingManualCityIsPreservedAndExplicitRefreshShowsLocationServicesAction() {
        val store = PrayerPreferenceStore(app)
        val original = PrayerCities.all.first { it.name == "Jakarta" }
        store.save(PrayerPreferences(location = original))
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        compose.setContent { NurayaTheme { Surface {
            LocationOnboarding("id")
            DeviceLocationButton("id")
        } } }
        compose.onNodeWithTag("location_setup_dialog").assertDoesNotExist()
        compose.onNodeWithTag("device_location_button").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("device_location_enable_services").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("device_location_enable_services").assertIsDisplayed()
        assertEquals(original, store.read().location)
        compose.onNodeWithTag("device_location_later").performClick()
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }
}
