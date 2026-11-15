package com.nuraya.app.learning

import com.nuraya.app.ui.icons.AppIcons as Icons
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceVerseSearchSheet(app: NurayaApplication, language: String, onDismiss: () -> Unit, onRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callback by rememberUpdatedState(onRecognized)
    val referenceSearch = remember(app) { QuranReferenceSearch(app.quran) }
    var state by remember { mutableStateOf(QuerySpeechState()) }
    val session = remember(context) { QuerySpeechSession(context, { state = it }, { callback(referenceSearch.preferredTranscript(it)) }) }
    var denied by remember { mutableStateOf(false) }
    var useSystem by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val start by rememberUpdatedState(newValue = {
        if (foreground) { app.playback.pause(); session.start(useSystem) }
    })
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> denied = !granted; if (granted) start() }
    fun requestStart(system: Boolean = false) {
        useSystem = system
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (foreground) { app.playback.pause(); session.start(system) }
        } else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(session, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (event == Lifecycle.Event.ON_STOP) session.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); session.destroy() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).testTag("voice_verse_search"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label(language, "Cari dengan suara", "Voice search"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, label(language, "Tutup pencarian suara", "Close voice search")) }
            }
            Text(label(language, "Bicara dalam bahasa Indonesia. Contoh: “cari surat Yasin”, “Al-Baqarah ayat dua ratus lima puluh lima”, atau “ayat tentang sabar”.",
                "Speak in Indonesian: for example, “cari surat Yasin”, “Al-Baqarah ayat dua ratus lima puluh lima”, or “ayat tentang sabar”."), Modifier.testTag("voice_search_language"))
            Text(label(language, "Hasil ucapan menjadi kata pencarian dan bisa kamu edit. Tidak perlu membaca ayat Arab atau menilai bacaan.",
                "Your words become an editable search query. You do not need to recite Arabic or have your recitation assessed."))
            Text(label(language, "Pengenalan suara memakai layanan Android. Jika pengenal Indonesia offline tidak tersedia, layanan perangkat dapat memerlukan internet dan memproses audio melalui penyedianya.",
                "Dictation uses Android speech services. Without offline Indonesian recognition, your device provider may require internet and process the audio."), style = MaterialTheme.typography.bodySmall)
            if (denied) Text(label(language, "Izinkan mikrofon untuk mencari dengan suara. Jika izin tidak muncul lagi, aktifkan melalui Pengaturan Android.", "Allow microphone access for voice search. If no prompt appears, enable it in Android Settings."), color = MaterialTheme.colorScheme.error)
            Text(label(language, when(state.status) {
                "starting", "listening" -> "Mendengarkan bahasa Indonesia…"
                "processing" -> "Mengubah ucapan menjadi pencarian…"
                "language_unavailable" -> "Bahasa Indonesia belum tersedia pada pengenal suara ini."
                "unavailable" -> "Layanan pengenalan suara belum tersedia di HP."
                "permission" -> "Izin mikrofon belum diberikan."
                "network" -> "Koneksi layanan suara bermasalah. Coba lagi atau ketik pencarian."
                "no_match" -> "Ucapan belum tertangkap. Coba sebut nama surah atau topik lagi."
                "busy" -> "Mikrofon sedang digunakan. Coba lagi setelah selesai."
                "timeout" -> "Layanan suara belum memberi hasil. Silakan coba lagi."
                "audio", "failed" -> "Pengenalan suara belum berhasil. Silakan coba lagi."
                else -> "Siap mencari dengan suara"
            }, when(state.status) {
                "starting", "listening" -> "Listening in Indonesian…"
                "processing" -> "Turning your words into a search…"
                "ready" -> "Ready for voice search"
                else -> "Speech recognition is unavailable or did not return a result. Retry or type your query."
            }), Modifier.testTag("voice_search_status"), style = MaterialTheme.typography.titleSmall)
            if (state.listening) LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth())
            if (state.waiting) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.transcript.isNotBlank()) Text(state.transcript, Modifier.fillMaxWidth().testTag("voice_search_transcript"))
            val available = session.onDeviceAvailable || session.systemAvailable
            if (!available) Text(label(language, "Aktifkan layanan pengenalan suara Android. Pencarian dengan tulisan tetap tersedia.", "Enable an Android speech recognition service. Typed search remains available."))
            Button(onClick = { if (state.listening) session.stopListening() else requestStart() },
                enabled = foreground && (state.listening || !state.waiting && available), modifier = Modifier.fillMaxWidth().testTag("voice_search_record")) {
                Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(8.dp))
                Text(label(language, if (state.listening) "Selesai" else "Mulai bicara", if (state.listening) "Finish" else "Start speaking"))
            }
            if (state.onDevice && !state.listening && !state.waiting && state.status !in setOf("ready", "complete") && session.systemAvailable) {
                OutlinedButton(onClick = { requestStart(true) }, modifier = Modifier.fillMaxWidth().testTag("voice_search_system")) {
                    Text(label(language, "Coba layanan suara Android", "Try the Android speech service"))
                }
            }
            if (!available || state.status == "language_unavailable") TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } }) { Text(label(language,"Pengaturan suara Android","Android voice settings")) }
            TextButton(onClick = { session.cancel(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text(label(language, "Kembali ke pencarian", "Back to search")) }
        }
    }
}
