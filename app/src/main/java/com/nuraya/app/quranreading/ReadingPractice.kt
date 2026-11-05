package com.nuraya.app.quranreading

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nuraya.app.data.Verse
import com.nuraya.app.study.StudyVerse
import com.nuraya.app.ui.TajwidLegend
import com.nuraya.app.ui.label

/** Local counters record the user's own practice, not an automatic claim of memorization. */
class TikrarProgress(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nuraya_tikrar", Context.MODE_PRIVATE)
    fun count(surah: Int, ayah: Int): Int = prefs.getInt(key(surah, ayah), 0).coerceIn(0, 999)
    fun increment(surah: Int, ayah: Int): Int = (count(surah, ayah) + 1).coerceAtMost(999).also {
        prefs.edit().putInt(key(surah, ayah), it).apply()
    }
    fun reset(surah: Int, ayah: Int) { prefs.edit().remove(key(surah, ayah)).apply() }
    private fun key(surah: Int, ayah: Int): String {
        require(surah in 1..114 && ayah in 1..286)
        return "$surah:$ayah"
    }
}

@Composable
fun TikrarCounter(verse: Verse, language: String, target: Int) {
    val context = LocalContext.current
    val store = remember(context) { TikrarProgress(context) }
    var count by remember(verse.surah, verse.ayah) { mutableIntStateOf(store.count(verse.surah, verse.ayah)) }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label(language, "Latihan mandiri: $count / $target kali", "My practice: $count / $target times"), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("tikrar_count_${verse.surah}_${verse.ayah}"))
            LinearProgressIndicator(progress = { (count.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text(label(language, "Tekan setelah Anda membaca ayat. Putaran audio tidak menambah hitungan ini.",
                "Tap after you recite. Audio repeats do not change this counter."), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { count = store.increment(verse.surah, verse.ayah) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("tikrar_increment_${verse.surah}_${verse.ayah}")) {
                Text(label(language, "Sudah membaca · tambah 1", "I recited · add 1"))
            }
            TextButton(onClick = { store.reset(verse.surah, verse.ayah); count = 0 }) { Text(label(language, "Mulai hitungan baru", "Start a new count")) }
        }
    }
}

@Composable
fun LatinTajwidPanel(verse: Verse, study: StudyVerse?, language: String, dark: Boolean) {
    val context = LocalContext.current
    val repository = remember(context) { LatinRepository.shared(context) }
    var transliteration by remember(verse.surah, verse.ayah) { mutableStateOf(verse.transliteration) }
    var loaded by remember(verse.surah, verse.ayah) { mutableStateOf(!verse.transliteration.isNullOrBlank()) }
    LaunchedEffect(verse.surah, verse.ayah, repository) {
        if (verse.transliteration.isNullOrBlank()) {
            transliteration = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { repository.verse(verse.surah, verse.ayah)?.latin }.getOrNull()
            }
        }
        loaded = true
    }
    val text = transliteration
    if (text.isNullOrBlank()) {
        Text(if (!loaded) label(language, "Memuat bantuan Latin…", "Loading transliteration…")
            else label(language, "Bantuan Latin belum dapat dibuka untuk ayat ini.", "Transliteration could not be opened for this verse."), style = MaterialTheme.typography.bodyMedium)
        return
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label(language, "Bantuan Latin", "Transliteration aid"), style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("latin_${verse.surah}_${verse.ayah}"))
            Text(repository.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label(language, "Warna tajwid berada pada huruf Arab. Latin bukan pengganti makhraj; cocokkan dengan audio qari dan bimbingan guru.",
                "Tajwid colors apply to Arabic letters. Transliteration cannot represent articulation fully; compare with the reciter and a teacher."), style = MaterialTheme.typography.bodySmall)
            if (study?.tajwidSpans?.isNotEmpty() == true) TajwidLegend(language, dark)
        }
    }
}

@Composable
fun WaqafVerseGuide(verse: Verse, language: String, annotations: List<StopAnnotation> = emptyList(), verified: Boolean = true) {
    if (!verified) Text(label(language, "Tanda berhenti ayat ini belum dapat dicocokkan dengan sumber; periksa mushaf dan guru.", "This verse’s stop signs could not be aligned with the source; consult a mushaf and teacher."), style = MaterialTheme.typography.bodySmall)
    val marks = remember(verse.arabic, annotations) {
        WaqafGuide.signs.filter { sign -> annotations.any { it.mark == sign.mark } } + WaqafGuide.inText(verse.arabic)
    }.distinctBy { it.mark }
    if (marks.isEmpty()) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label(language, "Tanda pada ayat ini", "Signs in this verse"), style = MaterialTheme.typography.titleMedium)
            marks.forEach { sign ->
                Text(sign.name, style = MaterialTheme.typography.titleMedium)
                Text(sign.explanation, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun WaqafLearningGuide(language: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(label(language, "Waqaf & ibtida", "Stopping & resuming")) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text(label(language, "Berhenti di akhir ayat umumnya diperbolehkan. Saat memulai lagi (ibtida), pilih rangkaian kata yang maknanya utuh; jangan memulai dari bagian yang merusak maksud ayat. Latihkan bersama guru.",
                    "Stopping at verse endings is generally allowed. When resuming, choose words that preserve the complete meaning. Practise with a teacher.")) }
                items(WaqafGuide.signs.size) { index ->
                    val sign = WaqafGuide.signs[index]
                    Text(sign.name, style = MaterialTheme.typography.titleMedium)
                    Text(sign.explanation)
                }
                item { Text(label(language, "Penanda bersumber dari Quran Uthmani Islamic Network dan hanya ditampilkan jika huruf serta batas katanya cocok. Letak tanda dapat berbeda antar mushaf. Huruf dan nomor ayat tidak diubah.",
                    "Signs come from Islamic Network’s Uthmani source and appear only when verse letters and word boundaries align. Editions may differ. Verse letters and numbering are unchanged."), style = MaterialTheme.typography.bodySmall) }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(label(language, "Kembali membaca", "Return to reading")) } })
}

@Composable
fun MemorizationRangeDialog(
    language: String, verses: List<Verse>, juz: Boolean, initialStart: Int, initialEnd: Int,
    onDismiss: () -> Unit, onSelect: (Int, Int) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf(initialStart.toString()) }
    var end by rememberSaveable { mutableStateOf(initialEnd.toString()) }
    val startNumber = start.toIntOrNull()
    val endNumber = end.toIntOrNull()
    val valid = startNumber != null && endNumber != null && startNumber in 1..verses.size && endNumber in startNumber..verses.size
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(label(language, "Pilih rentang hafalan", "Choose a memorization range")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (juz) label(language, "Masukkan urutan ayat dalam juz: 1–${verses.size}. Referensi aslinya ditampilkan di bawah.", "Enter verse positions within this juz: 1–${verses.size}. Original references appear below.")
                else label(language, "Masukkan nomor ayat: 1–${verses.size}.", "Enter verse numbers: 1–${verses.size}."))
            OutlinedTextField(start, { start = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth().testTag("memorization_range_start"),
                label = { Text(label(language, "Mulai", "Start")) }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            OutlinedTextField(end, { end = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth().testTag("memorization_range_end"),
                label = { Text(label(language, "Sampai", "End")) }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            if (valid) {
                val first = verses[startNumber!! - 1]
                val last = verses[endNumber!! - 1]
                Text("${first.surah}:${first.ayah} – ${last.surah}:${last.ayah}")
            } else Text(label(language, "Nomor akhir harus sama atau lebih besar dari awal, dalam rentang bacaan.", "The end must not precede the start; both must be within this reading range."), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSelect(requireNotNull(startNumber), requireNotNull(endNumber)) }, modifier = Modifier.testTag("memorization_range_apply")) { Text(label(language, "Bagi menjadi blok", "Create blocks")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(label(language, "Batal", "Cancel")) } })
}
