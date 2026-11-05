package com.nuraya.app.prayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import kotlin.coroutines.resume

enum class LocationProblem { PERMISSION, DISABLED, UNAVAILABLE, TIMEOUT }

sealed interface DeviceLocationResult {
    data class Found(val location: PrayerLocation, val accuracyMeters: Float, val approximate: Boolean) : DeviceLocationResult
    data class Failed(val problem: LocationProblem) : DeviceLocationResult
}

internal const val MAX_LOCATION_AGE_NANOS = 120_000_000_000L
internal const val MAX_LOCATION_ACCURACY_METERS = 10_000f

/** Elapsed time rejects old fixes even when the device's wall clock was changed. */
internal fun usableLocationFix(location: Location, elapsedNanos: Long): Boolean {
    val age = elapsedNanos - location.elapsedRealtimeNanos
    return location.latitude.isFinite() && location.latitude in -89.0..89.0 &&
        location.longitude.isFinite() && location.longitude in -180.0..180.0 &&
        location.hasAccuracy() && location.accuracy.isFinite() &&
        location.accuracy > 0f && location.accuracy <= MAX_LOCATION_ACCURACY_METERS &&
        location.elapsedRealtimeNanos > 0L && age in 0..MAX_LOCATION_AGE_NANOS
}

internal fun bestCachedLocation(fixes: List<Location>, elapsedNanos: Long): Location? =
    fixes.filter { usableLocationFix(it, elapsedNanos) }
        .minWithOrNull(compareBy<Location> { it.accuracy }.thenByDescending { it.elapsedRealtimeNanos })

fun hasDeviceLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Foreground, cancellable one-shot location. No background service or location upload. */
class DeviceLocationReader(context: Context) {
    private val context = context.applicationContext
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun servicesEnabled(): Boolean = manager?.let { service ->
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).any {
            runCatching { service.isProviderEnabled(it) }.getOrDefault(false)
        }
    } ?: false

    @Suppress("MissingPermission", "DEPRECATION")
    suspend fun find(): DeviceLocationResult = withContext(Dispatchers.Main.immediate) {
        if (!hasDeviceLocationPermission(context)) return@withContext DeviceLocationResult.Failed(LocationProblem.PERMISSION)
        val service = manager ?: return@withContext DeviceLocationResult.Failed(LocationProblem.UNAVAILABLE)
        if (!servicesEnabled()) return@withContext DeviceLocationResult.Failed(LocationProblem.DISABLED)
        val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
            (precise || it != LocationManager.GPS_PROVIDER) && runCatching { service.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (providers.isEmpty()) return@withContext DeviceLocationResult.Failed(LocationProblem.UNAVAILABLE)
        fun result(fix: Location): DeviceLocationResult.Found = DeviceLocationResult.Found(
            PrayerLocation("Lokasi HP", fix.latitude, fix.longitude, ZoneId.systemDefault().id), fix.accuracy, !precise,
        )
        val cached = providers.mapNotNull { runCatching { service.getLastKnownLocation(it) }.getOrNull() }
        bestCachedLocation(cached, SystemClock.elapsedRealtimeNanos())?.let { return@withContext result(it) }
        val fix = withTimeoutOrNull(25_000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                lateinit var listener: LocationListener
                fun stop() { runCatching { service.removeUpdates(listener) } }
                listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (usableLocationFix(location, SystemClock.elapsedRealtimeNanos()) && continuation.isActive) {
                            stop()
                            continuation.resume(location)
                        }
                    }
                    override fun onProviderDisabled(provider: String) {
                        if (continuation.isActive && !servicesEnabled()) { stop(); continuation.resume(null) }
                    }
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                continuation.invokeOnCancellation { stop() }
                var registered = false
                for (provider in providers) if (continuation.isActive) {
                    // Keep listening when a provider's first fix is stale/inaccurate.
                    // This bounded foreground lookup ends after the first usable fix.
                    registered = runCatching {
                        service.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper())
                        true
                    }.getOrDefault(false) || registered
                }
                if (!registered && continuation.isActive) { stop(); continuation.resume(null) }
            }
        }
        when {
            fix != null -> result(fix)
            !hasDeviceLocationPermission(context) -> DeviceLocationResult.Failed(LocationProblem.PERMISSION)
            !servicesEnabled() -> DeviceLocationResult.Failed(LocationProblem.DISABLED)
            else -> DeviceLocationResult.Failed(LocationProblem.TIMEOUT)
        }
    }
}

/** The introduction is remembered independently of permission: denying never causes a prompt loop. */
class LocationPromptStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("nuraya_location_prompt", Context.MODE_PRIVATE)
    fun shouldIntroduce(hasSavedLocation: Boolean): Boolean = !hasSavedLocation && !preferences.getBoolean("introduced", false)
    fun markIntroduced() { preferences.edit().putBoolean("introduced", true).apply() }
    fun markPermissionRequested() { preferences.edit().putBoolean("requested", true).apply() }
    fun hasRequestedPermission(): Boolean = preferences.getBoolean("requested", false)
}
