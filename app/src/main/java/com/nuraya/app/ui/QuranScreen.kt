package com.nuraya.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Surah
import com.nuraya.app.data.Verse
import com.nuraya.app.storage.UserEntry
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.study.StudyVerse
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.quranreading.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuranScreen(repo: QuranRepository, language: String, onRead: (Int, Int) -> Unit, onJuz: (Int) -> Unit,
    readingModeId: String = "standard", onChooseMode: (() -> Unit)? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val normalized = remember(query) { QuranRepository.normalizeSearch(query.trim()) }
    val surahs = remember(normalized) {
        repo.surahs.filter {
            normalized.isEmpty() || it.id.toString() == normalized ||
                QuranRepository.normalizeSearch("${it.name} ${it.arabicName} ${it.englishName}").contains(normalized)
        }
    }
    var results by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        searching = query.isNotBlank()
        results = emptyList()
        if (query.isNotBlank()) {
            delay(200)
            results = withContext(Dispatchers.Default) { repo.search(query) }
        }
        searching = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(QuranReadingModes.find(readingModeId).title(language), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("surah_reading_mode"))
            if (onChooseMode != null) TextButton(onClick = onChooseMode, modifier = Modifier.testTag("change_reading_mode")) {
                Text(label(language, "Ganti cara membaca", "Change reading mode"))
            }
            Spacer(Modifier.height(6.dp))
            Text(if (readingModeId in setOf("madinah", "madinah_tajwid"))
                label(language, "Pilih surah atau juz untuk membuka halaman mushaf. Halaman baru memerlukan internet.", "Choose a surah or juz to open its mushaf page. New pages need internet.")
                else label(language, "114 surah · 30 juz · tersedia offline", "114 surahs · 30 juz · available offline"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = query, onValueChange = { query = it.take(200) }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, label(language,"Hapus pencarian","Clear search")) } },
                placeholder = { Text(label(language, "Cari surah, ayat, atau 2:255", "Search surah, verse, or 2:255")) },
                modifier = Modifier.fillMaxWidth().testTag("quran_search"), shape = RoundedCornerShape(18.dp))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("Surah") })
                FilterChip(selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("Juz") })
            }
        }
        if (query.isNotBlank()) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (surahs.isNotEmpty()) {
                    item { Text(label(language,"Surah","Surahs"), style = MaterialTheme.typography.titleMedium) }
                    items(surahs, key = { "surah_${it.id}" }) { surah -> SurahRow(surah, language) { onRead(surah.id, 1) } }
                }
                item {
                    Text(if (searching) label(language,"Mencari ayat…","Searching verses…") else label(language,"${results.size} ayat ditemukan","${results.size} verses found"),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                if (searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (!searching && results.isEmpty() && surahs.isEmpty()) item {
                    Text(label(language,"Belum ada hasil. Coba kata lain atau nomor seperti 2:255.","No results yet. Try another word or a reference such as 2:255."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(results.take(100), key = { "verse_${it.number}" }) { verse ->
                    OutlinedCard(onClick = { onRead(verse.surah, verse.ayah) }, modifier = Modifier.testTag("search_verse_${verse.surah}_${verse.ayah}"), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${repo.surahs[verse.surah - 1].name} · ${verse.surah}:${verse.ayah}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(verse.arabic, Modifier.fillMaxWidth(), fontFamily = QuranArabicFont, fontSize = 24.sp,
                                textAlign = TextAlign.Right, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl, lineHeight = 42.sp))
                            Spacer(Modifier.height(6.dp))
                            Text(if (language == "en") verse.english else verse.indonesian, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (results.size > 100) item { Text(label(language,"Menampilkan 100 hasil pertama. Perjelas pencarian untuk hasil lainnya.","Showing the first 100 results. Refine your search to find more."), style = MaterialTheme.typography.bodySmall) }
            }
        } else if (selectedTab == 0) {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repo.surahs, key = Surah::id) { surah -> SurahRow(surah, language) { onRead(surah.id, 1) } }
                item { Text(label(language,"Teks Arab: Tanzil · Terjemahan: Kemenag RI / Pickthall", "Arabic: Tanzil · Translations: Kemenag RI / Pickthall"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp)) }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items((1..30).toList(), key = { it }) { number ->
                    val verses = remember(number) { repo.juz(number) }
                    val first = verses.firstOrNull()
                    val last = verses.lastOrNull()
                    OutlinedCard(onClick = { onJuz(number) }, modifier = Modifier.fillMaxWidth().testTag("juz_$number"), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberTile(number)
                            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                                Text("Juz $number", style = MaterialTheme.typography.titleMedium)
                                if (first != null && last != null) Text("${repo.surahs[first.surah - 1].name} ${first.ayah} – ${repo.surahs[last.surah - 1].name} ${last.ayah}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.ChevronRight, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, language: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("surah_${surah.id}"), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberTile(surah.id)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(surah.name, style = MaterialTheme.typography.titleMedium)
                val revelation = if (surah.revelation == "meccan") label(language,"Makkiyah","Meccan") else label(language,"Madaniyah","Medinan")
                Text("${surah.ayahCount} ${label(language,"ayat","verses")} · $revelation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(surah.arabicName, fontFamily = QuranArabicFont, fontSize = 23.sp,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NumberTile(number: Int) { SurahNumberMedallion(number) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
    app: NurayaApplication, settings: UserSettings, surahId: Int, ayahId: Int, juzId: Int,
    entries: List<UserEntry>, perform: (suspend () -> Unit) -> Unit, onRead: (Int, Int) -> Unit,
    playback: PlaybackState = PlaybackState(),
    onPlayVerse: ((Int, Int) -> Unit)? = null,
    onToggleAudio: () -> Unit = {},
    reciterChoices: List<Pair<String, String>> = emptyList(),
    wordSyncReciterIds: Set<String> = emptySet(),
    readingModeId: String = "standard",
    onPracticeRange: ((Int, Int, Int, Int) -> Unit)? = null,
    onOpenMushaf: ((Int, Int) -> Unit)? = null,
) {
    val mode = QuranReadingModes.find(readingModeId)
    val displaySettings = QuranReadingModes.settings(mode.id, settings)
    val waqafAnnotations = remember(app) { WaqafAnnotations(app) }
    val language = displaySettings.language
    val context = LocalContext.current
    val allVerses = remember(surahId, juzId) { if (juzId > 0) app.quran.juz(juzId) else app.quran.verses(surahId) }
    val requestedPosition = if (juzId > 0) 1 else (allVerses.indexOfFirst { it.ayah == ayahId } + 1).coerceAtLeast(1)
    var rangeStart by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf(requestedPosition) }
    var rangeEnd by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf((requestedPosition + 15).coerceAtMost(allVerses.size)) }
    val blocks = remember(allVerses, rangeStart, rangeEnd) {
        val startIndex = (rangeStart - 1).coerceIn(0, (allVerses.size - 1).coerceAtLeast(0))
        memorizationBlocks(allVerses.subList(startIndex, rangeEnd.coerceIn(startIndex, allVerses.size)))
    }
    var blockIndex by rememberSaveable(surahId, ayahId, juzId) { mutableIntStateOf(0) }
    val block = blocks.getOrNull(blockIndex) ?: blocks.firstOrNull()
    val verses = if (mode.id == "hafalan_8") block?.verses.orEmpty() else allVerses
    var hideReading by rememberSaveable(surahId, juzId, mode.id, blockIndex) { mutableStateOf(false) }
    var rangeDialog by remember { mutableStateOf(false) }
    var waqafGuide by remember { mutableStateOf(false) }
    var repeatTarget by rememberSaveable { mutableIntStateOf(3) }
    val headerCount = if (mode.id == "standard") 0 else 1
    val initialIndex = remember(surahId, ayahId, juzId, rangeStart, blockIndex) {
        if (juzId > 0) 0 else verses.indexOfFirst { it.ayah == ayahId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (initialIndex == 0) 0 else initialIndex + headerCount)
    val bookmarkKeys = remember(entries) { entries.filter { it.type == "bookmark" }.map { it.key }.toSet() }
    var preferencesVisible by rememberSaveable { mutableStateOf(false) }
    var jumpVisible by rememberSaveable { mutableStateOf(false) }
    var jumpInput by rememberSaveable(surahId, juzId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var studies by remember(surahId, juzId) { mutableStateOf<Map<Int, StudyVerse?>>(emptyMap()) }
    var showTajwidLegend by rememberSaveable { mutableStateOf(false) }
    var qariMenu by remember { mutableStateOf(false) }
    var studyFailed by remember(surahId, juzId) { mutableStateOf(false) }

    LaunchedEffect(surahId, juzId, rangeStart, rangeEnd, blockIndex) {
        try {
            studies = withContext(Dispatchers.IO) { verses.associate { it.number to app.study.verse(it.surah, it.ayah) } }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            studyFailed = true
        }
    }
    LaunchedEffect(surahId, ayahId, juzId, rangeStart, rangeEnd, blockIndex) {
        val playingIndex = if (displaySettings.followRecitation && playback.verseMode && playback.isPlaying)
            verses.indexOfFirst { it.surah == playback.surahId && it.ayah == playback.ayahId } else -1
        listState.scrollToItem(if (playingIndex >= 0) playingIndex + headerCount else if (initialIndex == 0) 0 else initialIndex + headerCount)
    }
    LaunchedEffect(playback.surahId, playback.ayahId, playback.isPlaying, displaySettings.followRecitation, surahId, juzId) {
        if (displaySettings.followRecitation && playback.verseMode && playback.isPlaying) {
            val index = verses.indexOfFirst { it.surah == playback.surahId && it.ayah == playback.ayahId }
            if (index >= 0) listState.animateScrollToItem(index + headerCount)
        }
    }

    LaunchedEffect(surahId, juzId, language, rangeStart, rangeEnd, blockIndex, mode.id) {
        snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collectLatest { index ->
            delay(600)
            verses.getOrNull((index - headerCount).coerceAtLeast(0))?.let { verse -> perform {
                app.library.save("read_history", "${verse.surah}:${verse.ayah}", app.quran.surahs[verse.surah - 1].name,
                    "${label(language,"Ayat","Verse")} ${verse.ayah}")
            } }
        }
    }
    Column(Modifier.fillMaxSize().testTag("reader_screen")) {
        if (onOpenMushaf != null) TextButton(onClick = { verses.firstOrNull { it.ayah == ayahId }?.let { onOpenMushaf(it.surah, it.ayah) } ?: verses.firstOrNull()?.let { onOpenMushaf(it.surah, it.ayah) } }, modifier = Modifier.fillMaxWidth().testTag("open_book_mushaf")) { Text(label(language, "Buka tampilan kitab Madinah", "Open Madinah book view")) }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (juzId > 0) "Juz $juzId" else app.quran.surahs.getOrNull(surahId - 1)?.arabicName.orEmpty(),
                        style = MaterialTheme.typography.titleLarge, fontFamily = if (juzId > 0) null else QuranArabicFont)
                    Text(mode.title(language), modifier = Modifier.testTag("reader_mode"), style = MaterialTheme.typography.bodySmall)
                    Text("${verses.size} ${label(language,"ayat · bacaan offline","verses · offline reading")}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (juzId == 0) TextButton(onClick = { jumpVisible = true }) { Text(label(language,"Ke ayat","Go to verse")) }
                TextButton(onClick = { preferencesVisible = true }) { Text(label(language,"Tulisan","Text size")) }
            }
        }
        if (onPlayVerse != null || onPracticeRange != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { qariMenu = true }) {
                        Icon(Icons.Outlined.RecordVoiceOver, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(reciterChoices.firstOrNull { it.first == displaySettings.readerQariId }?.second
                            ?: label(language, "Pilih qari sinkron", "Choose a synchronized reciter"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = qariMenu, onDismissRequest = { qariMenu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                        reciterChoices.forEach { (id, name) ->
                            DropdownMenuItem(text = { Column {
                                Text(name)
                                Text(if (id in wordSyncReciterIds) label(language, "Sorotan per kata", "Word highlighting") else label(language, "Sorotan per ayat", "Verse highlighting"), style = MaterialTheme.typography.labelSmall)
                            } }, onClick = { perform { app.settings.setReaderQari(id) }; qariMenu = false })
                        }
                    }
                }
                IconButton(onClick = { showTajwidLegend = !showTajwidLegend }) {
                    Icon(Icons.Outlined.Palette, label(language, "Panduan warna tajwid", "Tajwid color guide"))
                }
            }
            Text(if (displaySettings.readerQariId in wordSyncReciterIds)
                label(language, "Ketuk ▶ pada ayat · sorotan kata mengikuti rekaman", "Tap ▶ on a verse · words follow the recording")
                else label(language, "Ketuk ▶ pada ayat · qari ini mendukung sorotan per ayat", "Tap ▶ on a verse · this reciter supports verse highlighting"),
                Modifier.padding(horizontal = 20.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showTajwidLegend) Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { TajwidLegend(language, displaySettings.darkMode) }
            if (playback.verseMode && verses.any { it.surah == playback.surahId && it.ayah == playback.ayahId }) {
                Text(label(language, "Ayat ${playback.surahId}:${playback.ayahId} · ${if (playback.wordTimingAvailable) "sorotan per kata" else "sorotan per ayat"}", "Verse ${playback.surahId}:${playback.ayahId} · ${if (playback.wordTimingAvailable) "word highlighting" else "verse highlighting"}"),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (studyFailed) Text(label(language, "Data belajar belum dapat dibuka. Teks ayat tetap tersedia.", "Study data could not be opened. Verse text is still available."),
            Modifier.padding(horizontal = 20.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
        LazyColumn(state = listState, modifier = Modifier.testTag("reader_verses"), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (headerCount > 0) item(key = "mode_controls") {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mode.id == "hafalan_8") {
                        Text(label(language, "Rentang ${allVerses[rangeStart - 1].surah}:${allVerses[rangeStart - 1].ayah}–${allVerses[rangeEnd - 1].surah}:${allVerses[rangeEnd - 1].ayah} · ${blocks.size} blok latihan", "Range ${allVerses[rangeStart - 1].surah}:${allVerses[rangeStart - 1].ayah}–${allVerses[rangeEnd - 1].surah}:${allVerses[rangeEnd - 1].ayah} · ${blocks.size} practice blocks"), style = MaterialTheme.typography.titleMedium)
                        Text(label(language, "Pembagian latihan Nuraya, bukan pembagian halaman edisi cetak. Rentang pendek dapat berisi kurang dari 8 blok.", "Nuraya practice groups, not printed page boundaries. A short range may contain fewer than 8 blocks."), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { rangeDialog = true }, modifier = Modifier.testTag("memorization_range")) { Text(label(language, "Pilih rentang ayat", "Choose verse range")) }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(blocks, key = { it.number }) { choice ->
                                FilterChip(selected = choice.number == block?.number, onClick = { blockIndex = choice.number - 1 },
                                    label = { Text("${label(language, "Blok", "Block")} ${choice.number}") }, modifier = Modifier.testTag("memorization_block_${choice.number}"))
                            }
                        }
                        if (block != null) {
                            Text("${label(language, "Blok", "Block")} ${block.number}: ${block.reference}", modifier = Modifier.testTag("memorization_block_reference"), style = MaterialTheme.typography.titleMedium)
                            if (onPracticeRange != null && block.start.surah == block.end.surah) Button(onClick = {
                                onPracticeRange(block.start.surah, block.start.ayah, block.end.ayah, 1)
                            }, modifier = Modifier.fillMaxWidth()) { Text(label(language, "Dengarkan blok ini", "Listen to this block")) }
                            else if (block.start.surah != block.end.surah) Text(label(language, "Blok lintas surah: dengarkan dari tombol tiap ayat.", "This block spans chapters: use each verse’s listen button."), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (mode.id == "tikrar") {
                        Text(label(language, "Ulangi audio tiap ayat", "Repeat each verse’s audio"), style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 7, 10, 20).forEach { target ->
                                FilterChip(selected = repeatTarget == target, onClick = { repeatTarget = target }, label = { Text("$target ×") }, modifier = Modifier.testTag("tikrar_target_$target"))
                            }
                        }
                        Text(label(language, "Tekan Dengarkan pada ayat untuk memutarnya $repeatTarget kali. Hitungan latihan mandiri tersimpan di HP.", "Tap Listen on a verse to repeat it $repeatTarget times. Your own practice counts stay on this phone."), style = MaterialTheme.typography.bodySmall)
                    }
                    if (mode.id in setOf("hafalan_8", "tikrar")) {
                        OutlinedButton(onClick = { hideReading = !hideReading }, modifier = Modifier.fillMaxWidth().testTag("recall_toggle")) {
                            Text(label(language, if (hideReading) "Tampilkan bacaan" else "Uji hafalan · sembunyikan bacaan", if (hideReading) "Show reading" else "Practise recall · hide reading"))
                        }
                    }
                    if (mode.id == "waqaf") Button(onClick = { waqafGuide = true }, modifier = Modifier.fillMaxWidth().testTag("waqaf_guide")) {
                        Text(label(language, "Pelajari tanda waqaf & cara memulai lagi", "Learn stopping signs & resuming"))
                    }
                    if (mode.id == "word_tajwid") Text(label(language, "Arti per kata terbuka pada setiap ayat. Warna tajwid mengikuti pengaturan Anda.", "Word meanings open for each verse. Tajwid colors follow your settings."), style = MaterialTheme.typography.bodySmall)
                    if (mode.id == "latin_tajwid") Text(label(language, "Latin membantu mengikuti bacaan; hukum tajwid berwarna ditampilkan pada huruf Arab yang sesuai sumber.", "Transliteration helps you follow along; source-verified tajwid colors appear on Arabic letters."), style = MaterialTheme.typography.bodySmall)
                }
            }
            itemsIndexed(verses, key = { _, verse -> verse.number }) { _, verse ->
                val key = "${verse.surah}:${verse.ayah}"
                val surah = app.quran.surahs[verse.surah - 1]
                val study = studies[verse.number]
                val active = playback.verseMode && playback.surahId == verse.surah && playback.ayahId == verse.ayah
                val selectedReciterPlaying = active && playback.qariId == displaySettings.readerQariId
                val verseBackground by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .38f) else MaterialTheme.colorScheme.background,
                    animationSpec = tween(300), label = "recitationVerse")
                // Keep the opening and verse in one lazy-list item so jump/history indices remain verse-based.
                verse.openingBasmalah?.takeUnless { hideReading }?.let { opening ->
                    Surface(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("surah_basmalah_${verse.surah}"),
                        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opening, Modifier.fillMaxWidth().testTag("basmalah_text_${verse.surah}"),
                                fontFamily = QuranArabicFont, fontSize = displaySettings.arabicSize.sp, textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl,
                                    lineHeight = (displaySettings.arabicSize * 2f).sp, letterSpacing = 0.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)))
                            Spacer(Modifier.height(6.dp))
                            Text(label(language, "Basmalah pembuka · tanpa nomor ayat", "Opening basmalah · unnumbered"),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().background(verseBackground, RoundedCornerShape(16.dp)).padding(vertical = 12.dp).testTag("verse_${verse.surah}_${verse.ayah}")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(key, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (juzId > 0) Text(surah.name, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.labelLarge)
                        else Spacer(Modifier.weight(1f))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onPlayVerse != null || onPracticeRange != null) TextButton(onClick = {
                            if (selectedReciterPlaying && playback.isPlaying) onToggleAudio()
                            else if (mode.id == "tikrar" && onPracticeRange != null) onPracticeRange(verse.surah, verse.ayah, verse.ayah, repeatTarget)
                            else if (selectedReciterPlaying) onToggleAudio()
                            else onPlayVerse?.invoke(verse.surah, verse.ayah)
                        }, modifier = Modifier.testTag("play_verse_${verse.surah}_${verse.ayah}")) {
                            Icon(if (selectedReciterPlaying && playback.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(label(language, if (selectedReciterPlaying && playback.isPlaying) "Jeda" else "Dengarkan", if (selectedReciterPlaying && playback.isPlaying) "Pause" else "Listen"))
                        }
                        TextButton(onClick = { perform { app.library.toggle("bookmark", key, surah.name, "${label(language,"Ayat","Verse")} ${verse.ayah}") } },
                            modifier = Modifier.testTag("bookmark_${verse.surah}_${verse.ayah}")) {
                            Icon(if (key in bookmarkKeys) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                                label(language, if (key in bookmarkKeys) "Hapus penanda" else "Tandai ayat", if (key in bookmarkKeys) "Remove bookmark" else "Bookmark verse"),
                                tint = if (key in bookmarkKeys) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(label(language, if (key in bookmarkKeys) "Ditandai" else "Tandai", if (key in bookmarkKeys) "Saved" else "Bookmark"))
                        }
                        TextButton(onClick = {
                            val translation = when(displaySettings.translation) { "en" -> verse.english; "none" -> ""; else -> verse.indonesian }
                            val source = when(displaySettings.translation) { "en" -> "Marmaduke Pickthall (1930)"; "none" -> ""; else -> "Kementerian Agama RI · QuranEnc v1.0.1" }
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${surah.name} ($key)\n\n${verse.arabic}\n\n$translation\n\nArabic: Tanzil Project, Uthmani · CC BY 3.0\n$source\nhttps://quran.com/$key")
                            }
                            try { context.startActivity(Intent.createChooser(share, label(language,"Bagikan ayat","Share verse"))) }
                            catch (_: ActivityNotFoundException) { Toast.makeText(context,label(language,"Tidak ada aplikasi untuk berbagi.","No sharing app is available."),Toast.LENGTH_SHORT).show() }
                        }) { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(label(language,"Bagikan","Share")) }
                    }
                    Spacer(Modifier.height(18.dp))
                    if (hideReading) {
                        Text(label(language, "Bacaan disembunyikan. Coba baca dari ingatan, lalu tampilkan untuk memeriksa.", "Reading is hidden. Recite from memory, then reveal it to check."),
                            modifier = Modifier.testTag("recall_hidden_${verse.surah}_${verse.ayah}"), style = MaterialTheme.typography.bodyLarge)
                    } else {
                    val coloredText = annotatedQuranText(verse.arabic, study, displaySettings.tajwidColors, displaySettings.darkMode,
                        if (active) playback.wordPosition else 0)
                    val stopAnnotations = if (mode.id == "waqaf") waqafAnnotations.forVerse(verse) else emptyList()
                    Text(if (mode.id == "waqaf") annotatedWaqafText(coloredText, stopAnnotations) else coloredText, Modifier.fillMaxWidth().testTag("verse_text_${verse.surah}_${verse.ayah}"), fontFamily = QuranArabicFont, fontSize = displaySettings.arabicSize.sp,
                        textAlign = TextAlign.Right, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl,
                            lineHeight = (displaySettings.arabicSize * 2f).sp, letterSpacing = 0.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = true)))
                    if (displaySettings.translation != "none") {
                        Spacer(Modifier.height(20.dp))
                        Text(if (displaySettings.translation == "en") verse.english else verse.indonesian, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(10.dp))
                        Text(if (displaySettings.translation == "en") "Marmaduke Pickthall (1930)" else "Kementerian Agama RI · QuranEnc v1.0.1",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (displaySettings.translation == "id" && !verse.indonesianFootnotes.isNullOrBlank()) {
                        var notesOpen by rememberSaveable(verse.number) { mutableStateOf(false) }
                        TextButton(onClick = { notesOpen = !notesOpen }) {
                            Text(label(language,if (notesOpen) "Tutup catatan terjemahan" else "Catatan terjemahan", if (notesOpen) "Hide translation notes" else "Translation notes"))
                        }
                        if (notesOpen) Text(verse.indonesianFootnotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (mode.id == "latin_tajwid") LatinTajwidPanel(verse, study, language, displaySettings.darkMode)
                    if (mode.id == "waqaf") WaqafVerseGuide(verse, language, stopAnnotations, waqafAnnotations.verified(verse))
                    StudyVersePanel(study, language, initiallyShowWords = mode.id == "word_tajwid", tajwidColors = displaySettings.tajwidColors, dark = displaySettings.darkMode)
                    TextButton(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.quran.tafsirUrl(verse.surah,verse.ayah)))) }
                        catch (_: ActivityNotFoundException) { Toast.makeText(context,label(language,"Peramban belum tersedia.","No browser is available."),Toast.LENGTH_SHORT).show() }
                    }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(label(language,"Tafsir Ibn Kathir · Quran.com (EN)","Ibn Kathir commentary · Quran.com (EN)"), style = MaterialTheme.typography.labelMedium)
                    }
                    }
                    if (mode.id == "tikrar") TikrarCounter(verse, language, repeatTarget)
                    HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            item(key = "reader_footer") {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label(language,"Rasm Utsmani · Tanzil · font Amiri Quran.\nBasmalah pembuka dipisahkan tanpa nomor, kecuali Al-Fatihah (1:1). At-Taubah tidak memiliki basmalah pembuka.\nArti per kata dan tafsir Indonesia tersedia di tiap ayat. Tautan tafsir tambahan memerlukan internet.","Uthmani script · Tanzil · Amiri Quran font.\nOpening basmalahs are unnumbered, except Al-Fatihah (1:1). At-Tawbah has no opening basmalah.\nWord meanings and Indonesian commentary are available for each verse. Additional commentary links require internet."),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (juzId == 0) Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { onRead(surahId - 1,1) }, enabled = surahId > 1) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack,null,Modifier.size(18.dp)); Spacer(Modifier.width(5.dp))
                            Text(label(language,"Sebelumnya","Previous"))
                        }
                        OutlinedButton(onClick = { onRead(surahId + 1,1) }, enabled = surahId < 114) {
                            Text(label(language,"Berikutnya","Next")); Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
    if (preferencesVisible) AlertDialog(onDismissRequest = { preferencesVisible = false },
        title = { Text(label(language,"Nyamankan bacaan","Make reading comfortable")) },
        text = {
            Column {
                Text(label(language,"Ukuran tulisan Arab","Arabic text size"),style = MaterialTheme.typography.titleMedium)
                var size by remember(displaySettings.arabicSize) { mutableFloatStateOf(displaySettings.arabicSize) }
                Slider(value = size, onValueChange = { size = it }, valueRange = 22f..48f, steps = 12,
                    onValueChangeFinished = { perform { app.settings.setArabicSize(size) } })
                Text("${size.toInt()} sp", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                Text(label(language,"Terjemahan","Translation"),style = MaterialTheme.typography.titleMedium)
                listOf("id" to "Bahasa Indonesia", "en" to "English", "none" to label(language,"Tanpa terjemahan","Arabic only")).forEach { (value,title) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = displaySettings.translation == value, onClick = { perform { app.settings.setTranslation(value) } })
                        TextButton(onClick = { perform { app.settings.setTranslation(value) } }) { Text(title) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { preferencesVisible = false }) { Text(label(language,"Selesai","Done")) } })
    if (jumpVisible) {
        val number = jumpInput.toIntOrNull()
        AlertDialog(onDismissRequest = { jumpVisible = false }, title = { Text(label(language,"Lompat ke ayat","Jump to verse")) },
            text = { OutlinedTextField(value = jumpInput, onValueChange = { jumpInput = it.filter(Char::isDigit).take(3) },
                label = { Text("${allVerses.first().ayah}–${allVerses.last().ayah}") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { TextButton(enabled = number != null && allVerses.any { it.ayah == number }, onClick = {
                number?.let { target ->
                    if (mode.id == "hafalan_8") onRead(surahId, target)
                    else scope.launch { listState.scrollToItem(allVerses.indexOfFirst { it.ayah == target } + headerCount) }
                }; jumpVisible = false
            }) { Text(label(language,"Buka","Open")) } },
            dismissButton = { TextButton(onClick = { jumpVisible = false }) { Text(label(language,"Batal","Cancel")) } })
    }
    if (waqafGuide) WaqafLearningGuide(language) { waqafGuide = false }
    if (rangeDialog) MemorizationRangeDialog(language, allVerses, juzId > 0, rangeStart, rangeEnd,
        onDismiss = { rangeDialog = false }, onSelect = { start, end ->
            rangeStart = start; rangeEnd = end; blockIndex = 0; hideReading = false; rangeDialog = false
        })

}
