package com.nuraya.app.study

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.QuranArabicFont
import com.nuraya.app.ui.TajwidPalette
import com.nuraya.app.ui.annotatedQuranText
import com.nuraya.app.ui.label
import com.nuraya.app.profile.rememberProfile
import com.nuraya.app.profile.ProfileAvatar
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(app: NurayaApplication, language: String, onRead: (Int, Int) -> Unit,
    onPracticeRange: ((Int, Int, Int, Int) -> Unit)? = null, initialTab: Int = 0) {
    val repository = remember(app) { PracticeRepository(app, app.quran) }
    val progress by repository.progress.collectAsStateWithLifecycle(PracticeProgress())
    val settings by app.settings.settings.collectAsStateWithLifecycle(UserSettings())
    var content by remember { mutableStateOf<PracticeContent?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0,3)) }
    val tabState = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val perform: (suspend () -> Unit) -> Unit = { block -> scope.launch {
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { snackbar.showSnackbar(label(language, "Perubahan belum tersimpan. Coba kembali.", "The change was not saved. Please retry.")) }
    } }
    LaunchedEffect(app) {
        try { content = withContext(Dispatchers.IO) { PracticeContent(app, app.quran) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { loadFailed = true }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).testTag("practice_screen")) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                listOf(label(language, "Hafalan", "Memorize"), "Tajwid", label(language, "Kuis", "Quiz"), label(language, "Progres", "Progress"))
                    .forEachIndexed { index, title ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) }, modifier = Modifier.testTag("practice_tab_$index"))
                    }
            }
            tabState.SaveableStateProvider(tab) { when (tab) {
                0 -> MemorizationTab(app, language, settings, repository, perform, onRead, onPracticeRange)
                1 -> if (content != null) TajwidTab(app, language, settings, content!!, progress, repository, perform, onRead, onPracticeRange)
                    else PracticeLoading(language, loadFailed)
                2 -> if (content != null) QuizTab(language, content!!, repository, perform)
                    else PracticeLoading(language, loadFailed)
                else -> ProgressTab(app, language, progress, repository, perform, onRead)
            } }
        }
    }
}

@Composable
private fun PracticeLoading(language: String, failed: Boolean) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (failed) Text(label(language, "Materi belajar belum dapat dibuka. Hafalan dan progres tetap tersedia.", "Learning material could not be opened. Memorization and progress remain available."))
        else CircularProgressIndicator()
    }
}

@Composable
private fun MemorizationTab(app: NurayaApplication, language: String, settings: UserSettings,
    repository: PracticeRepository, perform: (suspend () -> Unit) -> Unit, onRead: (Int, Int) -> Unit,
    onPracticeRange: ((Int, Int, Int, Int) -> Unit)?) {
    var surahId by rememberSaveable { mutableIntStateOf(1) }
    var startInput by rememberSaveable { mutableStateOf("1") }
    var endInput by rememberSaveable { mutableStateOf("3") }
    var currentAyah by rememberSaveable { mutableIntStateOf(1) }
    var repeats by rememberSaveable { mutableIntStateOf(3) }
    var wholeRange by rememberSaveable { mutableStateOf(false) }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val surah = app.quran.surahs[surahId - 1]
    val start = startInput.toIntOrNull()
    val end = endInput.toIntOrNull()
    val valid = start != null && end != null && start in 1..surah.ayahCount && end in start..surah.ayahCount
    val verse = app.quran.verse(surahId, currentAyah.coerceIn(1, surah.ayahCount))!!
    val study by produceState<StudyVerse?>(null, surahId, currentAyah) {
        value = withContext(Dispatchers.IO) { app.study.verse(surahId, currentAyah) }
    }
    val reset: () -> Unit = { currentAyah = start?.coerceIn(1, surah.ayahCount) ?: 1; reveal = false; finished = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(label(language, "Sedikit demi sedikit, dengan pengulangan.", "Build memory through repetition."), style = MaterialTheme.typography.headlineSmall)
            Text(label(language, "Pilih rentang, dengarkan contoh, lalu sembunyikan teks dan coba dari ingatan. Penilaian di sini adalah catatan Anda sendiri.",
                "Choose a range, listen, hide the text and try from memory. Ratings here are your own self-assessment."), style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("$surahId · ${surah.name}", Modifier.weight(1f)); Icon(Icons.Outlined.ExpandMore, null)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                            app.quran.surahs.forEach { choice ->
                                DropdownMenuItem(text = { Text("${choice.id} · ${choice.name}") }, onClick = {
                                    surahId = choice.id; startInput = "1"; endInput = minOf(3, choice.ayahCount).toString()
                                    currentAyah = 1; reveal = false; finished = false; menu = false
                                })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(startInput, { value -> startInput = value.filter(Char::isDigit).take(3); currentAyah = startInput.toIntOrNull()?.coerceIn(1, surah.ayahCount) ?: 1; reveal = false; finished = false },
                            label = { Text(label(language, "Dari ayat", "First verse")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, enabled = !busy, isError = !valid, modifier = Modifier.weight(1f).testTag("practice_start"))
                        OutlinedTextField(endInput, { value -> endInput = value.filter(Char::isDigit).take(3); reset() },
                            label = { Text(label(language, "Sampai ayat", "Last verse")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, enabled = !busy, isError = !valid, modifier = Modifier.weight(1f).testTag("practice_end"))
                    }
                    if (!valid) Text(label(language, "Gunakan rentang berurutan antara 1 dan ${surah.ayahCount}.", "Use an ordered range between 1 and ${surah.ayahCount}."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label(language, "Jumlah pengulangan", "Repeat count"), Modifier.weight(1f))
                        IconButton(onClick = { repeats-- }, enabled = repeats > 1) { Icon(Icons.Outlined.Remove, label(language, "Kurangi", "Decrease")) }
                        Text("${repeats}×")
                        IconButton(onClick = { repeats++ }, enabled = repeats < 20) { Icon(Icons.Outlined.Add, label(language, "Tambah", "Increase")) }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=!wholeRange,onClick={wholeRange=false},label={Text(label(language,"Per ayat","Each verse"))},modifier=Modifier.testTag("repeat_each_mode"))
                        FilterChip(selected=wholeRange,onClick={wholeRange=true},label={Text(label(language,"Seluruh rentang","Whole range"))},modifier=Modifier.testTag("repeat_range_mode"))
                    }
                    Text(label(language,if(wholeRange)"Ayat awal hingga akhir diputar, lalu rentang diulang. Berhenti setelah $repeats putaran." else "Setiap ayat diulang $repeats kali sebelum lanjut ke ayat berikutnya.",if(wholeRange)"Play the full range, repeat, then stop after $repeats passes." else "Repeat each verse $repeats times before moving on."),style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={startInput="1";endInput=surah.ayahCount.toString();reset()}){Text(label(language,"Pilih satu surah penuh","Choose the entire surah"))}
                    if (onPracticeRange != null) Button(onClick = {
                        if(wholeRange){app.playback.playVerseRange(surahId,start!!,end!!,settings.readerQariId,rangeRepeats=repeats);onRead(surahId,start)}
                        else onPracticeRange(surahId, start!!, end!!, repeats)
                    }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Headphones, null); Spacer(Modifier.width(8.dp)); Text(label(language, "Dengarkan rentang", "Listen to range"))
                    }
                }
            }
        }
        if (valid) item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${surah.name} · $surahId:${verse.ayah}", style = MaterialTheme.typography.titleMedium)
                    if (finished) {
                        Text(label(language, "Rentang selesai. Catatan latihan sudah disimpan.", "Range complete. Your practice record is saved."), modifier = Modifier.testTag("practice_finished"))
                        OutlinedButton(onClick = reset) { Text(label(language, "Latih lagi", "Practice again")) }
                    } else {
                        if (reveal) {
                            Text(annotatedQuranText(verse.arabic, study, settings.tajwidColors, settings.darkMode), Modifier.fillMaxWidth().testTag("memorization_text"),
                                fontFamily = QuranArabicFont, fontSize = settings.arabicSize.sp, lineHeight = (settings.arabicSize * 2).sp,
                                textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl))
                            Text(if (language == "id") verse.indonesian else verse.english, style = MaterialTheme.typography.bodyMedium)
                        } else Text(label(language, "Teks disembunyikan. Bacakan ayat ini dari ingatan, lalu periksa.", "Text hidden. Recite this verse from memory, then check."), Modifier.padding(vertical = 24.dp).testTag("memorization_hidden"))
                        OutlinedButton(onClick = { reveal = !reveal }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("memorization_reveal")) {
                            Icon(if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null)
                            Spacer(Modifier.width(8.dp)); Text(label(language, if (reveal) "Sembunyikan teks" else "Tampilkan & periksa", if (reveal) "Hide text" else "Reveal & check"))
                        }
                        Text(label(language, "Setelah memeriksa, simpan penilaian untuk ayat ini:", "After checking, save your assessment for this verse:"), style = MaterialTheme.typography.bodySmall)
                        listOf(RecallRating.AGAIN to label(language, "Perlu mengulang", "Need another attempt"),
                            RecallRating.ASSISTED to label(language, "Masih memakai bantuan", "Needed a prompt"),
                            RecallRating.CONFIDENT to label(language, "Lancar menurut saya", "Confident on my own")).forEach { (rating, title) ->
                            OutlinedButton(onClick = {
                                busy = true
                                perform {
                                    try {
                                        repository.reviewVerse(surahId, verse.ayah, rating, finishSession = currentAyah == end)
                                        if (currentAyah < end!!) { currentAyah++; reveal = false }
                                        else { finished = true }
                                    } finally { busy = false }
                                }
                            }, enabled = reveal && !busy, modifier = Modifier.fillMaxWidth().testTag("memorization_rate_${rating.name}")) { Text(title) }
                        }
                    }
                    TextButton(onClick = { onRead(surahId, verse.ayah) }) { Text(label(language, "Buka ayat di pembaca", "Open verse in reader")) }
                }
            }
        }
    }
}

@Composable
private fun TajwidTab(app: NurayaApplication, language: String, settings: UserSettings, content: PracticeContent,
    progress: PracticeProgress, repository: PracticeRepository, perform: (suspend () -> Unit) -> Unit,
    onRead: (Int, Int) -> Unit, onPracticeRange: ((Int, Int, Int, Int) -> Unit)?) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(label(language, "Kenali hukum, dengarkan contohnya.", "Recognize the rule, hear its example."), style = MaterialTheme.typography.headlineSmall)
            Text(label(language, "Warna adalah bantuan mengenali hukum. Pelajari cara melafalkan dan ukurannya bersama guru; penjelasan berikut adalah pengantar riwayat Hafs.",
                "Colors help identify rules. Learn their pronunciation and duration with a teacher; these are introductory Hafs lessons."), style = MaterialTheme.typography.bodyMedium)
        }
        items(content.lessons, key = { it.id }) { lesson ->
            var open by rememberSaveable(lesson.id) { mutableStateOf(false) }
            val example by produceState(AnnotatedString(lesson.excerpt), lesson.id, settings.tajwidColors, settings.darkMode) {
                val study = withContext(Dispatchers.IO) { app.study.verse(lesson.surah, lesson.ayah) }
                val start = study?.arabic?.indexOf(lesson.excerpt) ?: -1
                value = if (study != null && start >= 0) annotatedQuranText(study.arabic, study, settings.tajwidColors, settings.darkMode)
                    .subSequence(start, start + lesson.excerpt.length) else AnnotatedString(lesson.excerpt)
            }
            OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("●", color = TajwidPalette.color(lesson.rule, settings.darkMode) ?: MaterialTheme.colorScheme.primary)
                        Text(if (language == "id") lesson.titleId else lesson.titleEn, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.titleMedium)
                        if (lesson.id in progress.completedLessons) Icon(Icons.Outlined.CheckCircle, label(language, "Telah dibaca", "Read"), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(if (language == "id") lesson.explanationId else lesson.explanationEn, style = MaterialTheme.typography.bodyMedium)
                    Text(example, Modifier.fillMaxWidth(), fontFamily = QuranArabicFont, fontSize = 29.sp, lineHeight = 58.sp,
                        textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl))
                    Text("${app.quran.surahs[lesson.surah - 1].name} · ${lesson.surah}:${lesson.ayah}", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { onRead(lesson.surah, lesson.ayah) }) { Text(label(language, "Lihat ayat & warna rinci", "View verse & detailed colors")) }
                    if (onPracticeRange != null) OutlinedButton(onClick = { onPracticeRange(lesson.surah, lesson.ayah, lesson.ayah, 3) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Headphones, null); Spacer(Modifier.width(8.dp)); Text(label(language, "Dengarkan contoh 3×", "Listen to example 3×"))
                    }
                    TextButton(onClick = { open = !open }) { Text(label(language, "Sumber pelajaran", "Lesson source")) }
                    if (open) {
                        Text(lesson.sourceTitle, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lesson.sourceUrl))) } }) {
                            Text(label(language, "Buka sumber", "Open source")); Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                        }
                    }
                    if (lesson.id !in progress.completedLessons) OutlinedButton(onClick = { perform { repository.completeLesson(lesson.id) } }) {
                        Text(label(language, "Tandai sudah dibaca", "Mark as read"))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizTab(language: String, content: PracticeContent, repository: PracticeRepository, perform: (suspend () -> Unit) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var choice by rememberSaveable { mutableIntStateOf(-1) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    val question = content.questions.getOrNull(index)
    val profile=rememberProfile()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Surface(color=MaterialTheme.colorScheme.primary,shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    ProfileAvatar(profile.avatar,Modifier.size(72.dp))
                    Column { Text(profile.nickname,color=MaterialTheme.colorScheme.onPrimary,style=MaterialTheme.typography.titleLarge)
                        Text(label(language,"Kuis pribadi · $score benar","Personal quiz · $score correct"),color=MaterialTheme.colorScheme.onPrimary,style=MaterialTheme.typography.bodyMedium) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(label(language, "Uji pemahaman tajwid", "Check your tajwid knowledge"), style = MaterialTheme.typography.headlineSmall)
            Text(label(language, "Kuis pengetahuan, bukan pengujian kualitas suara. Baca alasan jawaban untuk memperbaiki pemahaman.",
                "A knowledge quiz, not a voice assessment. Read each explanation to improve your understanding."), style = MaterialTheme.typography.bodyMedium)
        }
        if (question == null) item {
            Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Outlined.EmojiEvents,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.secondary)
                    Text(label(language,"Latihan selesai!","Practice complete!"),style=MaterialTheme.typography.headlineSmall)
                    ProfileAvatar(profile.avatar,Modifier.size(96.dp))
                    Text(label(language, "Selesai: $score dari ${content.questions.size} jawaban benar.", "Complete: $score of ${content.questions.size} answers correct."), style = MaterialTheme.typography.titleLarge,textAlign=TextAlign.Center)
                    Text(label(language,"Terus belajar dengan ritmemu sendiri.","Keep learning at your own pace."),textAlign=TextAlign.Center)
                }
            }
            Button(onClick = { index = 0; choice = -1; locked = false; score = 0 }) { Text(label(language, "Ulangi kuis", "Restart quiz")) }
        } else {
            item {
                LinearProgressIndicator(progress = { index.toFloat() / content.questions.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Text("${index + 1} / ${content.questions.size}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                Surface(shape=RoundedCornerShape(26.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth()) {
                    Text(if (language == "id") question.questionId else question.questionEn, style = MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(24.dp))
                }
            }
            items((if (language == "id") question.optionsId else question.optionsEn).withIndex().toList(), key = { it.index }) { option ->
                OutlinedCard(onClick = { choice = option.index }, enabled = !locked && !busy, modifier = Modifier.fillMaxWidth().testTag("quiz_option_${option.index}")) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choice == option.index, onClick = null, enabled = !locked && !busy)
                        Text(option.value, Modifier.padding(start = 10.dp))
                    }
                }
            }
            item {
                if (!locked) Button(onClick = {
                    busy = true
                    perform {
                        try { repository.recordQuizAnswer(choice == question.correctIndex); if (choice == question.correctIndex) score++; locked = true }
                        finally { busy = false }
                    }
                }, enabled = choice >= 0 && !busy, modifier = Modifier.fillMaxWidth().testTag("quiz_check")) { Text(label(language, "Periksa jawaban", "Check answer")) }
                else {
                    Text(if (choice == question.correctIndex) label(language, "Jawaban benar", "Correct answer") else label(language, "Belum tepat", "Not quite"), style = MaterialTheme.typography.titleMedium)
                    Text(label(language, "Jawaban: ", "Answer: ") + (if (language == "id") question.optionsId else question.optionsEn)[question.correctIndex], style = MaterialTheme.typography.bodyMedium)
                    Text(if (language == "id") question.explanationId else question.explanationEn, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { index++; choice = -1; locked = false }, modifier = Modifier.fillMaxWidth()) { Text(label(language, "Lanjut", "Continue")) }
                }
            }
        }
    }
}

@Composable
private fun ProgressTab(app: NurayaApplication, language: String, progress: PracticeProgress,
    repository: PracticeRepository, perform: (suspend () -> Unit) -> Unit, onRead: (Int, Int) -> Unit) {
    val today = LocalDate.now()
    val count = progress.countOn(today)
    val due = progress.dueOn(today)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(label(language, "Perjalanan belajar Anda", "Your learning journey"), style = MaterialTheme.typography.headlineSmall)
            Text(label(language, "Catatan pribadi di perangkat ini. Jumlah latihan dan penilaian mandiri bukan sertifikasi hafalan.",
                "A personal record on this device. Practice counts and self-ratings are not memorization certification."), style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label(language, "$count / ${progress.dailyGoal} penilaian ayat hari ini", "$count / ${progress.dailyGoal} verse reviews today"), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { (count.toFloat() / progress.dailyGoal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(label(language, "${progress.streakOn(today)} hari beruntun · ${progress.reviews.size} ayat pernah dilatih", "${progress.streakOn(today)} day streak · ${progress.reviews.size} verses practiced"))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label(language, "Target harian", "Daily goal"), Modifier.weight(1f))
                        IconButton(onClick = { perform { repository.setDailyGoal(progress.dailyGoal - 1) } }, enabled = progress.dailyGoal > 1) { Icon(Icons.Outlined.Remove, label(language, "Kurangi target", "Decrease goal")) }
                        Text(progress.dailyGoal.toString())
                        IconButton(onClick = { perform { repository.setDailyGoal(progress.dailyGoal + 1) } }, enabled = progress.dailyGoal < 50) { Icon(Icons.Outlined.Add, label(language, "Tambah target", "Increase goal")) }
                    }
                }
            }
        }
        item {
            Text(label(language, "${progress.completedSessions} sesi hafalan selesai · ${progress.totalReviews} penilaian ayat", "${progress.completedSessions} memorization sessions completed · ${progress.totalReviews} verse reviews"))
            Text(label(language, "${progress.completedLessons.size} pelajaran ditandai dibaca", "${progress.completedLessons.size} lessons marked as read"))
            Text(label(language, "Kuis: ${progress.quizCorrect} benar dari ${progress.quizAnswered} jawaban tersimpan", "Quiz: ${progress.quizCorrect} correct of ${progress.quizAnswered} saved answers"))
        }
        item { Text(label(language, "Saatnya diulang (${due.size})", "Due for review (${due.size})"), style = MaterialTheme.typography.titleLarge) }
        if (due.isEmpty()) item { Text(label(language, "Belum ada jadwal ulang hari ini. Catat latihan di tab Hafalan untuk memulai.", "No reviews are due today. Record practice in Memorize to begin.")) }
        items(due, key = { it.reference }) { review ->
            OutlinedCard(onClick = { onRead(review.surah, review.ayah) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${app.quran.surahs[review.surah - 1].name} · ${review.reference}", style = MaterialTheme.typography.titleMedium)
                        Text(label(language, "${review.reviews} latihan · jadwal ${review.nextReviewOn}", "${review.reviews} reviews · due ${review.nextReviewOn}"), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.MenuBook, label(language, "Buka ayat", "Open verse"))
                }
            }
        }
    }
}
