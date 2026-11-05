package com.nuraya.app.prayer

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import java.time.ZoneId

/** Android location-manager callbacks are controlled here; this is not a physical GPS accuracy test. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DeviceLocationTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()
    private val manager get() = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Before fun prepare() {
        Dispatchers.setMain(StandardTestDispatcher())
        ShadowSystemClock.advanceBy(Duration.ofMinutes(5))
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        app.getSharedPreferences("nuraya_location_prompt", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun finish() { Dispatchers.resetMain() }

    private fun grant(precise: Boolean = true) {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (precise) shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fix(provider: String = LocationManager.NETWORK_PROVIDER, ageSeconds: Long = 0, accuracy: Float = 25f) = Location(provider).apply {
        latitude = -6.2; longitude = 106.8
        this.accuracy = accuracy
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - ageSeconds * 1_000_000_000L
        time = System.currentTimeMillis() - ageSeconds * 1000L
    }

    @Test fun permissionDenialAndDisabledServicesReturnWithoutRegisteringListeners() = runTest {
        assertEquals(DeviceLocationResult.Failed(LocationProblem.PERMISSION), DeviceLocationReader(app).find())
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
        grant()
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        assertEquals(DeviceLocationResult.Failed(LocationProblem.DISABLED), DeviceLocationReader(app).find())
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun coarsePermissionUsesOnlyFreshNetworkCoordinatesAndThePhonesZone() = runTest {
        grant(precise = false)
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER, fix(accuracy = 1500f))
        shadowOf(manager).setLastKnownLocation(LocationManager.GPS_PROVIDER, fix(LocationManager.GPS_PROVIDER, accuracy = 1f).apply { latitude = 10.0 })
        val result = DeviceLocationReader(app).find() as DeviceLocationResult.Found
        assertEquals(-6.2, result.location.latitude, .00001)
        assertEquals(106.8, result.location.longitude, .00001)
        assertEquals(ZoneId.systemDefault().id, result.location.zoneId)
        assertTrue(result.approximate)
        assertEquals(1500f, result.accuracyMeters, 0f)
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun staleOrInaccurateFirstCallbackDoesNotConsumeTheOnlyChanceToFindLocation() = runTest {
        grant()
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER, fix(ageSeconds = 180))
        val pending = async { DeviceLocationReader(app).find() }
        runCurrent()
        assertFalse(pending.isCompleted)
        assertTrue(shadowOf(manager).locationUpdateListeners.isNotEmpty())
        shadowOf(manager).simulateLocation(LocationManager.NETWORK_PROVIDER, fix(accuracy = 20_000f))
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertFalse(pending.isCompleted)
        assertTrue(shadowOf(manager).locationUpdateListeners.isNotEmpty())
        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
        shadowOf(manager).simulateLocation(LocationManager.GPS_PROVIDER, fix(LocationManager.GPS_PROVIDER, accuracy = 15f))
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertTrue(pending.await() is DeviceLocationResult.Found)
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun timeoutAndCancellationReleaseEveryProviderListener() = runTest {
        grant()
        val reader = DeviceLocationReader(app)
        val timed = async { reader.find() }
        runCurrent()
        assertTrue(shadowOf(manager).locationUpdateListeners.isNotEmpty())
        advanceTimeBy(25_001)
        runCurrent()
        assertEquals(DeviceLocationResult.Failed(LocationProblem.TIMEOUT), timed.await())
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
        val cancelled = async { reader.find() }
        runCurrent()
        assertTrue(shadowOf(manager).locationUpdateListeners.isNotEmpty())
        cancelled.cancel()
        runCurrent()
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun turningProvidersOffDuringLookupReturnsDisabledAndRemovesListeners() = runTest {
        grant()
        val pending = async { DeviceLocationReader(app).find() }
        runCurrent()
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowOf(Looper.getMainLooper()).idle()
        runCurrent()
        assertEquals(DeviceLocationResult.Failed(LocationProblem.DISABLED), pending.await())
        assertTrue(shadowOf(manager).locationUpdateListeners.isEmpty())
    }

    @Test fun cachedFixSelectionRejectsFutureStaleAndUnreliableCoordinates() {
        val now = SystemClock.elapsedRealtimeNanos()
        val recent = fix(ageSeconds = 20, accuracy = 100f)
        val accurate = fix(ageSeconds = 60, accuracy = 10f)
        val stale = fix(ageSeconds = 121, accuracy = 1f)
        val future = fix(ageSeconds = -1)
        val poor = fix(accuracy = 10_001f)
        assertEquals(accurate, bestCachedLocation(listOf(recent, accurate, stale, future, poor), now))
        assertFalse(usableLocationFix(fix().apply { latitude = Double.NaN }, now))
        assertFalse(usableLocationFix(fix().apply { removeAccuracy() }, now))
        assertNull(bestCachedLocation(listOf(stale, future, poor), now))
    }

    @Test fun introductionIsOnceOnlyAndExistingManualLocationIsPreserved() {
        val store = LocationPromptStore(app)
        assertTrue(store.shouldIntroduce(false))
        assertFalse(store.shouldIntroduce(true))
        assertFalse(store.hasRequestedPermission())
        store.markIntroduced()
        store.markPermissionRequested()
        val reopened = LocationPromptStore(app)
        assertFalse(reopened.shouldIntroduce(false))
        assertTrue(reopened.hasRequestedPermission())
    }
}
