package com.nuraya.app.quranreading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuraya.app.ui.label
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Explicit foreground download: completed pages survive cancellation and can be resumed. */
@Composable
fun MushafDownloadDialog(repository: MushafImages, language: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    fun close() { job?.cancel(); onDismiss() }
    AlertDialog(onDismissRequest = { close() }, title = { Text(label(language, "Simpan satu mushaf", "Save this edition")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(repository.title(language))
            Text(label(language, "604 halaman · sekitar ${(repository.totalBytes + 999_999) / 1_000_000} MB. Biarkan halaman ini terbuka sampai selesai. Halaman yang selesai tersimpan tetap tersedia jika unduhan dihentikan.",
                "604 pages · about ${(repository.totalBytes + 999_999) / 1_000_000} MB. Keep this screen open until finished. Completed pages remain saved if you stop."))
            if (running || progress > 0) {
                LinearProgressIndicator(progress = { progress / 604f }, modifier = Modifier.fillMaxWidth())
                Text("$progress / 604")
            }
            if (progress == 604) Text(label(language, "Seluruh halaman tersimpan di HP.", "All pages are saved on this phone."))
            if (error) Text(label(language, "Unduhan terhenti. Periksa koneksi atau ruang penyimpanan, lalu lanjutkan.", "Download stopped. Check connection or storage, then resume."), color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        if (!running && progress < 604) TextButton(onClick = {
            running = true; error = false
            job = scope.launch {
                try { for (page in 1..604) { repository.saveOffline(page); progress = page } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = true }
                finally { running = false }
            }
        }) { Text(label(language, if (error) "Lanjutkan" else "Unduh", if (error) "Resume" else "Download")) }
    }, dismissButton = { TextButton(onClick = { close() }) { Text(label(language, if (running) "Hentikan" else "Tutup", if (running) "Stop" else "Close")) } })
}
