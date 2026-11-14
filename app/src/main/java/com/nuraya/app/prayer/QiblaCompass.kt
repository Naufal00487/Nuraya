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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.cos
import kotlin.math.sin
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
fun QiblaScreen(language:String) {
    val (prefs, _) = rememberPrayerPreferences()
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF247F78),Color(0xFF144B48)))).verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        if(prefs.location!=null) QiblaPanel(prefs.location,language)
        else {
            Spacer(Modifier.height(60.dp))
            Text(label(language,"Tentukan lokasi terlebih dahulu","Choose your location first"),color=Color.White,style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp));DeviceLocationButton(language)
        }
    }
}

@Composable
internal fun QiblaPanel(location: PrayerLocation, language: String) {
    val bearing=remember(location){PrayerCalculator.qiblaBearing(location)}
    val compass=rememberCompass(location)
    val turn=compass.heading?.let{PrayerCalculator.signedTurn(bearing,it)}
    val trusted=compass.accuracy>=SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    var info by remember{mutableStateOf(false)}
    Surface(color=Color(0xFF1E6660),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().testTag("qibla_panel")) {
        Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(label(language,"Arah kiblat","Qibla direction"),Modifier.weight(1f),color=Color.White,style=MaterialTheme.typography.headlineSmall)
                IconButton(onClick={info=true}){Icon(Icons.Outlined.Info,label(language,"Panduan kompas","Compass guide"),tint=Color.White)}
            }
            Text(location.name,color=Color(0xFFD8EEDE),style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val r=size.minDimension*.39f
                val white=Color(0xFFF4F2DC);val gold=Color(0xFFE7BF65)
                repeat(16){i->rotate(i*22.5f){drawOval(Color(0xFF348E84),topLeft=Offset(center.x-r*.38f,center.y-r*1.22f),size=Size(r*.76f,r*1.8f))}}
                drawCircle(Color(0xFF123D39),r*1.08f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF4F8278),Color(0xFF24524C))),r)
                rotate(-(compass.heading?:0.0).toFloat()){
                    repeat(180){i->rotate(i*2f){drawLine(white.copy(alpha=if(i%5==0)1f else .6f),Offset(center.x,center.y-r*.94f),Offset(center.x,center.y-r*(if(i%5==0).85f else .9f)),if(i%5==0)2.dp.toPx() else 1.dp.toPx())}}
                    drawIntoCanvas{canvas->
                        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.WHITE;textAlign=android.graphics.Paint.Align.CENTER;textSize=12.dp.toPx()}
                        for(deg in 0 until 360 step 20){val angle=Math.toRadians(deg.toDouble()-90);canvas.nativeCanvas.drawText(deg.toString(),center.x+(cos(angle)*r*.74).toFloat(),center.y+(sin(angle)*r*.74).toFloat()+4.dp.toPx(),paint)}
                        paint.textSize=18.dp.toPx();paint.isFakeBoldText=true
                        listOf(if(language=="en")"N" else "U",if(language=="en")"E" else "T","S",if(language=="en")"W" else "B").forEachIndexed{i,t->val angle=Math.toRadians(i*90.0-90);canvas.nativeCanvas.drawText(t,center.x+(cos(angle)*r*1.2).toFloat(),center.y+(sin(angle)*r*1.2).toFloat()+6.dp.toPx(),paint)}
                    }
                }
                if(turn!=null)rotate(turn.toFloat()) {
                    val arrow=Path().apply{moveTo(center.x,center.y-r*.67f);lineTo(center.x-r*.10f,center.y+r*.12f);lineTo(center.x+r*.10f,center.y+r*.12f);close()}
                    drawPath(arrow,gold.copy(alpha=if(trusted)1f else .45f))
                    val target=Offset(center.x,center.y-r*.57f)
                    drawCircle(gold,r*.13f,target)
                    drawRect(Color(0xFF171B25),Offset(target.x-r*.075f,target.y-r*.075f),Size(r*.15f,r*.15f))
                    drawRect(Color(0xFFE7D6A2),Offset(target.x-r*.075f,target.y-r*.025f),Size(r*.15f,r*.025f))
                }
                drawCircle(gold,r*.045f);drawCircle(Color.White,r*.025f)
            }
            Text(label(language,"Kiblat","Qibla"),color=Color.White,style=MaterialTheme.typography.headlineMedium)
            Text(label(language,"${bearing.roundToInt()}° dari utara sejati","${bearing.roundToInt()}° from true north"),color=Color(0xFFE7BF65),style=MaterialTheme.typography.titleLarge,modifier=Modifier.testTag("qibla_bearing"),textAlign=TextAlign.Center)
            Text(when {
                !compass.supported -> label(language,"Sensor kompas tidak tersedia. Gunakan arah derajat di atas dengan kompas lain; panah tidak ditampilkan tanpa sensor.","No compass sensor. Use the bearing above with another compass; no arrow is shown without sensor data.")
                turn==null -> label(language,"Menunggu sensor kompas…","Waiting for compass sensor…")
                !trusted -> label(language,"Kalibrasi kompas terlebih dahulu","Calibrate the compass first")
                abs(turn)<5 -> label(language,"Arah perangkat mendekati kiblat","Your device is pointing near the Qibla")
                turn>0 -> label(language,"Putar ke kanan ${abs(turn).roundToInt()}°","Turn right ${abs(turn).roundToInt()}°")
                else -> label(language,"Putar ke kiri ${abs(turn).roundToInt()}°","Turn left ${abs(turn).roundToInt()}°")
            },color=Color.White,textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyLarge)
            Text(label(language,"Jauhkan HP dari magnet dan logam. Gerakkan membentuk angka delapan untuk kalibrasi.","Keep away from magnets and metal. Move your phone in a figure eight to calibrate."),color=Color(0xFFD8EEDE),textAlign=TextAlign.Center,style=MaterialTheme.typography.bodySmall)
        }
    }
    if(info)AlertDialog(onDismissRequest={info=false},title={Text(label(language,"Menggunakan kompas","Using the compass"))},text={Text(label(language,"Letakkan HP mendatar. Arah kiblat dihitung dari lokasi Anda menuju Ka'bah; putaran penunjuk mengikuti sensor perangkat dan koreksi utara sejati. Jika sensor belum akurat, lakukan kalibrasi sebelum mengikuti panah.","Hold the phone flat. The Qibla bearing uses your location and the device sensor with true-north correction. Calibrate before following an unreliable pointer."))},confirmButton={TextButton(onClick={info=false}){Text(label(language,"Mengerti","Got it"))}})
}
