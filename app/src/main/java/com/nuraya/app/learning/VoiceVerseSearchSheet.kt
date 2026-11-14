package com.nuraya.app.learning

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.NurayaApplication
import com.nuraya.app.recitation.*
import com.nuraya.app.recitation.ai.QuranWhisperEngine
import com.nuraya.app.ui.label
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceVerseSearchSheet(app: NurayaApplication, language: String, onDismiss: () -> Unit, onRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onRecognized)
    var state by remember { mutableStateOf(LocalQuranRecitationState()) }
    val session = remember(context) { LocalQuranRecitationSession(context, onState = { state = it }, onFinal = { callback(it) }) }
    val model = remember(context) { QuranModelStore(context) }
    val modelState by model.state.collectAsStateWithLifecycle()
    val supported = remember { QuranWhisperEngine.isSupported() }
    var job by remember { mutableStateOf<Job?>(null) }
    var denied by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val start by rememberUpdatedState(newValue = {
        if (foreground && supported && modelState.phase == ModelPhase.READY) { app.playback.pause(); session.start(model.modelFile) }
    })
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> denied = !granted; if (granted) start() }
    LaunchedEffect(model) { model.inspect() }
    LaunchedEffect(state.elapsedSeconds, state.speech.listening) { if (state.elapsedSeconds >= 20 && state.speech.listening) session.stopListening() }
    DisposableEffect(session, model, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (event == Lifecycle.Event.ON_STOP) session.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); session.destroy(); model.cancel(); job?.cancel() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).testTag("voice_verse_search"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label(language, "Cari ayat lewat suara", "Find a verse by voice"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, label(language, "Tutup pencarian suara", "Close voice search")) }
            }
            Text(label(language, "Bacakan potongan ayat dalam bahasa Arab selama 5–20 detik, lalu tekan Selesai. Nuraya mencari ayat yang cocok dari bacaan yang dikenali.",
                "Recite an Arabic verse fragment for 5–20 seconds, then tap Finish. Nuraya searches for matching verses from the recognized words."))
            QuranModelCard(language, modelState, supported, model.spec.bytes,
                unsupportedMessage = label(language, "Mesin suara belum mendukung perangkat ini. Anda tetap dapat mengetik potongan ayat di pencarian.", "Voice recognition is not supported on this device. You can still type the verse fragment."),
                onDownload = { job = scope.launch { model.download() } }, onCancel = { model.cancel(); job?.cancel() })
            if (denied) Text(label(language, "Izin mikrofon diperlukan untuk pencarian suara. Izinkan lewat Pengaturan Android jika permintaan izin tidak muncul lagi.", "Microphone permission is required. Allow it in Android Settings if the permission request no longer appears."), color = MaterialTheme.colorScheme.error)
            Text(statusText(state.speech.status, language), Modifier.testTag("voice_search_status"), style = MaterialTheme.typography.titleSmall)
            if (state.speech.listening) {
                LinearProgressIndicator(progress = { state.audioLevel }, modifier = Modifier.fillMaxWidth())
                Text(label(language, "Mendengarkan · ${state.elapsedSeconds} / 20 detik", "Listening · ${state.elapsedSeconds} / 20 seconds"))
            }
            if (state.speech.waiting && !state.speech.listening) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.speech.transcript.isNotBlank()) Text(state.speech.transcript, Modifier.fillMaxWidth().testTag("voice_search_transcript"))
            Button(onClick = {
                if (state.speech.listening) session.stopListening()
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                else permission.launch(Manifest.permission.RECORD_AUDIO)
            }, enabled = state.speech.listening || (!state.speech.waiting && supported && modelState.phase == ModelPhase.READY), modifier = Modifier.fillMaxWidth().testTag("voice_search_record")) {
                Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(8.dp))
                Text(label(language, if (state.speech.listening) "Selesai & cari ayat" else "Mulai membaca", if (state.speech.listening) "Finish & find verse" else "Start reciting"))
            }
            TextButton(onClick = { session.cancel(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text(label(language, "Batal", "Cancel")) }
            Text(label(language, "Suara diproses di HP. Hasil pengenalan bisa keliru; cocokkan potongan yang dikenali dengan ayat hasil pencarian.",
                "Audio is processed on your phone. Recognition can be wrong; compare the recognized fragment with the returned verses."), style = MaterialTheme.typography.bodySmall)
        }
    }
}
