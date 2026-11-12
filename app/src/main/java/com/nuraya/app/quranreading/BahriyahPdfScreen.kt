package com.nuraya.app.quranreading

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuraya.app.ui.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A locally chosen edition: never substitute a Madinah page for a Bahriyah page. */
internal data class BahriyahDocument(val uri: String, val name: String, val page: Int)

internal class BahriyahPdfStore(context: Context) {
    private val preferences = context.getSharedPreferences("bahriyah_pdf", Context.MODE_PRIVATE)

    fun read(): BahriyahDocument? = preferences.getString("uri", null)?.let { uri ->
        BahriyahDocument(uri, preferences.getString("name", null) ?: "Mushaf Bahriyah.pdf",
            preferences.getInt("page", 1).coerceAtLeast(1))
    }

    fun save(document: BahriyahDocument) {
        preferences.edit().putString("uri", document.uri).putString("name", document.name.take(160))
            .putInt("page", document.page.coerceAtLeast(1)).apply()
    }
}

internal fun boundedPdfPage(requested: Int, count: Int): Int = requested.coerceIn(1, count.coerceAtLeast(1))

internal data class PdfFrame(val bitmap: Bitmap, val page: Int, val pageCount: Int)

private suspend fun renderBahriyahPdf(context: Context, uri: Uri, requestedPage: Int, zoom: Float): PdfFrame =
    withContext(Dispatchers.IO) {
        // Open a fresh descriptor for each rendering. Closing a screen never races a renderer in use.
        val descriptor = requireNotNull(context.contentResolver.openFileDescriptor(uri, "r"))
        descriptor.use { file ->
            PdfRenderer(file).use { renderer ->
                require(renderer.pageCount > 0) { "Empty PDF" }
                val selected = boundedPdfPage(requestedPage, renderer.pageCount)
                renderer.openPage(selected - 1).use { page ->
                    require(page.width > 0 && page.height > 0) { "Invalid page dimensions" }
                    val wantedWidth = (1200 * zoom).coerceAtMost(2400f)
                    val scale = minOf(wantedWidth / page.width,
                        sqrt(8_000_000.0 / (page.width.toDouble() * page.height)).toFloat())
                    val bitmap = Bitmap.createBitmap((page.width * scale).roundToInt().coerceAtLeast(1),
                        (page.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    try {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PdfFrame(bitmap, selected, renderer.pageCount)
                    } catch (error: Throwable) {
                        bitmap.recycle()
                        throw error
                    }
                }
            }
        }
    }

/** User supplies the actual Bahriyah/pojok PDF; the file is read on the device, never uploaded. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BahriyahPdfScreen(
    language: String,
    onBack: () -> Unit = {},
    loadPage: suspend (Context, Uri, Int, Float) -> PdfFrame = ::renderBahriyahPdf,
) {
    val context = LocalContext.current
    val store = remember(context) { BahriyahPdfStore(context) }
    var document by remember { mutableStateOf(store.read()) }
    var page by rememberSaveable { mutableIntStateOf(document?.page ?: 1) }
    var pageInput by rememberSaveable { mutableStateOf(page.toString()) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var reload by remember { mutableIntStateOf(0) }
    var frame by remember { mutableStateOf<PdfFrame?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf(false) }
    var controls by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importError = false
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    require(uri.scheme == "content")
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "Mushaf Bahriyah.pdf"
                    BahriyahDocument(uri.toString(), name.take(160), 1)
                }.getOrNull()
            }
            if (imported == null) importError = true else {
                store.save(imported)
                document = imported
                page = 1
                pageInput = "1"
                zoom = 1f
                reload++
            }
        }
    }
    LaunchedEffect(document?.uri, page, zoom, reload) {
        val current = document ?: return@LaunchedEffect
        loading = true
        error = false
        frame = null
        try {
            val rendered = loadPage(context, Uri.parse(current.uri), page, zoom)
            frame = rendered
            pageInput = rendered.page.toString()
            store.save(current.copy(page = rendered.page))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        } finally {
            loading = false
        }
    }
    val currentDocument = document
    BackHandler(controls) { controls = false }
    if (currentDocument != null && !error) {
        val threshold = with(LocalDensity.current) { 64.dp.toPx() }
        BoxWithConstraints(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White).testTag("bahriyah_pdf_screen")) {
            val availableWidth = maxWidth
            Box(Modifier.fillMaxSize().testTag("bahriyah_pdf_viewport")
                .semantics {
                    onClick(label(language, "Tampilkan menu baca", "Show reading menu")) { controls = true; true }
                    customActions = listOf(
                        CustomAccessibilityAction(label(language, "Halaman berikutnya", "Next page")) { if (page < (frame?.pageCount ?: page)) { page++; true } else false },
                        CustomAccessibilityAction(label(language, "Halaman sebelumnya", "Previous page")) { if (page > 1) { page--; true } else false },
                    )
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }) }
                .pointerInput(page, zoom, frame?.pageCount) {
                    if (zoom == 1f) {
                        var drag = 0f
                        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                            if (drag > threshold && page < (frame?.pageCount ?: page)) page++
                            else if (drag < -threshold && page > 1) page--
                        }, onHorizontalDrag = { change, amount -> change.consume(); drag += amount })
                    }
                }, contentAlignment = Alignment.Center) {
                val visible = frame
                if (visible == null || loading) CircularProgressIndicator()
                else if (zoom == 1f) Image(visible.bitmap.asImageBitmap(),
                    label(language, "Halaman PDF ${visible.page}, ${currentDocument.name}", "PDF page ${visible.page}, ${currentDocument.name}"),
                    Modifier.fillMaxSize().testTag("bahriyah_pdf_page"), contentScale = ContentScale.Fit)
                else {
                    val width = availableWidth * zoom
                    Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                        Image(visible.bitmap.asImageBitmap(), label(language, "Halaman PDF $page", "PDF page $page"),
                            Modifier.width(width).height(width * visible.bitmap.height.toFloat() / visible.bitmap.width).testTag("bahriyah_pdf_page"), contentScale = ContentScale.Fit)
                    }
                }
            }
            if (controls) {
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(MaterialTheme.colorScheme.surface), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { controls = false; onBack() }, modifier = Modifier.testTag("back_button")) { Text(label(language, "Kembali", "Back")) }
                    Text(currentDocument.name, Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 300.dp).background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState()).padding(12.dp).testTag("bahriyah_pdf_controls")) {
                    TextButton(onClick = { controls = false }, modifier = Modifier.align(Alignment.End)) { Text(label(language, "Tutup menu baca", "Close reading menu")) }
                    Text(label(language, "Halaman PDF $page / ${frame?.pageCount ?: "…"}", "PDF page $page / ${frame?.pageCount ?: "…"}"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { page-- }, enabled = page > 1) { Text(label(language, "Sebelumnya", "Previous")) }
                        OutlinedButton(onClick = { page++ }, enabled = page < (frame?.pageCount ?: page)) { Text(label(language, "Berikutnya", "Next")) }
                        TextButton(onClick = { zoom = (zoom - .5f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text("−") }
                        TextButton(onClick = { zoom = (zoom + .5f).coerceAtMost(3f) }, enabled = zoom < 3f) { Text("+") }
                    }
                    OutlinedTextField(pageInput, { pageInput = it.filter(Char::isDigit).take(6) }, label = { Text(label(language, "Nomor halaman PDF", "PDF page number")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    TextButton(onClick = { page = boundedPdfPage(pageInput.toIntOrNull() ?: page, frame?.pageCount ?: page); controls = false }) { Text(label(language, "Buka halaman", "Go to page")) }
                    TextButton(onClick = { launcher.launch(arrayOf("application/pdf")) }, modifier = Modifier.testTag("bahriyah_choose_pdf")) { Text(label(language, "Pilih PDF lain", "Choose another PDF")) }
                }
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("bahriyah_pdf_screen"), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TextButton(onClick = { controls = false; onBack() }, modifier = Modifier.testTag("back_button")) { Text(label(language, "Kembali", "Back")) }
            Text(label(language, "Mushaf Bahriyah / Al-Qur’an pojok", "Bahriyah / corner-verse mushaf"),
                style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(label(language,
                "Buka PDF Mushaf Bahriyah milik Anda untuk membaca susunan halaman edisi tersebut. Halaman Bahriyah belum disertakan di Nuraya.",
                "Open your own Bahriyah PDF to read that edition’s original page layout. Bahriyah pages are not bundled with Nuraya."))
            Spacer(Modifier.height(8.dp))
            Text(label(language, "PDF dibaca di perangkat dan tidak diunggah. Jika tersedia, pilih berkas yang sudah tersimpan agar dapat dibaca tanpa internet.",
                "The PDF is read on your device and is not uploaded. Choose a downloaded file to read without internet."),
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Button(onClick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bahriyah_choose_pdf")) {
                Text(label(language, if (document == null) "Buka PDF Mushaf Bahriyah" else "Pilih PDF lain",
                    if (document == null) "Open Bahriyah PDF" else "Choose another PDF"))
            }
            if (importError) Text(label(language, "Berkas belum dapat diakses. Pilih PDF dari penyimpanan perangkat.",
                "This file could not be accessed. Choose a PDF in device storage."), color = MaterialTheme.colorScheme.error)
        }
        document?.let { selected ->
            item { Text(selected.name, style = MaterialTheme.typography.titleMedium) }
            if (loading) item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(label(language, "Membuka halaman…", "Opening page…"))
            }
            if (error) item {
                Text(label(language,
                    "PDF tidak dapat dibuka. Berkas mungkin telah dipindah, belum diunduh, rusak, atau dilindungi sandi. Coba lagi atau pilih PDF lain yang dapat dibuka tanpa sandi.",
                    "The PDF could not be opened. It may have moved, not finished downloading, be damaged, or be password protected. Retry or choose another PDF that opens without a password."),
                    color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { reload++ }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(label(language, "Coba lagi", "Retry"))
                }
            }
        }
    }
}
