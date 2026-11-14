package com.nuraya.app.recitation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.NurayaApplication
import com.nuraya.app.recitation.ai.QuranWhisperEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.nuraya.app.ui.QuranArabicFont
import com.nuraya.app.ui.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecitationPracticeScreen(app: NurayaApplication, language: String, onRead: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var surahId by rememberSaveable { mutableIntStateOf(1) }
    var ayahId by rememberSaveable { mutableIntStateOf(1) }
    var menuOpen by remember { mutableStateOf(false) }
    var systemConsent by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var permissionForMicrophoneCheck by remember { mutableStateOf(false) }
    var systemSpeech by remember { mutableStateOf(SpeechSessionState()) }
    var localSpeech by remember { mutableStateOf(LocalQuranRecitationState()) }
    var aiMode by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val modelStore = remember(context) { QuranModelStore(context) }
    val modelState by modelStore.state.collectAsStateWithLifecycle()
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    val nativeSupported = remember { QuranWhisperEngine.isSupported() }
    val speech = if (aiMode) localSpeech.speech else systemSpeech
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val surah = app.quran.surahs.first { it.id == surahId }
    val verse = app.quran.verses(surahId).first { it.ayah == ayahId }
    val latestVerse by rememberUpdatedState(verse)
    val preferences = remember { context.getSharedPreferences("recitation_practice", android.content.Context.MODE_PRIVATE) }
    var attempts by remember { mutableIntStateOf(preferences.getInt("completed_attempts", 0)) }
    var practicedVerses by remember { mutableStateOf(preferences.getStringSet("practiced_verses", emptySet()).orEmpty().toSet()) }
    val recordAttempt: (String) -> Unit = {
        attempts = (attempts.toLong() + 1).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        practicedVerses = practicedVerses + "${latestVerse.surah}:${latestVerse.ayah}"
        // Keep progress, never voice recordings or recognized transcripts.
        preferences.edit().putInt("completed_attempts", attempts).putStringSet("practiced_verses", practicedVerses).apply()
    }
    val latestRecordAttempt by rememberUpdatedState(recordAttempt)
    val session = remember(context) {
        RecitationSpeechSession(context, onState = { systemSpeech = it }, onFinal = { latestRecordAttempt(it) })
    }
    val localSession = remember(context) {
        LocalQuranRecitationSession(context, onState = { localSpeech = it }, onFinal = { latestRecordAttempt(it) })
    }
    LaunchedEffect(modelStore) { modelStore.inspect() }
    val localAvailable = remember(session) { session.onDeviceAvailable }
    val systemAvailable = remember(session) { session.systemAvailable }
    val active = speech.listening || speech.waiting
    val latestConsent by rememberUpdatedState(systemConsent)
    val start: () -> Unit = {
        if (foreground) {
            app.playback.pause()
            if (aiMode && nativeSupported && modelState.phase == ModelPhase.READY) localSession.start(modelStore.modelFile)
            else if (!aiMode) session.start(latestConsent)
        }
    }
    val latestStart by rememberUpdatedState(start)
    val checkMicrophone: () -> Unit = {
        if (foreground) {
            app.playback.pause()
            session.cancel()
            localSession.checkMicrophone()
        }
    }
    val latestCheckMicrophone by rememberUpdatedState(checkMicrophone)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted && foreground) {
            if (permissionForMicrophoneCheck) latestCheckMicrophone() else latestStart()
        }
        permissionForMicrophoneCheck = false
    }
    DisposableEffect(lifecycle, session, localSession, modelStore) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (event == Lifecycle.Event.ON_STOP) { session.cancel(); localSession.cancel() }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            session.destroy(); localSession.destroy()
            modelStore.cancel(); downloadJob?.cancel()
        }
    }
    LaunchedEffect(surahId, ayahId, aiMode) { session.cancel(); localSession.cancel() }
    val comparison = remember(verse.arabic, speech.transcript, speech.finalResult) {
        RecitationMatcher.compare(verse.arabic, speech.transcript, speech.finalResult)
    }
    val matchedColor = MaterialTheme.colorScheme.primaryContainer
    val uncertainColor = MaterialTheme.colorScheme.tertiaryContainer
    val markedArabic = remember(verse.arabic, comparison, matchedColor, uncertainColor) {
        buildAnnotatedString {
            append(verse.arabic)
            comparison.words.forEach { word ->
                val color = when (word.match) {
                    WordMatch.MATCHED -> matchedColor
                    WordMatch.DIFFERENT, WordMatch.NOT_HEARD -> uncertainColor
                    WordMatch.PENDING -> Color.Unspecified
                }
                if (color != Color.Unspecified) addStyle(SpanStyle(background = color), word.start, word.end)
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("recitation_practice_screen"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text(label(language, "AI bacaan Al-Qur’an", "Qur’an recitation AI"), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(label(language,
                "AI mengenali bacaan lalu membandingkannya dengan ayat yang dipilih. Kata cocok disorot selama membaca; kata yang perlu ditinjau ditampilkan setelah selesai. Hasil dapat keliru dan belum menilai tajwid atau makhraj.",
                "AI recognizes your recitation and compares it with the selected verse. Matching words are highlighted while reading; words to review appear after finishing. Results can be wrong and do not grade tajwid or articulation."),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = aiMode, enabled = !active, onClick = { aiMode = true },
                    label = { Text(label(language, "AI Quran di perangkat", "On-device Quran AI")) })
                FilterChip(selected = !aiMode, enabled = !active, onClick = { aiMode = false },
                    label = { Text(label(language, "Pengenal Android", "Android recognition")) })
            }
        }
        if (aiMode) item {
            QuranModelCard(language, modelState, nativeSupported, modelStore.spec.bytes,
                onDownload = { downloadJob = scope.launch { modelStore.download() } },
                onCancel = { modelStore.cancel(); downloadJob?.cancel() })
        }
        item {
            OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label(language, "Pilih ayat latihan", "Choose a practice verse"), style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(onClick = { menuOpen = true }, enabled = !active, modifier = Modifier.fillMaxWidth()) {
                            Text("$surahId · ${surah.name}", Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                            app.quran.surahs.forEach { choice ->
                                DropdownMenuItem(text = { Text("${choice.id} · ${choice.name}") }, onClick = {
                                    surahId = choice.id; ayahId = 1; menuOpen = false
                                })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { ayahId-- }, enabled = !active && ayahId > 1) { Icon(Icons.Outlined.Remove, label(language, "Ayat sebelumnya", "Previous verse")) }
                        Text(label(language, "Ayat $ayahId / ${surah.ayahCount}", "Verse $ayahId / ${surah.ayahCount}"))
                        IconButton(onClick = { ayahId++ }, enabled = !active && ayahId < surah.ayahCount) { Icon(Icons.Outlined.Add, label(language, "Ayat berikutnya", "Next verse")) }
                    }
                    Text(markedArabic, Modifier.fillMaxWidth().testTag("recitation_target"), fontFamily = QuranArabicFont, fontSize = 29.sp,
                        lineHeight = 58.sp, textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl))
                    if (verse.openingBasmalah != null) Text(label(language,
                        "Latihan ini dimulai dari ayat bernomor; basmalah pembuka tidak masuk pencocokan.",
                        "This exercise starts at the numbered verse; the opening basmalah is excluded from matching."), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { session.cancel(); localSession.cancel(); onRead(surahId, ayahId) }) { Text(label(language, "Buka bacaan & audio ayat", "Open verse reading & audio")) }
                }
            }
        }
        if (!aiMode) item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (localAvailable && !systemConsent) label(language, "Pengenalan di perangkat", "On-device recognition")
                        else label(language, "Penyedia suara Android", "Android speech provider"), style = MaterialTheme.typography.titleMedium)
                    Text(if (localAvailable && !systemConsent) label(language,
                        "Nuraya memakai pengenal suara di perangkat. Model bahasa Arab tetap harus tersedia di layanan suara Anda.",
                        "Nuraya uses on-device recognition. Your speech service must still provide an Arabic language model.")
                        else label(language,
                            "Penyedia suara Android dapat mengirim suara ke servernya. Nuraya tidak menyimpan rekaman atau teks hasil pengenalan. Biaya data internet mungkin berlaku.",
                            "Your Android speech provider may send audio to its servers. Nuraya stores neither recordings nor recognized text. Mobile data charges may apply."),
                        style = MaterialTheme.typography.bodySmall)
                    if (systemAvailable) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = systemConsent, onCheckedChange = { systemConsent = it; session.cancel() }, enabled = !active,
                            modifier = Modifier.testTag("speech_system_consent"))
                        Text(label(language, "Saya mengizinkan penyedia suara Android memproses suara; pemrosesan bisa online.",
                            "I allow the Android speech provider to process my audio; processing may be online."), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!localAvailable && !systemAvailable) Text(label(language,
                        "Pengenal suara belum tersedia. Pasang atau aktifkan layanan pengenalan suara dengan bahasa Arab di pengaturan Android.",
                        "Speech recognition is unavailable. Install or enable an Arabic-capable speech service in Android settings."), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } }) {
                        Text(label(language, "Pengaturan suara Android", "Android voice settings"))
                    }
                }
            }
        }
        item {
            Text(statusText(speech.status, language), modifier = Modifier.testTag("recitation_status"), style = MaterialTheme.typography.titleSmall)
            if (aiMode && (active || localSpeech.elapsedSeconds > 0)) Text(
                label(language, "${localSpeech.elapsedSeconds} / ${if (localSpeech.microphoneCheck) 5 else 90} detik suara masuk",
                    "${localSpeech.elapsedSeconds} / ${if (localSpeech.microphoneCheck) 5 else 90} seconds of audio received"), style = MaterialTheme.typography.bodySmall)
            if (aiMode && active) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { localSpeech.audioLevel },
                    modifier = Modifier.fillMaxWidth().testTag("recitation_audio_level"))
                Text(label(language, when {
                    speech.status == "starting" -> "Menyiapkan mikrofon…"
                    !speech.listening -> "Mikrofon berhenti; rekaman tidak disimpan."
                    !localSpeech.receivedAudio -> "Menunggu suara dari mikrofon…"
                    localSpeech.detectedSpeech -> "Suara masuk terdeteksi."
                    else -> "Suara masih sangat pelan. Dekatkan HP dan baca dengan jelas."
                }, when {
                    speech.status == "starting" -> "Preparing microphone…"
                    !speech.listening -> "Microphone stopped; recordings are not saved."
                    !localSpeech.receivedAudio -> "Waiting for microphone audio…"
                    localSpeech.detectedSpeech -> "Audio input detected."
                    else -> "The input is very quiet. Move closer and read clearly."
                }), style = MaterialTheme.typography.bodySmall)
                if (localSpeech.processingSeconds > 0) Text(label(language,
                    "AI memproses ${localSpeech.processingSeconds} detik · kecepatan bergantung HP",
                    "AI processing for ${localSpeech.processingSeconds} seconds · speed depends on your phone"),
                    style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    if (speech.listening) { if (aiMode) localSession.stopListening() else session.stopListening() }
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                    else { permissionForMicrophoneCheck = false; permission.launch(Manifest.permission.RECORD_AUDIO) }
                }, enabled = !speech.waiting && (speech.listening || if (aiMode) nativeSupported && modelState.phase == ModelPhase.READY
                    else localAvailable || systemAvailable && systemConsent),
                    modifier = Modifier.weight(1f).testTag("recitation_microphone")) {
                    Icon(if (speech.listening) Icons.Outlined.Stop else Icons.Outlined.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text(label(language, if (speech.listening) {
                        if (localSpeech.microphoneCheck && aiMode) "Selesai cek" else "Selesai membaca"
                    } else "Mulai membaca", if (speech.listening) {
                        if (localSpeech.microphoneCheck && aiMode) "Finish check" else "Finish reading"
                    } else "Start reading"))
                }
                if (active) OutlinedButton(onClick = { session.cancel(); localSession.cancel() }) { Text(label(language, "Batal", "Cancel")) }
            }
            if (aiMode) {
                TextButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) checkMicrophone()
                    else { permissionForMicrophoneCheck = true; permission.launch(Manifest.permission.RECORD_AUDIO) }
                }, enabled = !active, modifier = Modifier.testTag("recitation_check_microphone")) {
                    Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(8.dp))
                    Text(label(language, "Cek mikrofon · 5 detik", "Check microphone · 5 seconds"))
                }
                Text(label(language, "Cek suara masuk tanpa menunggu model AI. Ucapkan beberapa kata; audio tidak disimpan.",
                    "Check audio input without waiting for the AI model. Say a few words; audio is not saved."), style = MaterialTheme.typography.bodySmall)
            }
            if (permissionDenied || speech.status == "permission") {
                Text(label(language, "Izin mikrofon diperlukan hanya saat latihan dimulai.", "Microphone permission is required when you start a practice attempt."), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } }) {
                    Text(label(language, "Buka izin aplikasi", "Open app permissions"))
                }
            }
        }
        if (speech.transcript.isNotBlank()) item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label(language, if (speech.finalResult) "Hasil pengenalan" else "Hasil sementara · bisa berubah",
                        if (speech.finalResult) "Recognized text" else "Interim result · may change"), style = MaterialTheme.typography.titleMedium)
                    Text(speech.transcript, Modifier.fillMaxWidth(), fontSize = 23.sp, textAlign = TextAlign.Right,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl))
                    Text(label(language, "${comparison.matchedCount} dari ${comparison.words.size} kata cocok dalam teks hasil pengenalan.",
                        "${comparison.matchedCount} of ${comparison.words.size} words match the recognized text."), style = MaterialTheme.typography.bodyMedium)
                    Text(label(language, "Hijau: teks cocok. Warna kedua: periksa kembali setelah hasil akhir. Tanda dapat berasal dari kesalahan pengenal suara.",
                        "Green: matching text. Secondary highlight: review after the final result. Marks may be caused by speech recognition errors."), style = MaterialTheme.typography.bodySmall)
                    if (speech.finalResult) {
                        val review = comparison.words.filter { it.match != WordMatch.MATCHED }
                        if (review.isNotEmpty()) Text(label(language, "Periksa bersama guru: ", "Review with a teacher: ") + review.joinToString(" · ") { it.text },
                            style = MaterialTheme.typography.bodyMedium)
                        if (comparison.extraWords.isNotEmpty()) Text(label(language, "Kata tambahan yang terdeteksi: ", "Additional detected words: ") + comparison.extraWords.joinToString(" "),
                            style = MaterialTheme.typography.bodySmall)
                        Text(label(language,
                            "Hasil ini tidak menilai panjang mad, dengung, harakat, atau makhraj. Ejaan Utsmani dan tulisan hasil suara bisa berbeda meskipun bacaan benar; huruf muqatta‘ah juga dapat dikenali sebagai nama huruf. Ulangi dengan suara jelas dan periksa bersama guru Al-Qur’an.",
                            "This result does not assess vowel length, nasalization, vowel marks, or articulation. Uthmani spelling can differ from recognized text even for a correct reading; disconnected letters may be transcribed as letter names. Retry clearly and review with a Qur’an teacher."), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Text(label(language, "Progres latihan suara", "Voice practice progress"), style = MaterialTheme.typography.titleMedium)
            Text(label(language, "$attempts percobaan selesai · ${practicedVerses.size} ayat pernah dilatih", "$attempts completed attempts · ${practicedVerses.size} verses practiced"),
                style = MaterialTheme.typography.bodyMedium)
            Text(label(language, "Tersimpan di perangkat ini. Jumlah latihan bukan nilai ketepatan bacaan atau bukti hafal.",
                "Saved on this device. Attempt counts are not correctness scores or proof of memorization."), style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun statusText(status: String, language: String): String = when (status) {
    "model_loading" -> label(language, "Memuat AI di perangkat… mikrofon belum aktif", "Loading on-device AI… microphone is not active yet")
    "local_listening_processing" -> label(language, "Mendengarkan · AI sedang mengenali bacaan", "Listening · AI is recognizing your recitation")
    "local_processing" -> label(language, "Mikrofon berhenti · AI memeriksa hasil akhir…", "Microphone stopped · AI is checking the final result…")
    "listening_model_loading" -> label(language, "Mikrofon aktif · menyiapkan AI sambil merekam", "Microphone active · preparing AI while recording")
    "microphone_check" -> label(language, "Cek mikrofon · ucapkan beberapa kata", "Microphone check · say a few words")
    "microphone_ok" -> label(language, "Mikrofon menerima suara. Anda bisa mulai latihan bacaan.", "The microphone received audio. You can start a recitation attempt.")
    "microphone_quiet" -> label(language, "Mikrofon menerima audio, tetapi suara terlalu pelan atau hening. Dekatkan HP dan coba lagi.", "The microphone received audio, but it was too quiet or silent. Move closer and retry.")
    "audio_no_frames" -> label(language, "Mikrofon tidak mengirim audio. Tutup aplikasi perekam/panggilan lain, lepaskan headset, lalu cek mikrofon lagi.", "The microphone sent no audio. Close other recording/call apps, disconnect the headset, then check again.")
    "audio_muted" -> label(language, "Akses suara dibisukan Android. Aktifkan Akses mikrofon di pengaturan cepat dan tutup aplikasi lain yang memakai mikrofon.", "Android is silencing the microphone. Enable Microphone access in quick settings and close other apps using the microphone.")
    "processing_timeout" -> label(language, "AI terlalu lama memproses. Coba satu ayat pendek dan tutup aplikasi lain, atau gunakan pengenal Android.", "AI processing took too long. Try one short verse and close other apps, or use Android recognition.")
    "model_memory" -> label(language, "Memori HP belum cukup untuk AI. Tutup aplikasi lain atau gunakan pengenal Android.", "Not enough memory for AI. Close other apps or use Android recognition.")
    "model_failed" -> label(language, "AI belum berhasil mengenali. Coba lagi atau gunakan pengenal Android.", "AI recognition did not finish. Retry or use Android recognition.")
    "starting" -> label(language, "Menyiapkan mikrofon…", "Preparing microphone…")
    "listening" -> label(language, "Mendengarkan · baca ayat dengan jelas", "Listening · read the verse clearly")
    "processing" -> label(language, "Menunggu hasil pengenalan…", "Waiting for recognition…")
    "complete" -> label(language, "Percobaan selesai · periksa hasil perkiraan", "Attempt finished · review the estimate")
    "consent" -> label(language, "Izinkan pemrosesan penyedia suara terlebih dahulu.", "Allow speech provider processing first.")
    "unavailable" -> label(language, "Pengenal suara belum tersedia di perangkat ini.", "Speech recognition is unavailable on this device.")
    "arabic_unavailable" -> label(language, "Model bahasa Arab tidak tersedia. Unduh model di pengaturan suara atau izinkan penyedia Android.", "Arabic recognition is unavailable. Download a model in voice settings or allow the Android provider.")
    "permission" -> label(language, "Izin mikrofon belum diberikan.", "Microphone permission is missing.")
    "network" -> label(language, "Koneksi penyedia suara gagal. Coba lagi.", "The speech provider connection failed. Retry.")
    "no_match" -> label(language, "Bacaan belum dikenali. Coba di tempat lebih tenang.", "No speech recognized. Retry in a quieter place.")
    "busy" -> label(language, "Pengenal suara sedang sibuk. Coba lagi.", "Speech recognition is busy. Retry.")
    "timeout" -> label(language, "Sesi berakhir. Ketuk Mulai untuk mencoba lagi.", "Session ended. Tap Start to retry.")
    "audio", "failed" -> label(language, "Pengenalan belum berhasil. Periksa mikrofon atau layanan suara.", "Recognition failed. Check the microphone or speech service.")
    else -> label(language, "Siap · mikrofon aktif setelah Anda menekan Mulai", "Ready · the microphone starts after you tap Start")
}


@Composable
internal fun QuranModelCard(language: String, state: QuranModelState, supported: Boolean, modelBytes: Long,
    onDownload: () -> Unit, onCancel: () -> Unit, unsupportedMessage: String? = null) {
    val downloading = state.phase == ModelPhase.DOWNLOADING
    val checking = state.phase in setOf(ModelPhase.CHECKING, ModelPhase.VERIFYING)
    OutlinedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.testTag("quran_ai_model")) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label(language, "Model AI Quran · digunakan offline", "Quran AI model · on-device recognition"), style = MaterialTheme.typography.titleMedium)
            Text(label(language,
                "Unduh model sekali (sekitar ${modelBytes / 1_000_000} MB). Setelah siap, suara diproses di HP tanpa dikirim ke server. Tidak perlu akun atau biaya layanan. Disarankan memakai Wi-Fi untuk unduhan awal.",
                "Download the model once (about ${modelBytes / 1_000_000} MB). Once ready, audio is processed on your phone without uploading it. No account or service fee is required. Wi-Fi is recommended for the initial download."),
                style = MaterialTheme.typography.bodySmall)
            if (!supported) Text(unsupportedMessage ?: label(language,
                "Mesin AI belum mendukung perangkat ini. Pilih Pengenal Android untuk latihan suara.",
                "This device is not supported by the AI engine. Choose Android recognition for voice practice."), style = MaterialTheme.typography.bodyMedium)
            else when (state.phase) {
                ModelPhase.READY -> {
                    Text(label(language, "AI siap · bisa digunakan tanpa internet", "AI ready · works without internet"), color = MaterialTheme.colorScheme.primary)
                    Text(label(language,
                        "Hasil sementara mulai setelah beberapa detik bacaan dan diperbarui sesuai kecepatan HP. AI dapat salah mengenali atau melengkapi kata; selalu cocokkan dengan teks ayat.",
                        "Interim results start after a few seconds and update at your phone’s speed. AI can misrecognize or fill in words; always check against the verse."), style = MaterialTheme.typography.bodySmall)
                }
                ModelPhase.CHECKING, ModelPhase.VERIFYING -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(label(language, "Memeriksa model AI…", "Checking the AI model…"), style = MaterialTheme.typography.bodySmall)
                }
                ModelPhase.DOWNLOADING -> {
                    LinearProgressIndicator(progress = { (state.downloaded.toFloat() / modelBytes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(label(language, "${state.downloaded / 1_000_000} / ${modelBytes / 1_000_000} MB · mengunduh model",
                        "${state.downloaded / 1_000_000} / ${modelBytes / 1_000_000} MB · downloading model"), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onCancel) { Text(label(language, "Jeda unduhan", "Pause download")) }
                }
                else -> {
                    if (state.phase == ModelPhase.ERROR) Text(modelErrorText(state.error, language), style = MaterialTheme.typography.bodySmall)
                    if (state.phase == ModelPhase.PAUSED) Text(label(language, "Unduhan dijeda. Bagian yang sudah selesai dapat dilanjutkan.",
                        "Download paused. Completed bytes can be resumed."), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onDownload, enabled = !downloading && !checking, modifier = Modifier.testTag("download_quran_ai")) {
                        Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(8.dp))
                        Text(label(language, if (state.downloaded > 0) "Lanjutkan unduhan AI" else "Unduh & aktifkan AI",
                            if (state.downloaded > 0) "Resume AI download" else "Download & enable AI"))
                    }
                }
            }
        }
    }
}

private fun modelErrorText(error: String?, language: String): String = when (error) {
    "space" -> label(language, "Ruang penyimpanan belum cukup. Kosongkan ruang, lalu coba lagi.", "Not enough storage. Free some space, then retry.")
    "checksum", "size" -> label(language, "Berkas model tidak lolos pemeriksaan. Unduh ulang sebelum memakai AI.", "The model failed verification. Download it again before using AI.")
    "storage" -> label(language, "Berkas model belum bisa disimpan. Periksa penyimpanan HP.", "The model could not be saved. Check your phone’s storage.")
    else -> label(language, "Unduhan belum selesai. Periksa internet, lalu lanjutkan unduhan.", "Download did not finish. Check your connection, then resume.")
}
