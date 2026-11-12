package com.nuraya.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuraya.app.quranreading.MushafImages
import com.nuraya.app.quranreading.MushafEdition
import kotlinx.coroutines.CancellationException

/** A genuine source page preview, not a generated cover or a different printed edition. */
@Composable
fun QuranArtwork(modifier: Modifier = Modifier, page: Int = 1) {
    val context = LocalContext.current
    val bitmap = remember(page) {
        val bundledPage = if (page in setOf(1, 2, 604)) page else 1
        context.assets.open("mushaf/%03d.png".format(java.util.Locale.US, bundledPage)).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
        }
    }
    if (bitmap != null) Image(bitmap.asImageBitmap(), "Pratinjau halaman mushaf Madinah", modifier.background(Color.White), contentScale = ContentScale.Fit)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MadinahScreen(
    language: String,
    initialSurah: Int = 1,
    initialAyah: Int = 1,
    showTajwidCompanion: Boolean = false,
    app: com.nuraya.app.NurayaApplication? = null,
    settings: com.nuraya.app.storage.UserSettings = com.nuraya.app.storage.UserSettings(),
    onBack: () -> Unit = {},
    edition: MushafEdition = MushafEdition.MADINAH,
    onReadVerses: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findMushafActivity()
    val repository = remember(context, edition) { MushafImages(context.applicationContext, edition) }
    val scope = rememberCoroutineScope()
    val english = language == "en"
    fun tr(id: String, en: String) = if (english) en else id
    var page by rememberSaveable(edition, initialSurah, initialAyah) { mutableIntStateOf(repository.pageFor(initialSurah, initialAyah)) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var controls by rememberSaveable { mutableStateOf(true) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var bitmap by remember(edition, page) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf(false) }
    var qaris by remember { mutableStateOf(false) }
    var sourceVisible by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(initialSurah) }
    var selectedAyah by rememberSaveable { mutableIntStateOf(initialAyah) }
    var showLatin by rememberSaveable { mutableStateOf(false) }
    var repeats by rememberSaveable { mutableIntStateOf(1) }
    val playback = if (app != null) app.playback.state.collectAsState().value else com.nuraya.app.media.PlaybackState()
    val selected = app?.quran?.verse(selectedSurah, selectedAyah)
    var study by remember(selectedSurah, selectedAyah) { mutableStateOf<com.nuraya.app.study.StudyVerse?>(null) }
    var latin by remember(selectedSurah, selectedAyah) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSurah, selectedAyah) {
        study = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app?.study?.verse(selectedSurah, selectedAyah) }
        latin = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nuraya.app.quranreading.LatinRepository.shared(context).verse(selectedSurah, selectedAyah)?.latin }
    }
    LaunchedEffect(edition, page, retry) {
        bitmap = null; error = false; downloadMessage = ""
        val reference = repository.firstReference(page)
        if (repository.pageFor(selectedSurah, selectedAyah) != page) { selectedSurah = reference.surah; selectedAyah = reference.ayah }
        try { bitmap = repository.load(page) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
    }
    LaunchedEffect(playback.surahId, playback.ayahId, settings.followRecitation) {
        if (settings.followRecitation && playback.verseMode && playback.surahId > 0 && playback.ayahId > 0) {
            selectedSurah = playback.surahId; selectedAyah = playback.ayahId
            page = repository.pageFor(selectedSurah, selectedAyah)
        }
    }
    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    LaunchedEffect(landscape) {
        activity?.requestedOrientation = if (landscape) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    fun rotate() { landscape = !landscape }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F0DB)).testTag("${edition.id}_screen")) {
        if (controls) Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, tr("Kembali", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(edition.title(language), style = MaterialTheme.typography.titleMedium)
                Text(tr("Halaman $page / 604", "Page $page / 604"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("mushaf_page_number"))
            }
            IconButton(onClick = { jump = true }) { Icon(Icons.Outlined.Search, tr("Pilih halaman, surah atau juz", "Choose page, chapter or juz")) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, tr("Menu mushaf", "Mushaf menu")) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("Lanskap / potret", "Landscape / portrait")) }, onClick = { menu = false; rotate() })
                    DropdownMenuItem(text = { Text(tr("Simpan halaman offline", "Save page offline")) }, enabled = !downloading, onClick = {
                        menu = false; downloading = true
                        val savedPage = page
                        scope.launch {
                            downloadMessage = try { repository.saveOffline(savedPage); tr("Halaman $savedPage tersimpan di HP", "Page $savedPage saved on this phone") }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { tr("Gagal menyimpan. Periksa koneksi dan ruang penyimpanan.", "Could not save. Check connection and storage.") }
                            finally { downloading = false }
                        }
                    })
                    DropdownMenuItem(text = { Text(tr("Info tajwid & sumber", "Tajwid & sources")) }, onClick = { menu = false; sourceVisible = true })
                    DropdownMenuItem(text = { Text(tr("Baca per ayat & fitur hafalan", "Verse reader & memorization")) }, onClick = { menu = false; onReadVerses(selectedSurah, selectedAyah) })
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(8.dp)
            .border(2.dp, Color(0xFFB19453), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(5.dp).testTag("mushaf_page_viewport")
            .pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }, onDoubleTap = { rotate() }) }, contentAlignment = Alignment.Center) {
            val current = bitmap
            when {
                error -> Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("Halaman belum berhasil dimuat. Periksa koneksi lalu coba lagi.", "Page unavailable. Check your connection and retry."), color = Color(0xFF202020))
                    Button(onClick = { retry++ }) { Text(tr("Coba lagi", "Retry")) }
                }
                current == null -> CircularProgressIndicator()
                else -> key(edition, page) {
                    if (zoom == 1f && androidx.compose.ui.platform.LocalConfiguration.current.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) Image(current.asImageBitmap(), edition.title(language) + tr(" halaman $page", " page $page"), Modifier.fillMaxSize().testTag("mushaf_page_image"), contentScale = ContentScale.Fit)
                    else {
                        val pageWidth = maxWidth * zoom
                        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                            Image(current.asImageBitmap(), edition.title(language) + tr(" halaman $page", " page $page"), Modifier.width(pageWidth).height(pageWidth * current.height.toFloat() / current.width).testTag("mushaf_page_image"), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
        }
        if (controls) Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (downloadMessage.isNotEmpty()) Text(downloadMessage, style = MaterialTheme.typography.bodySmall)
            if (edition == MushafEdition.INDONESIA) Text(tr("Arti per kata & tajwid: buka panel di bawah", "Word meanings & tajwid: open the panel below"), style = MaterialTheme.typography.labelSmall)
            Text(tr("Ketuk: tampil/sembunyikan tombol · Ketuk 2×: lanskap", "Tap: show/hide controls · Double tap: landscape"), style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { page-- }, enabled = page > 1, modifier = Modifier.testTag("mushaf_previous")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, tr("Halaman sebelumnya", "Previous page")) }
                TextButton(onClick = { zoom = (zoom - .5f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text(tr("− Kecilkan", "− Smaller")) }
                TextButton(onClick = { zoom = (zoom + .5f).coerceAtMost(3f) }, enabled = zoom < 3f) { Text(tr("+ Besarkan", "+ Larger")) }
                IconButton(onClick = { page++ }, enabled = page < 604, modifier = Modifier.testTag("mushaf_next")) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, tr("Halaman berikutnya", "Next page")) }
            }
            if (app != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { panel = true }, modifier = Modifier.testTag("mushaf_translation")) { Text(tr("Arti & tajwid", "Meaning & tajwid")) }
                OutlinedButton(onClick = { qaris = true }) { Text(tr("Qari & audio", "Reciter & audio")) }
                FilledTonalButton(onClick = {
                    if (playback.verseMode && playback.surahId == selectedSurah && playback.isPlaying) app.playback.toggle()
                    else app.playback.playVerseRange(selectedSurah, selectedAyah, app.quran.surahs[selectedSurah - 1].ayahCount, settings.readerQariId, repeats)
                }) { Text(if (playback.isPlaying) tr("Jeda", "Pause") else tr("Dengarkan", "Listen")) }
            } else Button(onClick = { onReadVerses(selectedSurah, selectedAyah) }) { Text(if (showTajwidCompanion) tr("Baca dengan tajwid berwarna", "Read with colored tajwid") else tr("Baca per ayat & dengarkan", "Read verses & listen")) }
        }
    }
    if (jump) MushafJumpDialog(language, page, selectedSurah, selectedAyah, app, onDismiss = { jump = false }) { surah, ayah, target ->
        if (surah != null && ayah != null) { selectedSurah = surah; selectedAyah = ayah }
        page = target ?: repository.pageFor(selectedSurah, selectedAyah); jump = false
    }
    if (sourceVisible) AlertDialog(onDismissRequest = { sourceVisible = false }, title = { Text(tr("Tajwid & sumber mushaf", "Tajwid & page sources")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TajwidLegend(language, settings.darkMode)
            if (edition == MushafEdition.INDONESIA) Text(tr("Gambar Arab berbingkai: siar08888000077/mushaf-indonesia, diperiksa terhadap checksum sumber. Ini bukan edisi terjemah per kata Islampedia. Terjemahan Indonesia, arti per kata, tajwid berwarna, dan sorotan audio tersedia di panel pendamping. Gambar tidak diubah. Halaman 1, 2, 3, 604 tersedia langsung; simpan halaman lain untuk membaca offline.", "Ornamental Arabic pages: siar08888000077/mushaf-indonesia, checked against source hashes. These are not the Islampedia word-translation edition. Indonesian translation, word meanings, colored tajwid and audio highlights appear in the companion panel. Images are unchanged. Pages 1, 2, 3, 604 are bundled; save other pages for offline reading."))
            else Text(tr("Gambar asli Madinah: King Fahd Quran Complex melalui GovarJabbar Quran-PNG/Quran-JPG, diperiksa terhadap checksum sumber. Warna tajwid, arti per kata, dan sorotan audio tersedia di panel pendamping; gambar halaman tidak diubah. Halaman 1, 2, 3, 604 tersedia langsung; simpan halaman lain untuk membacanya offline.", "Original Madinah pages: King Fahd Quran Complex via GovarJabbar Quran-PNG/Quran-JPG, checked against source hashes. Tajwid, word meanings and audio highlights appear in the companion panel; page images remain unchanged. Pages 1, 2, 3, 604 are bundled; save other pages for offline reading."))
        }
    }, confirmButton = { TextButton(onClick = { sourceVisible = false }) { Text(tr("Tutup", "Close")) } })
    if (qaris && app != null) ModalBottomSheet(onDismissRequest = { qaris = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        var search by remember { mutableStateOf("") }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Qari & pengulangan", "Reciter & repeats"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { qaris = false }) { Icon(Icons.Outlined.Close, tr("Tutup pilihan qari", "Close reciter picker")) }
            }
            OutlinedTextField(search, { search = it }, label = { Text(tr("Cari nama qari", "Search reciters")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Ulang tiap ayat: $repeats×", "Repeat each verse: $repeats×"), Modifier.weight(1f))
                TextButton(onClick = { repeats-- }, enabled = repeats > 1) { Text("−") }
                TextButton(onClick = { repeats++ }, enabled = repeats < 20) { Text("+") }
            }
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 450.dp)) {
                items(app.playback.verseReciters.filter { it.name.contains(search, ignoreCase = true) }, key = { it.id }) { qari ->
                    ListItem(headlineContent = { Text(qari.name) }, leadingContent = { QariPortrait(qari.id, qari.name, language, Modifier.size(52.dp)) }, supportingContent = { Text(if (qari.supportsWordSync) tr("Sorotan per kata tersedia", "Word highlighting available") else tr("Sorotan per ayat", "Verse highlighting")) }, trailingContent = { RadioButton(selected = settings.readerQariId == qari.id, onClick = null) }, modifier = Modifier.clickable { scope.launch { app.settings.setReaderQari(qari.id); qaris = false } })
                }
            }
        }
    }
    if (panel && selected != null && app != null) ModalBottomSheet(onDismissRequest = { panel = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${app.quran.surahs[selectedSurah - 1].name} · $selectedAyah", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { panel = false }) { Icon(Icons.Outlined.Close, tr("Tutup panel arti", "Close meaning panel")) }
            }
            TextButton(onClick = { panel = false; jump = true }) { Text(tr("Pilih ayat", "Choose verse")) }
            Text(annotatedQuranText(selected.arabic, study, settings.tajwidColors, settings.darkMode, if (playback.verseMode && playback.surahId == selectedSurah && playback.ayahId == selectedAyah) playback.wordPosition else 0), fontFamily = QuranArabicFont, fontSize = settings.arabicSize.sp, lineHeight = (settings.arabicSize * 1.8f).sp, textAlign = androidx.compose.ui.text.style.TextAlign.Right, modifier = Modifier.fillMaxWidth())
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val previous = app.quran.allVerses.getOrNull(selected.number - 2)
                    if (previous != null) { selectedSurah = previous.surah; selectedAyah = previous.ayah; page = repository.pageFor(selectedSurah, selectedAyah) }
                }, enabled = selected.number > 1) { Text(tr("Ayat sebelumnya", "Previous verse")) }
                OutlinedButton(onClick = {
                    val next = app.quran.allVerses.getOrNull(selected.number)
                    if (next != null) { selectedSurah = next.surah; selectedAyah = next.ayah; page = repository.pageFor(selectedSurah, selectedAyah) }
                }, enabled = selected.number < 6236) { Text(tr("Ayat berikutnya", "Next verse")) }
            }
            Text(selected.indonesian)
            Text("Kementerian Agama RI · QuranEnc v1.0.1", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text(tr("Tampilkan latin", "Show transliteration"), Modifier.weight(1f)); Switch(showLatin, { showLatin = it }) }
            if (showLatin) {
                Text(latin ?: tr("Memuat latin…", "Loading transliteration…"))
                Text("Transliterasi Latin Indonesia · EQuran.id API v2", style = MaterialTheme.typography.labelSmall)
            }
            Text(tr("Ukuran tulisan Arab", "Arabic text size"))
            Slider(value = settings.arabicSize, onValueChange = { value -> scope.launch { app.settings.setArabicSize(value) } }, valueRange = 22f..48f)
            StudyVersePanel(study, language, initiallyShowWords = showTajwidCompanion || edition == MushafEdition.INDONESIA, tajwidColors = settings.tajwidColors, dark = settings.darkMode)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { app.playback.playVerseRange(selectedSurah, selectedAyah, selectedAyah, settings.readerQariId, repeats) }) { Text(tr("Putar ayat ini", "Play this verse")) }
                if (playback.isPlaying) OutlinedButton(onClick = { app.playback.toggle() }) { Text(tr("Jeda", "Pause")) }
                OutlinedButton(onClick = { onReadVerses(selectedSurah, selectedAyah); panel = false }) { Text(tr("Baca per ayat", "Verse reader")) }
            }
        }
    }
}

private fun android.content.Context.findMushafActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findMushafActivity()
    else -> null
}

@Composable
private fun MushafJumpDialog(language: String, currentPage: Int, surah: Int, ayah: Int, app: com.nuraya.app.NurayaApplication?, onDismiss: () -> Unit, onOpen: (Int?, Int?, Int?) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf(currentPage.toString()) }
    var second by remember { mutableStateOf(ayah.toString()) }
    val s = first.toIntOrNull(); val a = second.toIntOrNull()
    val valid = when (tab) { 0 -> s in 1..604; 1 -> s in 1..114 && a in 1..(app?.quran?.surahs?.getOrNull((s ?: 0) - 1)?.ayahCount ?: 0); else -> s in 1..30 && app != null }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(label(language, "Menuju bacaan", "Jump to reading")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TabRow(tab) { listOf(label(language, "Halaman", "Page"), "Surah", "Juz").forEachIndexed { i, name -> Tab(selected = tab == i, enabled = app != null || i == 0, onClick = { tab = i; first = when(i) { 0 -> currentPage.toString(); 1 -> surah.toString(); else -> "1" } }, text = { Text(name) }) } }
            OutlinedTextField(first, { first = it.filter(Char::isDigit).take(3) }, label = { Text(when(tab) { 0 -> "1–604"; 1 -> "Surah 1–114"; else -> "Juz 1–30" }) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.testTag("mushaf_jump_number"))
            if (tab == 1) { Text(app?.quran?.surahs?.getOrNull((s ?: 0) - 1)?.name.orEmpty()); OutlinedTextField(second, { second = it.filter(Char::isDigit).take(3) }, label = { Text(label(language, "Ayat", "Verse")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = {
        when (tab) { 0 -> onOpen(null, null, s); 1 -> onOpen(s, a, null); else -> app?.quran?.juz(s!!)?.firstOrNull()?.let { onOpen(it.surah, it.ayah, null) } }
    }) { Text(label(language, "Buka", "Open")) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(label(language, "Batal", "Cancel")) } })
}
