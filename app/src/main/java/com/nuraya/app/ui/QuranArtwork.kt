package com.nuraya.app.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.nuraya.app.quranreading.QuranReadingModes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
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
    readingModeId: String = "standard",
    printedEdition: com.nuraya.app.quranreading.PrintedMushafEdition? = null,
    onReadVerses: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findMushafActivity()
    val repository = remember(context, edition, printedEdition) { MushafImages(context.applicationContext, edition, printedEdition) }
    val scope = rememberCoroutineScope()
    val english = language == "en"
    fun tr(id: String, en: String) = if (english) en else id
    var page by rememberSaveable(edition, printedEdition, initialSurah, initialAyah) { mutableIntStateOf(repository.pageFor(initialSurah, initialAyah)) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var controls by rememberSaveable { mutableStateOf(false) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    var loadedPage by remember { mutableIntStateOf(0) }
    var jump by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var verseActions by remember { mutableStateOf(false) }
    var practice by remember { mutableStateOf(false) }
    var waqafGuide by remember { mutableStateOf(false) }
    var recallHidden by remember { mutableStateOf(false) }
    var downloadBook by remember { mutableStateOf(false) }
    var followingAudio by remember { mutableStateOf(false) }
    var selectionVisible by rememberSaveable { mutableStateOf(initialAyah > 1) }
    var qaris by remember { mutableStateOf(false) }
    var sourceVisible by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    var selectedSurah by rememberSaveable { mutableIntStateOf(initialSurah) }
    var selectedAyah by rememberSaveable { mutableIntStateOf(initialAyah) }
    var showLatin by rememberSaveable { mutableStateOf(readingModeId == "latin_tajwid") }
    var repeats by rememberSaveable { mutableIntStateOf(if (readingModeId == "tikrar") 3 else 1) }
    val playback = if (app != null) app.playback.state.collectAsState().value else com.nuraya.app.media.PlaybackState()
    val entries = if (app != null) app.library.entries.collectAsState(initial = emptyList()).value else emptyList()
    val selected = app?.quran?.verse(selectedSurah, selectedAyah)
    val pageVerses = remember(app, repository, page) {
        repository.references.filter { it.page == page }.mapNotNull { app?.quran?.verse(it.surah, it.ayah) }
    }
    var study by remember(selectedSurah, selectedAyah) { mutableStateOf<com.nuraya.app.study.StudyVerse?>(null) }
    var latin by remember(selectedSurah, selectedAyah) { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSurah, selectedAyah) {
        study = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app?.study?.verse(selectedSurah, selectedAyah) }
        latin = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nuraya.app.quranreading.LatinRepository.shared(context).verse(selectedSurah, selectedAyah)?.latin }
    }
    LaunchedEffect(repository, page) {
        loadedPage = 0; downloadMessage = ""
        val reference = repository.firstReference(page)
        if (repository.pageFor(selectedSurah, selectedAyah) != page) { selectedSurah = reference.surah; selectedAyah = reference.ayah }
    }
    LaunchedEffect(loadedPage, selectedSurah, selectedAyah) {
        if (loadedPage == page && app != null) {
            kotlinx.coroutines.delay(600)
            try { app.library.save("read_history", "$selectedSurah:$selectedAyah",
                app.quran.surahs[selectedSurah - 1].name, "${tr("Ayat", "Verse")} $selectedAyah", "mushaf:$readingModeId") }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Reading remains available if history storage fails. */ }
        }
    }
    var previousAudio by remember(initialSurah, initialAyah) { mutableStateOf(playback) }
    LaunchedEffect(playback.surahId, playback.ayahId, playback.isPlaying, settings.followRecitation) {
        if (!verseActions && !jump && !details && !practice && !qaris && com.nuraya.app.quranreading.shouldFollowBookAudio(selectedSurah, previousAudio, playback, settings.followRecitation, followingAudio)) {
            selectedSurah = playback.surahId
            selectedAyah = playback.ayahId
            page = repository.pageFor(selectedSurah, selectedAyah)
        }
        previousAudio = playback
    }
    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    LaunchedEffect(landscape) {
        activity?.requestedOrientation = if (landscape) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    fun rotate() { landscape = !landscape }
    fun openVerseTools() { controls = false; onReadVerses(selectedSurah, selectedAyah) }
    fun listen() {
        if (app == null) { openVerseTools(); return }
        if (playback.verseMode && playback.surahId == selectedSurah && playback.isPlaying) app.playback.toggle()
        else { followingAudio = true; app.playback.playVerseRange(selectedSurah, selectedAyah, app.quran.surahs[selectedSurah - 1].ayahCount, settings.readerQariId, repeats) }
    }
    BackHandler(controls && !panel && !details && !qaris && !jump && !sourceVisible) { controls = false }
    BackHandler(panel && !details && !qaris && !jump && !sourceVisible) { panel = false }
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().background(if (printedEdition != null) Color(0xFFFFFCF3) else Color.White).testTag("${edition.id}_screen")) {
        val translationPanelHeight = (maxHeight * .48f).coerceAtMost(440.dp)
        Box(Modifier.fillMaxSize().testTag("mushaf_page_viewport")
            .semantics {
                onClick(tr("Tampilkan menu baca", "Show reading menu")) { controls = true; true }
                customActions = listOf(
                    CustomAccessibilityAction(tr("Halaman berikutnya", "Next page")) { if (page < 604) { page++; true } else false },
                    CustomAccessibilityAction(tr("Halaman sebelumnya", "Previous page")) { if (page > 1) { page--; true } else false },
                )
            }
            .pointerInput(page, zoom) {
                if (zoom == 1f) {
                    var drag = 0f
                    detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                        followingAudio = false; recallHidden = false
                        if (drag > swipeThreshold && page < 604) page++
                        else if (drag < -swipeThreshold && page > 1) page--
                    }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount })
                }
            }) {
            AnimatedContent(targetState = page, modifier = Modifier.fillMaxSize(), label = "Mushaf page turn",
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    slideInHorizontally(tween(220)) { -direction * it } togetherWith slideOutHorizontally(tween(220)) { direction * it }
                }) { visiblePage ->
                val audioReference = if (playback.verseMode && playback.isPlaying && repository.pageFor(playback.surahId, playback.ayahId) == visiblePage)
                    playback.surahId to playback.ayahId else null
                MushafPageImage(repository, visiblePage, zoom, language,
                    highlight = audioReference ?: if (selectionVisible) selectedSurah to selectedAyah else null,
                    grayscale = printedEdition?.colored == true && !settings.tajwidColors,
                    onTap = { controls = !controls; if (!controls) panel = false }, onDoubleTap = { rotate() },
                    onVerse = { surah, ayah -> selectedSurah = surah; selectedAyah = ayah; selectionVisible = true; verseActions = true },
                    onReady = { if (page == visiblePage) loadedPage = visiblePage })
            }
        }
        if (recallHidden) Surface(Modifier.fillMaxSize().testTag("mushaf_recall_hidden"), color = Color(0xFFFFFCF3)) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Baca dari ingatan, lalu tampilkan halaman untuk memeriksa.", "Recite from memory, then reveal the page to check."))
                Button(onClick = { recallHidden = false }) { Text(tr("Tampilkan halaman", "Reveal page")) }
            }
        }
        if (controls) Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp, vertical = 4.dp).testTag("mushaf_header"), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { controls = false; onBack() }, modifier = Modifier.testTag("back_button")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, tr("Kembali", "Back")) }
            Column(Modifier.weight(1f)) {
                Text(if (readingModeId == "standard") edition.title(language) else QuranReadingModes.find(readingModeId).title(language), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.testTag("reader_mode"))
                Text(tr("Halaman $page / 604", "Page $page / 604"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("mushaf_page_number"))
            }
            if (app != null) IconButton(onClick = { panel = !panel }, modifier = Modifier.testTag("mushaf_translation")) {
                Icon(Icons.Outlined.Translate, tr("Terjemahan & latin", "Translation & transliteration"))
            }
            IconButton(onClick = { jump = true }) { Icon(Icons.Outlined.Search, tr("Pilih halaman, surah atau juz", "Choose page, chapter or juz")) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, tr("Menu mushaf", "Mushaf menu")) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (readingModeId in setOf("tikrar", "waqaf") && app != null) DropdownMenuItem(
                        text = { Text(if (readingModeId == "tikrar") tr("Latihan Tikrar", "Repetition practice") else tr("Panduan waqaf & ibtida", "Stopping & resuming guide")) },
                        onClick = { menu = false; practice = true })
                    if (readingModeId == "hafalan_8") DropdownMenuItem(text = { Text(tr("Uji hafalan tanpa melihat", "Practise recall without looking")) }, onClick = { menu = false; controls = false; recallHidden = true })
                    if (printedEdition != null) DropdownMenuItem(text = { Text(tr("Unduh 604 halaman edisi ini", "Download all 604 pages")) }, onClick = { menu = false; downloadBook = true })
                    DropdownMenuItem(text = { Text(tr("Layar baca penuh", "Full reading view")) }, onClick = { menu = false; panel = false; controls = false })
                    DropdownMenuItem(text = { Text(tr("Lanskap / potret", "Landscape / portrait")) }, onClick = { menu = false; rotate() })
                    DropdownMenuItem(text = { Text(tr("Perbesar halaman", "Enlarge page")) }, enabled = zoom < 3f, onClick = { menu = false; zoom = (zoom + .5f).coerceAtMost(3f) })
                    DropdownMenuItem(text = { Text(tr("Ukuran halaman semula", "Original page size")) }, enabled = zoom > 1f, onClick = { menu = false; zoom = 1f })
                    DropdownMenuItem(text = { Text(tr("Halaman sebelumnya", "Previous page")) }, enabled = page > 1,
                        modifier = Modifier.testTag("mushaf_previous"), onClick = { menu = false; followingAudio = false; page-- })
                    DropdownMenuItem(text = { Text(tr("Halaman berikutnya", "Next page")) }, enabled = page < 604,
                        modifier = Modifier.testTag("mushaf_next"), onClick = { menu = false; followingAudio = false; page++ })
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
                    if (app != null) DropdownMenuItem(text = { Text(tr("Arti per kata & tafsir ayat", "Word meanings & verse tafsir")) }, onClick = { menu = false; details = true })
                    DropdownMenuItem(text = { Text(tr("Baca per ayat & fitur hafalan", "Verse reader & memorization")) }, onClick = { menu = false; openVerseTools() })
                }
            }
        }
        if (controls) Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            if (panel && selected != null) com.nuraya.app.quranreading.MushafTranslationPanel(
                verses = pageVerses, selected = selected, surahName = app.quran.surahs[selectedSurah - 1].name,
                language = language, showLatin = showLatin, onLatinChange = { showLatin = it },
                onSelect = { selectedSurah = it.surah; selectedAyah = it.ayah; selectionVisible = true }, onClose = { panel = false },
                modifier = Modifier.fillMaxWidth().height(translationPanelHeight),
            )
            if (downloadMessage.isNotEmpty() || playback.error != null) Surface {
                Text(playback.error ?: downloadMessage, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
            Surface(Modifier.fillMaxWidth().testTag("mushaf_controls"), tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val playingHere = playback.verseMode && playback.surahId == selectedSurah && playback.isPlaying
                    IconButton(onClick = { listen() }, modifier = Modifier.testTag("mushaf_play")) {
                        Icon(if (playingHere) Icons.Outlined.PauseCircleOutline else Icons.Outlined.PlayCircleOutline, if (playingHere) tr("Jeda audio", "Pause audio") else tr("Putar bacaan", "Play recitation"))
                    }
                    if (app != null) {
                        val saved = entries.any { it.type == "bookmark" && it.key == "$selectedSurah:$selectedAyah" }
                        IconButton(onClick = { scope.launch { app.library.toggle("bookmark", "$selectedSurah:$selectedAyah", app.quran.surahs[selectedSurah - 1].name, "Ayat $selectedAyah") } }) {
                            Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, tr(if (saved) "Hapus penanda ayat" else "Tandai ayat", if (saved) "Remove verse bookmark" else "Bookmark verse"))
                        }
                        IconButton(onClick = { qaris = true }) { Icon(Icons.Outlined.Tune, tr("Pengaturan murottal", "Recitation settings")) }
                        Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable { qaris = true }.testTag("mushaf_qari"), verticalAlignment = Alignment.CenterVertically) {
                            val displayedQari = if (playingHere) playback.qariId else settings.readerQariId
                            Text(app.playback.verseReciters.firstOrNull { it.id == displayedQari }?.name ?: tr("Pilih qari", "Choose reciter"),
                                Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.ExpandMore, tr("Pilih qari", "Choose reciter"))
                        }
                    }
                }
            }
        }
    }
    if (jump) MushafJumpDialog(language, page, selectedSurah, selectedAyah, app, onDismiss = { jump = false }) { surah, ayah, target ->
        followingAudio = false; recallHidden = false
        if (surah != null && ayah != null) { selectedSurah = surah; selectedAyah = ayah; selectionVisible = true }
        page = target ?: repository.pageFor(selectedSurah, selectedAyah); jump = false
    }
    if (verseActions && selected != null) ModalBottomSheet(onDismissRequest = { verseActions = false }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${app.quran.surahs[selectedSurah - 1].name} · $selectedAyah", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("mushaf_selected_verse"))
            TextButton(onClick = { verseActions = false; app.playback.playVerseRange(selectedSurah, selectedAyah, selectedAyah, settings.readerQariId, repeats) }) {
                Text(tr("Putar ayat ini", "Play this verse"))
            }
            TextButton(onClick = { scope.launch { app.library.toggle("bookmark", "$selectedSurah:$selectedAyah", app.quran.surahs[selectedSurah - 1].name, "Ayat $selectedAyah") }; verseActions = false }) {
                Text(tr("Tandai ayat", "Bookmark verse"))
            }
            TextButton(onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("$selectedSurah:$selectedAyah", "${selected.arabic}\n\n${selected.indonesian}\n($selectedSurah:$selectedAyah)"))
                verseActions = false
            }) { Text(tr("Salin ayat", "Copy verse")) }
            TextButton(onClick = {
                val text = "${selected.arabic}\n\n${selected.indonesian}\n(${app.quran.surahs[selectedSurah - 1].name} $selectedSurah:$selectedAyah)"
                context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND)
                    .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text), tr("Bagikan ayat", "Share verse")))
                verseActions = false
            }) { Text(tr("Bagikan ayat", "Share verse")) }
            TextButton(onClick = { verseActions = false; controls = true; panel = true }) { Text(tr("Terjemahan & latin", "Translation & transliteration")) }
            TextButton(onClick = { verseActions = false; details = true }) { Text(tr("Arti per kata & tafsir ayat", "Word meanings & verse tafsir")) }
            TextButton(onClick = { verseActions = false }) { Text(tr("Tutup", "Close")) }
        }
    }
    if (sourceVisible) AlertDialog(onDismissRequest = { sourceVisible = false }, title = { Text(tr("Tajwid & sumber mushaf", "Tajwid & page sources")) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Panduan warna pada teks digital Nuraya", "Color guide for Nuraya digital text"), style = MaterialTheme.typography.titleSmall)
            TajwidLegend(language, settings.darkMode)
            if (printedEdition != null) Text(tr("Warna pada gambar cetak mengikuti edisi sumbernya; legenda di atas digunakan pada panel teks digital.", "Printed image colors follow their source edition; the legend above applies to the digital text panel."))
            if (printedEdition == com.nuraya.app.quranreading.PrintedMushafEdition.HAFALAN_8) Text(tr("Edisi Hafalan belum memiliki peta sorotan ayat. Pilih surah/ayat melalui tombol pencarian untuk memutar atau melihat artinya.", "The Hafalan edition has no verse highlight map. Use the search control to choose a verse for audio or translation."))
            if (printedEdition != null) Text(tr("Halaman ${printedEdition.title(language)}: Quran Image Assets, agusibrahim/quran-image. Gambar asli dipertahankan. Terjemahan pada panel: Kementerian Agama RI melalui QuranEnc; latin panel: EQuran.id. Sorotan tersedia pada enam edisi berpeta (belum pada Hafalan); arti per kata dan latin tercetak mengikuti edisi yang dipilih.", "${printedEdition.title(language)} pages: Quran Image Assets, agusibrahim/quran-image. Original images are preserved. Panel translations: QuranEnc / Pickthall; panel transliteration: EQuran.id. Verse highlighting is available for six mapped editions (not yet Hafalan); printed word meanings and transliteration belong to the selected edition."))
            else if (edition == MushafEdition.INDONESIA) Text(tr("Gambar Arab berbingkai: siar08888000077/mushaf-indonesia, diperiksa terhadap checksum sumber. Ini bukan edisi terjemah per kata Islampedia. Terjemahan Indonesia, arti per kata, tajwid berwarna, dan sorotan audio tersedia di panel pendamping. Gambar tidak diubah. Halaman 1, 2, 3, 604 tersedia langsung; simpan halaman lain untuk membaca offline.", "Ornamental Arabic pages: siar08888000077/mushaf-indonesia, checked against source hashes. These are not the Islampedia word-translation edition. Indonesian translation, word meanings, colored tajwid and audio highlights appear in the companion panel. Images are unchanged. Pages 1, 2, 3, 604 are bundled; save other pages for offline reading."))
            else Text(tr("Gambar asli Madinah: King Fahd Quran Complex melalui GovarJabbar Quran-PNG/Quran-JPG, diperiksa terhadap checksum sumber. Warna tajwid, arti per kata, dan sorotan audio tersedia di panel pendamping; gambar halaman tidak diubah. Halaman 1, 2, 3, 604 tersedia langsung; simpan halaman lain untuk membacanya offline.", "Original Madinah pages: King Fahd Quran Complex via GovarJabbar Quran-PNG/Quran-JPG, checked against source hashes. Tajwid, word meanings and audio highlights appear in the companion panel; page images remain unchanged. Pages 1, 2, 3, 604 are bundled; save other pages for offline reading."))
        }
    }, confirmButton = { TextButton(onClick = { sourceVisible = false }) { Text(tr("Tutup", "Close")) } })
    if (downloadBook) com.nuraya.app.quranreading.MushafDownloadDialog(repository, language) { downloadBook = false }
    if (qaris && app != null) com.nuraya.app.quranreading.MushafAudioSheet(app, settings, selectedSurah, selectedAyah, repeats,
        onDismiss = { qaris = false }) { startSurah, startAyah, endSurah, endAyah, qari, each, passes, loop ->
            selectedSurah = startSurah; selectedAyah = startAyah; page = repository.pageFor(startSurah, startAyah)
            repeats = each; followingAudio = true; selectionVisible = true; qaris = false
            app.playback.playVerseSpan(startSurah, startAyah, endSurah, endAyah, qari, each, loop, passes)
        }
    if (waqafGuide) com.nuraya.app.quranreading.WaqafLearningGuide(language) { waqafGuide = false }
    if (practice && selected != null) ModalBottomSheet(onDismissRequest = { practice = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${app.quran.surahs[selectedSurah - 1].name} · $selectedAyah", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { practice = false; jump = true }) { Text(tr("Pilih ayat", "Choose verse")) }
            if (readingModeId == "tikrar") {
                com.nuraya.app.quranreading.TikrarCounter(selected, language, repeats)
                Button(onClick = { practice = false; qaris = true }) { Text(tr("Atur pengulangan audio", "Set audio repetitions")) }
            } else {
                val waqaf = remember { com.nuraya.app.quranreading.WaqafAnnotations(context) }
                com.nuraya.app.quranreading.WaqafVerseGuide(selected, language, waqaf.forVerse(selected), waqaf.verified(selected))
                Text(tr("Letak tanda pada edisi cetak dapat berbeda. Saat ibtida, mulai dari rangkaian kata yang maknanya utuh.", "Printed editions may place signs differently. Resume from words that preserve the complete meaning."))
                Button(onClick = { practice = false; waqafGuide = true }) { Text(tr("Panduan semua tanda", "Guide to all signs")) }
            }
            TextButton(onClick = { practice = false }) { Text(tr("Tutup", "Close")) }
        }
    }
    if (details && selected != null) ModalBottomSheet(onDismissRequest = { details = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${app.quran.surahs[selectedSurah - 1].name} · $selectedAyah", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { details = false }) { Icon(Icons.Outlined.Close, tr("Tutup rincian ayat", "Close verse details")) }
            }
            TextButton(onClick = { details = false; jump = true }) { Text(tr("Pilih ayat", "Choose verse")) }
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
                OutlinedButton(onClick = { openVerseTools(); details = false }) { Text(tr("Baca per ayat", "Verse reader")) }
            }
        }
    }
}

/** Page geometry never depends on whether its overlay controls are visible. */
@Composable
private fun MushafPageImage(repository: MushafImages, page: Int, zoom: Float, language: String,
                            highlight: Pair<Int, Int>?, grayscale: Boolean, onTap: () -> Unit, onDoubleTap: () -> Unit,
                            onVerse: (Int, Int) -> Unit, onReady: () -> Unit) {
    var bitmap by remember(repository, page) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val ready by rememberUpdatedState(onReady)
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val regions = remember(repository, page) { repository.regionsFor(page) }
    LaunchedEffect(repository, page, retry) {
        error = false
        try { bitmap = repository.load(page); ready() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
    }
    BoxWithConstraints(Modifier.fillMaxSize().then(if (bitmap == null) Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { tap() }, onDoubleTap = { doubleTap() })
    } else Modifier), contentAlignment = Alignment.Center) {
        val current = bitmap
        when {
            error -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label(language, "Halaman belum berhasil dimuat. Periksa koneksi lalu coba lagi.", "Page unavailable. Check your connection and retry."), color = Color.Black)
                Button(onClick = { retry++ }) { Text(label(language, "Coba lagi", "Retry")) }
            }
            current == null -> CircularProgressIndicator()
            else -> {
                val description = repository.title(language) + label(language, " halaman $page", " page $page")
                if (zoom == 1f && androidx.compose.ui.platform.LocalConfiguration.current.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    com.nuraya.app.quranreading.MushafImageLayer(current, description, regions, highlight, grayscale, onTap, onDoubleTap, onVerse, Modifier.fillMaxSize())
                } else {
                    val width = maxWidth * zoom
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                        com.nuraya.app.quranreading.MushafImageLayer(current, description, regions, highlight, grayscale, onTap, onDoubleTap, onVerse,
                            Modifier.width(width).height(width * current.height.toFloat() / current.width))
                    }
                }
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
