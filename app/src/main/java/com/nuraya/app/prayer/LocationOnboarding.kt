package com.nuraya.app.prayer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.ui.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class LocationStage { INTRO, LOOKING, DENIED, FAILED, FOUND }

private tailrec fun Context.locationActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.locationActivity()
    else -> null
}

/** Place once beside the app content. Existing manual city choices are never replaced automatically. */
@Composable
fun LocationOnboarding(language: String, onManualLocation: () -> Unit = {}) {
    LocationSetupFlow(language, autoPrompt = true, onManualLocation = onManualLocation)
}

/** Explicit refresh on the Ibadah location tab, including after a denial or a trip. */
@Composable
fun DeviceLocationButton(language: String, onLocated: () -> Unit = {}, onManualLocation: () -> Unit = {}) {
    LocationSetupFlow(language, autoPrompt = false, onManualLocation = onManualLocation, onLocated = onLocated)
}

@Composable
private fun LocationSetupFlow(
    language: String,
    autoPrompt: Boolean,
    onManualLocation: () -> Unit,
    onLocated: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context.locationActivity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val reader = remember(context.applicationContext) { DeviceLocationReader(context) }
    val store = remember(context.applicationContext) { PrayerPreferenceStore(context) }
    val prompt = remember(context.applicationContext) { LocationPromptStore(context) }
    var visible by rememberSaveable { mutableStateOf(autoPrompt && prompt.shouldIntroduce(store.read().location != null)) }
    var stage by rememberSaveable { mutableStateOf(LocationStage.INTRO) }
    var problem by remember { mutableStateOf(LocationProblem.UNAVAILABLE) }
    var found by remember { mutableStateOf<DeviceLocationResult.Found?>(null) }
    var waitingForSettings by rememberSaveable { mutableStateOf(false) }
    var lookup by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val latestLocated by rememberUpdatedState(onLocated)
    val latestManual by rememberUpdatedState(onManualLocation)
    val locate: () -> Unit = {
        lookup?.cancel()
        stage = LocationStage.LOOKING
        lookup = scope.launch {
            try {
                when (val result = reader.find()) {
                    is DeviceLocationResult.Found -> {
                        found = result
                        // Read current preferences now, so Hijri and minute corrections survive the lookup.
                        val name=DistrictNameResolver(context).resolve(result.location) ?: label(language,"Lokasi tersimpan","Saved location")
                        val named=result.location.copy(name=name)
                        found=result.copy(location=named)
                        store.save(store.read().copy(location = named))
                        stage = LocationStage.FOUND
                    }
                    is DeviceLocationResult.Failed -> {
                        problem = result.problem
                        stage = if (problem == LocationProblem.PERMISSION) LocationStage.DENIED else LocationStage.FAILED
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                problem = LocationProblem.UNAVAILABLE
                stage = LocationStage.FAILED
            }
        }
    }
    val latestLocate by rememberUpdatedState(locate)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasDeviceLocationPermission(context)) latestLocate() else stage = LocationStage.DENIED
    }
    val request: () -> Unit = {
        prompt.markIntroduced()
        if (hasDeviceLocationPermission(context)) locate() else {
            prompt.markPermissionRequested()
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    val close: () -> Unit = {
        lookup?.cancel()
        prompt.markIntroduced()
        visible = false
    }
    val openSettings: (Boolean) -> Unit = { deviceServices ->
        lookup?.cancel()
        waitingForSettings = true
        val intent = if (deviceServices) Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(intent) }.onFailure {
            waitingForSettings = false
            problem = LocationProblem.UNAVAILABLE
            stage = LocationStage.FAILED
        }
    }
    LaunchedEffect(visible) { if (visible) prompt.markIntroduced() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && stage == LocationStage.LOOKING) {
                lookup?.cancel()
                problem = LocationProblem.UNAVAILABLE
                stage = LocationStage.FAILED
            }
            if (event == Lifecycle.Event.ON_RESUME && waitingForSettings && visible) {
                waitingForSettings = false
                if (hasDeviceLocationPermission(context)) latestLocate() else stage = LocationStage.DENIED
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lookup?.cancel(); lifecycle.removeObserver(observer) }
    }
    // A configuration recreation can restore LOOKING without restoring its coroutine.
    LaunchedEffect(Unit) {
        if (visible && stage == LocationStage.LOOKING && lookup == null) {
            if (hasDeviceLocationPermission(context)) locate() else stage = LocationStage.DENIED
        }
        if (visible && stage == LocationStage.FOUND && found == null) stage = LocationStage.INTRO
    }
    if (!autoPrompt) Button(onClick = {
        visible = true
        stage = LocationStage.INTRO
        found = null
        if (hasDeviceLocationPermission(context)) locate()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("device_location_button")) {
        Text(label(language, "Gunakan lokasi HP", "Use device location"))
    }
    if (!visible) return
    val deniedPermanently = stage == LocationStage.DENIED && prompt.hasRequestedPermission() && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION) &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    AlertDialog(onDismissRequest = { /* Explicit buttons keep the first-use explanation visible until a choice is made. */ },
        modifier = Modifier.testTag("location_setup_dialog"),
        title = { Text(label(language, "Lokasi untuk salat & kiblat", "Location for prayer & Qibla")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (stage) {
                    LocationStage.INTRO -> {
                        Text(label(language, "Aktifkan izin lokasi agar Nuraya mengisi koordinat Anda secara otomatis. Anda tidak perlu mengetik koordinat.",
                            "Allow location access so Nuraya can fill your coordinates automatically. You do not need to type them."))
                        Text(label(language, "Pilih “Saat aplikasi digunakan”. Lokasi perkiraan juga dapat digunakan; lokasi akurat membantu menentukan arah lebih tepat.",
                            "Choose “While using the app”. Approximate location is supported; precise location helps improve the direction."))
                        Text(label(language, "Koordinat disimpan di HP untuk menghitung jadwal dan kiblat. Nuraya tidak mengunggah lokasi atau melacaknya di latar belakang. Zona waktu mengikuti pengaturan HP.",
                            "Coordinates stay on your phone for prayer and Qibla calculations. Nuraya does not upload or track your location in the background. The time zone follows your phone settings."), style = MaterialTheme.typography.bodyMedium)
                    }
                    LocationStage.LOOKING -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth().testTag("device_location_loading"))
                        Text(label(language, "Mencari koordinat HP… Mendekatlah ke jendela atau tempat terbuka jika GPS belum mendapat sinyal.",
                            "Finding your coordinates… Move near a window or outdoors if GPS does not have a signal."))
                    }
                    LocationStage.DENIED -> Text(label(language,
                        if (deniedPermanently) "Izin lokasi belum aktif. Buka pengaturan aplikasi untuk mengizinkannya. Al-Qur’an tetap dapat dibaca tanpa izin lokasi."
                        else "Izin lokasi belum diberikan. Anda dapat mencoba lagi atau memilih kota secara manual. Al-Qur’an tetap dapat dibaca.",
                        if (deniedPermanently) "Location permission is off. Open app settings to allow it. You can still read the Qur’an without location permission."
                        else "Location permission was not granted. Retry or choose a city manually. You can still read the Qur’an."))
                    LocationStage.FAILED -> Text(label(language,
                        if (problem == LocationProblem.DISABLED) "Lokasi/GPS HP masih mati. Aktifkan layanan Lokasi, lalu kembali ke Nuraya."
                        else "Koordinat yang cukup baru dan akurat belum ditemukan. Periksa layanan Lokasi dan sinyal GPS, lalu coba lagi. Lokasi lama tidak diganti dengan tebakan.",
                        if (problem == LocationProblem.DISABLED) "Device location/GPS is off. Turn on Location services and return to Nuraya."
                        else "A sufficiently recent and accurate position was not found. Check Location services and GPS reception, then retry. Your saved location is not replaced with a guess."))
                    LocationStage.FOUND -> found?.let { result ->
                        Text(label(language, "Lokasi berhasil digunakan", "Location is ready"), style = MaterialTheme.typography.titleMedium)
                        Text(String.format(java.util.Locale.ROOT, "%.5f, %.5f", result.location.latitude, result.location.longitude))
                        Text(label(language, "Perkiraan akurasi: ±${result.accuracyMeters.roundToInt()} m", "Estimated accuracy: ±${result.accuracyMeters.roundToInt()} m"))
                        if (result.approximate) Text(label(language, "Menggunakan izin lokasi perkiraan.", "Using approximate location permission."))
                        Text(label(language, "Zona waktu HP: ${result.location.zoneId}. Periksa pengaturan zona waktu otomatis HP jika jadwal berbeda.",
                            "Phone time zone: ${result.location.zoneId}. Check your phone’s automatic time-zone setting if the schedule differs."))
                    }
                }
                if (stage in setOf(LocationStage.DENIED, LocationStage.FAILED)) {
                    TextButton(onClick = { close(); latestManual() }, modifier = Modifier.testTag("device_location_manual")) {
                        Text(label(language, "Pilih kota secara manual", "Choose a city manually"))
                    }
                    if (stage == LocationStage.FAILED && problem != LocationProblem.DISABLED) TextButton(onClick = { openSettings(true) }) {
                        Text(label(language, "Pengaturan Lokasi HP", "Device Location settings"))
                    }
                }
            }
        },
        confirmButton = {
            when {
                stage == LocationStage.LOOKING -> Unit
                stage == LocationStage.FOUND -> Button(onClick = { close(); latestLocated() }, modifier = Modifier.testTag("device_location_done")) {
                    Text(label(language, "Lanjutkan", "Continue"))
                }
                deniedPermanently -> Button(onClick = { openSettings(false) }, modifier = Modifier.testTag("device_location_app_settings")) {
                    Text(label(language, "Buka pengaturan izin", "Open permission settings"))
                }
                stage == LocationStage.FAILED && problem == LocationProblem.DISABLED -> Button(onClick = { openSettings(true) }, modifier = Modifier.testTag("device_location_enable_services")) {
                    Text(label(language, "Aktifkan Lokasi HP", "Turn on device Location"))
                }
                else -> Button(onClick = request, modifier = Modifier.testTag("device_location_request")) {
                    Text(label(language, if (stage == LocationStage.INTRO) "Aktifkan izin lokasi" else "Coba lagi",
                        if (stage == LocationStage.INTRO) "Allow location access" else "Try again"))
                }
            }
        },
        dismissButton = {
            if (stage != LocationStage.FOUND) TextButton(onClick = close, modifier = Modifier.testTag("device_location_later")) {
                Text(label(language, if (stage == LocationStage.LOOKING) "Batalkan" else "Nanti dulu",
                    if (stage == LocationStage.LOOKING) "Cancel" else "Not now"))
            }
        })
}
