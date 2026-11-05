package com.nuraya.app.ui

import android.graphics.Bitmap
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MadinahScreen(
    language: String,
    initialSurah: Int = 1,
    initialAyah: Int = 1,
    showTajwidCompanion: Boolean = false,
    onReadVerses: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { MushafImages(context.applicationContext) }
    val english = language == "en"
    var page by rememberSaveable(initialSurah, initialAyah) { mutableIntStateOf(repository.pageFor(initialSurah, initialAyah)) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var retry by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var pageText by remember { mutableStateOf("") }
    var sourceVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(page, retry) {
        bitmap = null
        error = false
        try { bitmap = repository.load(page) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = true }
    }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("madinah_screen")) {
      // Keep a readable page viewport when controls wrap on short screens or at large font sizes.
      val contentHeight = maxHeight.coerceAtLeast(800.dp * fontScale)
      Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("madinah_scroll")) {
       Column(Modifier.fillMaxWidth().height(contentHeight).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (english) "Madinah mushaf" else "Mushaf Madinah", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (english) "Page $page / 604" else "Halaman $page / 604", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).testTag("mushaf_page_number"))
            OutlinedButton(onClick = { pageText = page.toString(); jump = true }) { Text(if (english) "Choose page" else "Pilih halaman") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text(if (english) "− Smaller" else "− Kecilkan") }
            OutlinedButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(3f) }, enabled = zoom < 3f) { Text(if (english) "+ Larger" else "+ Besarkan") }
            TextButton(onClick = { sourceVisible = !sourceVisible }) { Text(if (english) "Page source" else "Sumber gambar") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).background(Color.White), contentAlignment = Alignment.Center) {
            val current = bitmap
            when {
                error -> Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (english) "This page could not be loaded. Check your connection and try again." else "Halaman belum berhasil dimuat. Periksa koneksi lalu coba lagi.", color = Color(0xFF202020))
                    Button(onClick = { retry++ }) { Text(if (english) "Try again" else "Coba lagi") }
                }
                current == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(if (english) "Loading page…" else "Memuat halaman…", color = Color(0xFF202020))
                }
                else -> key(page) {
                    val pageWidth = maxWidth * zoom
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                        Image(current.asImageBitmap(), if (english) "Madinah page $page" else "Mushaf Madinah halaman $page",
                            Modifier.width(pageWidth).height(pageWidth * current.height.toFloat() / current.width.toFloat()).testTag("mushaf_page_image"), contentScale = ContentScale.FillBounds)
                    }
                }
            }
        }
        Text(if (english) "Scroll to read; drag sideways after enlarging." else "Gulir untuk membaca; geser ke samping setelah diperbesar.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { page-- }, enabled = page > 1, modifier = Modifier.weight(1f).testTag("mushaf_previous")) { Text(if (english) "Previous" else "Sebelumnya") }
            Button(onClick = { page++ }, enabled = page < 604, modifier = Modifier.weight(1f).testTag("mushaf_next")) { Text(if (english) "Next" else "Berikutnya") }
        }
        Button(onClick = { repository.firstReference(page).let { onReadVerses(it.surah, it.ayah) } }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showTajwidCompanion) {
                if (english) "Read with colored tajwid" else "Baca dengan tajwid berwarna"
            } else if (english) "Read verses & listen" else "Baca per ayat & dengarkan")
        }
        if (showTajwidCompanion) Text(if (english) "The original page is unchanged. Open the verse reader for colored tajwid." else "Gambar halaman asli tetap utuh. Warna tajwid tersedia di bacaan per ayat.", style = MaterialTheme.typography.bodySmall)
       }
      }
    }
    if (sourceVisible) AlertDialog(onDismissRequest = { sourceVisible = false },
        title = { Text(if (english) "Page source" else "Sumber gambar") },
        text = { Text(
                if (english) "Original Madinah pages, King Fahd Quran Complex artwork, via GovarJabbar Quran-PNG/Quran-JPG. Pages 1, 2, 3 and 604 are bundled; other pages download when opened and remain in the recent-page cache."
                else "Halaman asli Madinah, karya King Fahd Quran Complex, melalui GovarJabbar Quran-PNG/Quran-JPG. Halaman 1, 2, 3 dan 604 tersedia langsung; halaman lain diunduh saat dibuka lalu disimpan dalam cache halaman terbaru.",
            Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { sourceVisible = false }) { Text(if (english) "Close" else "Tutup") } })
    if (jump) AlertDialog(onDismissRequest = { jump = false }, title = { Text(if (english) "Choose a page" else "Pilih halaman") },
        text = { OutlinedTextField(value = pageText, onValueChange = { pageText = it.filter(Char::isDigit).take(3) },
            label = { Text("1–604") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) },
        confirmButton = { TextButton(onClick = { page = pageText.toInt(); jump = false }, enabled = pageText.toIntOrNull() in 1..604) { Text(if (english) "Open" else "Buka") } },
        dismissButton = { TextButton(onClick = { jump = false }) { Text(if (english) "Cancel" else "Batal") } })
}
