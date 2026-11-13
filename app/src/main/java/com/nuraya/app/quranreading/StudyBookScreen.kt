package com.nuraya.app.quranreading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.study.StudyVerse
import com.nuraya.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Source text and study aids are laid out as reading pages, never painted into a source scan. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyBookScreen(
    app: NurayaApplication, settings: UserSettings, surahId: Int, ayahId: Int, juzId: Int = 0,
    modeId: String, onBack: () -> Unit, onOpenMushaf: (Int, Int) -> Unit,
) {
    val mode = QuranReadingModes.find(modeId)
    val display = QuranReadingModes.settings(mode.id, settings)
    val language = display.language
    val all = remember(surahId, juzId) { if (juzId > 0) app.quran.juz(juzId) else app.quran.verses(surahId) }
    val initial = if (juzId > 0) 0 else all.indexOfFirst { it.ayah == ayahId }.coerceAtLeast(0)
    var position by rememberSaveable(surahId, ayahId, juzId, modeId) { mutableIntStateOf(initial) }
    var controls by rememberSaveable { mutableStateOf(false) }
    var repeats by rememberSaveable { mutableIntStateOf(3) }
    var hidden by rememberSaveable(position, modeId) { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    var rangeDialog by remember { mutableStateOf(false) }
    var rangeStart by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf(initial + 1) }
    var rangeEnd by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf(all.size) }
    var jump by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val pageIndex = remember { MushafImages(app, MushafEdition.MADINAH) }
    val waqaf = remember { WaqafAnnotations(app) }
    val blocks = remember(all, rangeStart, rangeEnd) { memorizationBlocks(all.subList(rangeStart - 1, rangeEnd)) }
    var blockIndex by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf(0) }
    val block = blocks[blockIndex.coerceIn(blocks.indices)]
    val current = if (modeId == "hafalan_8") block.start else all[position.coerceIn(all.indices)]
    val page = pageIndex.pageFor(current.surah, current.ayah)
    val visible = when (modeId) {
        "hafalan_8" -> block.verses
        "madinah_tajwid" -> remember(page) { pageIndex.references.filter { it.page == page }.mapNotNull { app.quran.verse(it.surah, it.ayah) } }
        else -> listOf(current)
    }
    var studies by remember(visible) { mutableStateOf<Map<Int, StudyVerse?>>(emptyMap()) }
    var studyLoading by remember(visible) { mutableStateOf(true) }
    val playback by app.playback.state.collectAsState()
    var previousAudio by remember(surahId, ayahId, juzId) { mutableStateOf(playback) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    LaunchedEffect(visible) {
        scroll.scrollTo(0)
        try {
            studies = withContext(Dispatchers.IO) { visible.associate { it.number to app.study.verse(it.surah, it.ayah) } }
            app.library.save("read_history", "${current.surah}:${current.ayah}", app.quran.surahs[current.surah - 1].name, "Ayat ${current.ayah}", "reading:$modeId")
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { /* Canonical reading remains available if study/history storage fails. */ }
        finally { studyLoading = false }
    }
    LaunchedEffect(playback.surahId, playback.ayahId, playback.isPlaying, settings.followRecitation) {
        if (modeId !in setOf("hafalan_8", "tikrar") && shouldFollowBookAudio(current.surah, previousAudio, playback, settings.followRecitation)) {
            val found = all.indexOfFirst { it.surah == playback.surahId && it.ayah == playback.ayahId }
            if (found >= 0) position = found
        }
        previousAudio = playback
    }
    fun turn(forward: Boolean) {
        hidden = false
        if (modeId == "hafalan_8") blockIndex = (blockIndex + if (forward) 1 else -1).coerceIn(blocks.indices)
        else if (modeId == "madinah_tajwid") {
            val target = (page + if (forward) 1 else -1).coerceIn(1, 604)
            val found = all.indexOfFirst { pageIndex.pageFor(it.surah, it.ayah) == target }
            if (found >= 0) position = found
        } else position = (position + if (forward) 1 else -1).coerceIn(all.indices)
    }
    fun listen() {
        if (playback.isPlaying && playback.verseMode && playback.surahId == current.surah) app.playback.toggle()
        else if (modeId == "hafalan_8" && block.start.surah == block.end.surah)
            app.playback.playVerseRange(block.start.surah, block.start.ayah, block.end.ayah, settings.readerQariId)
        else app.playback.playVerseRange(current.surah, current.ayah, current.ayah, settings.readerQariId, if (modeId == "tikrar") repeats else 1)
    }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    Surface(Modifier.fillMaxSize().testTag("study_book_screen"), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().testTag("study_book_viewport")
            .pointerInput(position, blockIndex, modeId) {
                var drag = 0f
                detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = { if (drag > threshold) turn(true) else if (drag < -threshold) turn(false) },
                    onHorizontalDrag = { change, amount -> change.consume(); drag += amount })
            }.pointerInput(Unit) { detectTapGestures(onTap = { controls = true }) }
            .verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.fillMaxWidth().testTag(if (studyLoading) "study_loading" else "study_ready"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(app.quran.surahs[current.surah - 1].name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("study_surah"))
                Text(mode.title(language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("reader_mode"))
                Text(if (modeId == "madinah_tajwid") label(language, "Halaman $page · teks tajwid", "Page $page · tajwid text")
                    else if (modeId == "hafalan_8") "Blok ${block.number} / ${blocks.size} · ${block.reference}"
                    else "${current.surah}:${current.ayah}", modifier = Modifier.testTag("study_reference"), style = MaterialTheme.typography.labelMedium)
            }
            if (modeId == "hafalan_8") {
                Text(label(language, "Kelompok latihan ini dibagi dari rentang pilihan Anda, berbeda dari blok warna pada halaman cetak Hafalan.",
                    "These practice groups divide your selected range; they differ from the colored blocks on printed Hafalan pages."),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("manual_memorization_groups"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { blocks.forEach { choice ->
                    FilterChip(selected = block.number == choice.number, onClick = { blockIndex = choice.number - 1; hidden = false },
                        label = { Text("${choice.number}") }, modifier = Modifier.testTag("memorization_block_${choice.number}"))
                } }
                OutlinedButton(onClick = { hidden = !hidden }, modifier = Modifier.fillMaxWidth().testTag("recall_toggle")) {
                    Text(label(language, if (hidden) "Tampilkan bacaan" else "Uji hafalan", if (hidden) "Reveal reading" else "Test recall"))
                }
            }
            if (modeId == "tikrar") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 7, 10, 20).forEach { n -> FilterChip(selected = repeats == n, onClick = { repeats = n }, label = { Text("$n ×") }, modifier = Modifier.testTag("tikrar_target_$n")) }
            }
            if (hidden) Text(label(language, "Baca dari ingatan, lalu tampilkan untuk memeriksa.", "Recite from memory, then reveal to check."), modifier = Modifier.testTag("recall_hidden"))
            else visible.forEachIndexed { index, verse ->
                if (modeId == "madinah_tajwid" && (index == 0 || visible[index - 1].surah != verse.surah)) {
                    Text(app.quran.surahs[verse.surah - 1].name, Modifier.fillMaxWidth().testTag("page_surah_${verse.surah}"),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                }
                val study = studies[verse.number]
                val word = if (playback.verseMode && playback.isPlaying && playback.surahId == verse.surah && playback.ayahId == verse.ayah) playback.wordPosition else 0
                val arabic = annotatedQuranText(verse.arabic, study, display.tajwidColors, display.darkMode, word)
                verse.openingBasmalah?.let { Text(it, Modifier.fillMaxWidth().testTag("basmalah_text_${verse.surah}"), fontFamily = QuranArabicFont, fontSize = 28.sp, lineHeight = 52.sp, textAlign = TextAlign.Center) }
                Column(Modifier.fillMaxWidth().testTag("study_verse_${verse.surah}_${verse.ayah}"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (modeId == "waqaf") annotatedWaqafText(arabic, waqaf.forVerse(verse)) else arabic,
                        Modifier.fillMaxWidth().testTag("verse_text_${verse.surah}_${verse.ayah}"), fontFamily = QuranArabicFont,
                        fontSize = display.arabicSize.sp, textAlign = TextAlign.Right,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, lineHeight = (display.arabicSize * 1.8f).sp, letterSpacing = 0.sp, platformStyle = PlatformTextStyle(includeFontPadding = true)))
                    if (visible.size > 1) Text("${verse.surah}:${verse.ayah}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (modeId == "word_tajwid") {
                        if (study == null) Text(label(language, if (studyLoading) "Memuat arti per kata…" else "Arti per kata belum dapat dibuka.", if (studyLoading) "Loading word meanings…" else "Word meanings are unavailable."))
                        else StudyVersePanel(study, language, initiallyShowWords = true, tajwidColors = display.tajwidColors, dark = display.darkMode)
                    }
                    if (modeId == "latin_tajwid") {
                        var latin by remember(verse.number) { mutableStateOf<String?>(null) }
                        LaunchedEffect(verse.number) { latin = withContext(Dispatchers.IO) { LatinRepository.shared(app).verse(verse.surah, verse.ayah)?.latin } }
                        latin?.let { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("latin_${verse.surah}_${verse.ayah}")) }
                    }
                    if (display.translation != "none" && modeId !in setOf("hafalan_8", "word_tajwid", "waqaf", "latin_tajwid")) {
                        Text(if (display.translation == "en") verse.english else verse.indonesian, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("translation_${verse.surah}_${verse.ayah}"))
                        Text(if (display.translation == "en") "Marmaduke Pickthall (1930)" else "Kementerian Agama RI · QuranEnc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (modeId == "waqaf") WaqafVerseGuide(verse, language, waqaf.forVerse(verse), waqaf.verified(verse))
                    if (modeId == "thematic" && !study?.tafsir.isNullOrBlank()) {
                        HorizontalDivider()
                        Text(study?.tafsirSource.orEmpty(), style = MaterialTheme.typography.titleSmall)
                        Text(study?.tafsir.orEmpty(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("thematic_tafsir"))
                    }
                    if (modeId == "tikrar") TikrarCounter(verse, language, repeats)
                }
            }
            if (modeId in setOf("hafalan_8", "tikrar")) Button(onClick = { listen() }, modifier = Modifier.fillMaxWidth().testTag("study_listen")) {
                Text(label(language, if (modeId == "tikrar") "Dengarkan $repeats kali" else "Dengarkan blok ini", if (modeId == "tikrar") "Listen $repeats times" else "Listen to this block"))
            }
            if (modeId == "waqaf") OutlinedButton(onClick = { guide = true }, modifier = Modifier.fillMaxWidth().testTag("waqaf_guide")) { Text(label(language, "Panduan waqaf & ibtida", "Stopping & resuming guide")) }
            TextButton(onClick = { controls = true }, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("study_open_menu")) { Text(label(language, "Menu bacaan", "Reading menu")) }
        }
    }
    if (controls) ModalBottomSheet(onDismissRequest = { controls = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(20.dp).testTag("study_controls"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(mode.title(language), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { controls = false }) { Text(label(language, "Tutup", "Close")) }
            }
            OutlinedButton(onClick = { controls = false; onBack() }, modifier = Modifier.fillMaxWidth().testTag("back_button")) { Text(label(language, "Kembali ke daftar", "Back to list")) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { turn(false); controls = false }, modifier = Modifier.weight(1f).testTag("study_previous")) { Text(label(language, "Sebelumnya", "Previous")) }
                OutlinedButton(onClick = { turn(true); controls = false }, modifier = Modifier.weight(1f).testTag("study_next")) { Text(label(language, "Berikutnya", "Next")) }
            }
            Button(onClick = { listen() }, modifier = Modifier.fillMaxWidth().testTag("study_play")) { Text(label(language, if (playback.isPlaying) "Jeda audio" else "Dengarkan ayat", if (playback.isPlaying) "Pause" else "Listen")) }
            playback.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(label(language, "Qari", "Reciter"), style = MaterialTheme.typography.titleMedium)
            app.playback.verseReciters.forEach { qari ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.readerQariId == qari.id, onClick = { scope.launch { app.settings.setReaderQari(qari.id) } })
                    QariPortrait(qari.id, qari.name, language, Modifier.size(48.dp))
                    Text(qari.name, Modifier.weight(1f).padding(start = 12.dp))
                }
            }
            TextButton(onClick = { controls = false; jump = true }) { Text(label(language, "Ke ayat", "Go to verse")) }
            if (modeId == "hafalan_8") TextButton(onClick = { controls = false; rangeDialog = true }, modifier = Modifier.testTag("memorization_range")) { Text(label(language, "Pilih rentang ayat", "Choose verse range")) }
            TextButton(onClick = { scope.launch { app.library.toggle("bookmark", "${current.surah}:${current.ayah}", app.quran.surahs[current.surah - 1].name, "Ayat ${current.ayah}") } }) { Text(label(language, "Tandai ayat", "Bookmark verse")) }
            TextButton(onClick = { controls = false; showDetails = true }) { Text(label(language, "Arti, tajwid & tafsir", "Meaning, tajwid & tafsir")) }
            TextButton(onClick = { controls = false; onOpenMushaf(current.surah, current.ayah) }, modifier = Modifier.testTag("open_book_mushaf")) { Text(label(language, "Lihat gambar mushaf", "View source mushaf image")) }
        }
    }
    if (showDetails) ModalBottomSheet(onDismissRequest = { showDetails = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${current.surah}:${current.ayah}", style = MaterialTheme.typography.titleLarge)
            Text(current.indonesian)
            StudyVersePanel(studies[current.number], language, initiallyShowWords = true, tajwidColors = display.tajwidColors, dark = display.darkMode)
            TajwidLegend(language, display.darkMode)
            TextButton(onClick = { showDetails = false }) { Text(label(language, "Tutup", "Close")) }
        }
    }
    if (guide) WaqafLearningGuide(language) { guide = false }
    if (rangeDialog) MemorizationRangeDialog(language, all, juzId > 0, rangeStart, rangeEnd, { rangeDialog = false }) { start, end ->
        rangeStart = start; rangeEnd = end; blockIndex = 0; hidden = false; rangeDialog = false
    }
    if (jump) AlertDialog(onDismissRequest = { jump = false }, title = { Text(label(language, "Ke ayat", "Go to verse")) }, text = {
        OutlinedTextField(jumpInput, { jumpInput = it.filter(Char::isDigit).take(3) }, label = { Text("1–${app.quran.surahs[current.surah - 1].ayahCount}") }, singleLine = true, modifier = Modifier.testTag("study_jump"))
    }, confirmButton = { TextButton(enabled = all.any { it.surah == current.surah && it.ayah == jumpInput.toIntOrNull() }, onClick = {
        position = all.indexOfFirst { it.surah == current.surah && it.ayah == jumpInput.toIntOrNull() }.coerceAtLeast(0)
        if (modeId == "hafalan_8") { rangeStart = position + 1; rangeEnd = all.size; blockIndex = 0; hidden = false }
        jump = false
    }) { Text(label(language, "Buka", "Open")) } })
}
