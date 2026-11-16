package com.nuraya.app.study

import com.nuraya.app.ui.icons.AppIcons as Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    onPracticeRange: ((Int, Int, Int, Int) -> Unit)? = null, initialTab: Int = 1,
    onSessionVisibility: (Boolean)->Unit = {}) {
    val repository=remember(app){PracticeRepository(app,app.quran)}
    val progress by repository.progress.collectAsStateWithLifecycle(PracticeProgress())
    val settings by app.settings.settings.collectAsStateWithLifecycle(UserSettings())
    var course by remember { mutableStateOf<LearningCourse?>(null) }
    var failed by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0,3)) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var sessionVisible by rememberSaveable { mutableStateOf(false) }
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    var replacingId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val tabState=rememberSaveableStateHolder()
    val snackbar=remember { SnackbarHostState() };val scope=rememberCoroutineScope()
    val perform:(suspend ()->Unit)->Unit={ block -> scope.launch {
        busy=true
        try { block() } catch(error:CancellationException){throw error}
        catch(_:Exception){snackbar.showSnackbar(label(language,"Perubahan belum tersimpan. Coba kembali.","The change was not saved. Please retry."))}
        finally {busy=false}
    } }
    LaunchedEffect(app) {
        try{course=withContext(Dispatchers.IO){LearningCourse(app,app.quran)}}
        catch(error:CancellationException){throw error}
        catch(_:Exception){failed=true}
    }
    val visibilityCallback by rememberUpdatedState(onSessionVisibility)
    DisposableEffect(sessionVisible){visibilityCallback(sessionVisible);onDispose{visibilityCallback(false)}}
    fun start(lessonId:String) {
        val content=course?:return
        perform { repository.startLesson(lessonId,content.exercises(lessonId,0).size);previewId=null;replacingId=null;sessionVisible=true }
    }
    Scaffold(snackbarHost={SnackbarHost(snackbar)},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).testTag("practice_screen")) {
            val content=course;val active=progress.learning.active;val result=progress.learning.result
            when {
                content==null -> Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
                    if(failed)Text(label(language,"Materi belum dapat dibuka. Coba buka kembali.","Could not load the lessons. Please reopen this screen.")) else CircularProgressIndicator()
                }
                sessionVisible&&active!=null -> LessonSessionScreen(app,content,active,settings,language,busy,
                    onSubmit={exercise,response->perform{repository.submitLearningAnswer(active.id,active.step,exercise,response)}},
                    onNext={perform{repository.advanceLesson(active.id,active.step)}},onClose={sessionVisible=false})
                sessionVisible&&result!=null -> LessonResultScreen(content,result,progress,language,onClose={sessionVisible=false;perform{repository.dismissLearningResult()}})
                else -> Column(Modifier.fillMaxSize()) {
                    val tabs=listOf(1 to label(language,"Belajar","Learn"),0 to label(language,"Hafalan","Memorize"),2 to label(language,"Latihan","Practice"),3 to label(language,"Progres","Progress"))
                    ScrollableTabRow(selectedTabIndex=tabs.indexOfFirst{it.first==tab},edgePadding=16.dp) {
                        tabs.forEach { (id,title) -> Tab(selected=tab==id,onClick={tab=id},text={Text(title)},modifier=Modifier.testTag("practice_tab_$id")) }
                    }
                    tabState.SaveableStateProvider(tab) {
                        when(tab) {
                            0 -> if(custom)Column(Modifier.fillMaxSize()) {
                                TextButton(onClick={custom=false},modifier=Modifier.padding(horizontal=16.dp)){Icon(Icons.Outlined.ArrowBack,null);Spacer(Modifier.width(8.dp));Text(label(language,"Pilihan surah pendek","Short surah lessons"))}
                                MemorizationTab(app,language,settings,repository,perform,onRead,onPracticeRange)
                            } else LearningPath(content,progress,language,LearningKind.MEMORIZE,onLesson={previewId=it.id},onResume={sessionVisible=true},onCustom={custom=true})
                            1 -> LearningPath(content,progress,language,LearningKind.TAJWID,onLesson={previewId=it.id},onResume={sessionVisible=true})
                            2 -> FreeLearning(content,progress,language,onLesson={previewId=it.id},onMemorize={tab=0;custom=false})
                            else -> LearningProgressScreen(progress,language,onGoal={goal,enabled->perform{repository.setLearningGoal(goal,enabled)}},onRead=onRead)
                        }
                    }
                }
            }
        }
    }
    course?.let { content ->
        previewId?.let { id -> LessonPreview(content.lesson(id),app,settings,language,progress.learning.active?.lessonId==id,
            onClose={previewId=null},onStart={
                if(!busy) {
                    if(progress.learning.active!=null&&progress.learning.active?.lessonId!=id)replacingId=id else start(id)
                }
            }) }
        replacingId?.let { id -> AlertDialog(onDismissRequest={replacingId=null},title={Text(label(language,"Mulai pelajaran lain?","Start another lesson?"))},
            text={Text(label(language,"Sesi yang belum selesai akan diganti. Poin dan pelajaran yang sudah selesai tetap tersimpan.","This replaces the unfinished session. Your points and completed lessons stay saved."))},
            confirmButton={TextButton(onClick={start(id)},enabled=!busy){Text(label(language,"Mulai pelajaran ini","Start this lesson"))}},
            dismissButton={TextButton(onClick={replacingId=null}){Text(label(language,"Kembali","Back"))}}) }
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
