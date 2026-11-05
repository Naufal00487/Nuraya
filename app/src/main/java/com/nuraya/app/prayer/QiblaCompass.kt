package com.nuraya.app.prayer

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.ui.label
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class CompassReading(val supported: Boolean, val heading: Double? = null, val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE)

@Suppress("DEPRECATION")
@Composable
internal fun rememberCompass(location: PrayerLocation): CompassReading {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val vector = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val accelerometer = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val magnetic = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
    val supported = vector != null || accelerometer != null && magnetic != null
    var reading by remember(location, supported) { mutableStateOf(CompassReading(supported)) }
    DisposableEffect(manager, vector, accelerometer, magnetic, location, lifecycle) {
        val declination = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        var gravity: FloatArray? = null
        var field: FloatArray? = null
        var sensorAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
        var registered = false
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) sensorAccuracy = accuracy
            }
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    sensorAccuracy = event.accuracy
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = event.values.clone()
                    if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) { field = event.values.clone(); sensorAccuracy = event.accuracy }
                    if (gravity == null || field == null || !SensorManager.getRotationMatrix(matrix, null, gravity, field)) return
                }
                val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                val axes = when (rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                val remapped = FloatArray(9)
                if (!SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, remapped)) return
                val orientation = SensorManager.getOrientation(remapped, FloatArray(3))
                val heading = (Math.toDegrees(orientation[0].toDouble()) + declination + 360.0) % 360.0
                if (heading.isFinite()) reading = CompassReading(supported, heading, sensorAccuracy)
            }
        }
        fun stop() { manager?.unregisterListener(listener); registered = false }
        fun start() {
            if (registered || !supported || manager == null) return
            registered = if (vector != null) manager.registerListener(listener, vector, SensorManager.SENSOR_DELAY_UI)
            else {
                val first = manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                val second = manager.registerListener(listener, magnetic, SensorManager.SENSOR_DELAY_UI)
                if (!first || !second) manager.unregisterListener(listener)
                first && second
            }
            if (!registered) reading = CompassReading(false)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { stop(); lifecycle.removeObserver(observer) }
    }
    return reading
}

@Composable
internal fun QiblaPanel(location: PrayerLocation, language: String) {
    val bearing = remember(location) { PrayerCalculator.qiblaBearing(location) }
    val compass = rememberCompass(location)
    val turn = compass.heading?.let { PrayerCalculator.signedTurn(bearing, it) }
    val trusted = compass.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().testTag("qibla_panel"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label(language, "Arah kiblat", "Qibla direction"), style = MaterialTheme.typography.headlineMedium)
        Text(location.name, style = MaterialTheme.typography.titleLarge)
        Text(label(language, "${bearing.roundToInt()}° dari utara sejati", "${bearing.roundToInt()}° from true north"), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("qibla_bearing"), textAlign = TextAlign.Center)
        if (turn != null) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension * .44f
                    drawCircle(outline, radius, style = Stroke(3.dp.toPx()))
                    rotate(turn.toFloat()) {
                        val arrow = Path().apply {
                            moveTo(center.x, center.y - radius * .82f)
                            lineTo(center.x - radius * .24f, center.y - radius * .23f)
                            lineTo(center.x - radius * .08f, center.y - radius * .23f)
                            lineTo(center.x - radius * .08f, center.y + radius * .55f)
                            lineTo(center.x + radius * .08f, center.y + radius * .55f)
                            lineTo(center.x + radius * .08f, center.y - radius * .23f)
                            lineTo(center.x + radius * .24f, center.y - radius * .23f)
                            close()
                        }
                        drawPath(arrow, primary.copy(alpha = if (trusted) 1f else .45f))
                    }
                }
            }
            Text(when {
                !trusted -> label(language, "Kalibrasi kompas terlebih dahulu", "Calibrate the compass first")
                abs(turn) < 5.0 -> label(language, "Arah perangkat mendekati kiblat", "Your device is pointing near the Qibla")
                turn > 0 -> label(language, "Putar ke kanan ${abs(turn).roundToInt()}°", "Turn right ${abs(turn).roundToInt()}°")
                else -> label(language, "Putar ke kiri ${abs(turn).roundToInt()}°", "Turn left ${abs(turn).roundToInt()}°")
            }, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        } else Text(if (!compass.supported) label(language,
            "Sensor kompas tidak tersedia. Gunakan arah derajat di atas dengan kompas lain; panah tidak ditampilkan tanpa pembacaan sensor.",
            "No compass sensor is available. Use the bearing above with another compass; an arrow is not shown without sensor readings.")
            else label(language, "Menunggu pembacaan sensor kompas…", "Waiting for compass sensor readings…"), textAlign = TextAlign.Center)
        Text(label(language,
            "Letakkan HP mendatar. Jauhkan dari magnet, logam, dan perangkat elektronik. Jika arah belum stabil, gerakkan HP membentuk angka delapan lalu periksa ulang. Arah dihitung dari lokasi yang Anda pilih.",
            "Hold your phone flat, away from magnets, metal and electronics. If the direction is unstable, move the phone in a figure eight and check again. The bearing uses your selected location."),
            style = MaterialTheme.typography.bodyLarge)
    }
}
