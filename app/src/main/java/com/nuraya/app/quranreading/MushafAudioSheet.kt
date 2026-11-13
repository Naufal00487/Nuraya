package com.nuraya.app.quranreading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.validVerseSpan
import com.nuraya.app.storage.UserSettings
import com.nuraya.app.ui.QariPortrait
import com.nuraya.app.ui.label
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafAudioSheet(app: NurayaApplication, settings: UserSettings, surah: Int, ayah: Int, repeatEach: Int,
                     onDismiss: () -> Unit, onPlay: (Int, Int, Int, Int, String, Int, Int, Boolean) -> Unit) {
    val language = settings.language
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var qari by remember { mutableStateOf(settings.readerQariId) }
    var startSurah by remember { mutableStateOf(surah.toString()) }
    var startAyah by remember { mutableStateOf(ayah.toString()) }
    var endSurah by remember { mutableStateOf(surah.toString()) }
    var endAyah by remember { mutableStateOf(app.quran.surahs[surah - 1].ayahCount.toString()) }
    var each by remember { mutableIntStateOf(repeatEach) }
    var passes by remember { mutableIntStateOf(1) }
    var loop by remember { mutableStateOf(false) }
    val s = startSurah.toIntOrNull() ?: 0; val a = startAyah.toIntOrNull() ?: 0
    val e = endSurah.toIntOrNull() ?: 0; val z = endAyah.toIntOrNull() ?: 0
    val valid = validVerseSpan(app.quran, s, a, e, z, each, passes) && app.playback.verseReciters.any { it.id == qari }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).testTag("mushaf_audio_settings"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label(language, "Pengaturan murottal", "Recitation settings"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, label(language, "Tutup pilihan qari", "Close reciter picker")) }
            }
            Text(label(language, "Mulai dari", "Start at"), style = MaterialTheme.typography.titleSmall)
            VerseInputs(language, startSurah, { startSurah = it }, startAyah, { startAyah = it }, "start")
            Text(app.quran.surahs.getOrNull(s - 1)?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
            Text(label(language, "Sampai", "End at"), style = MaterialTheme.typography.titleSmall)
            VerseInputs(language, endSurah, { endSurah = it }, endAyah, { endAyah = it }, "end")
            Text(app.quran.surahs.getOrNull(e - 1)?.name.orEmpty(), style = MaterialTheme.typography.labelMedium)
            RepeatRow(label(language, "Ulang tiap ayat", "Repeat each verse"), each, each > 1, (each + 1) * passes <= 20, { each-- }, { each++ })
            RepeatRow(label(language, "Putaran rentang", "Range passes"), passes, passes > 1, (passes + 1) * each <= 20, { passes-- }, { passes++ })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label(language, "Ulang terus sampai dijeda", "Loop until paused"), Modifier.weight(1f))
                Switch(loop, { loop = it }, Modifier.testTag("mushaf_audio_loop"))
            }
            if (!valid) Text(label(language, "Periksa surah dan ayat awal–akhir. Kurangi rentang atau pengulangan jika antrean melebihi 12.500 ayat.", "Check the start and end references. Shorten the range or repeats if the queue exceeds 12,500 verses."), color = MaterialTheme.colorScheme.error)
            Button(enabled = valid, onClick = { onPlay(s, a, e, z, qari, each, passes, loop) }, modifier = Modifier.fillMaxWidth().testTag("mushaf_audio_start")) {
                Text(label(language, "Putar rentang", "Play range"))
            }
            HorizontalDivider()
            OutlinedTextField(search, { search = it }, label = { Text(label(language, "Cari nama qari", "Search reciters")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            app.playback.verseReciters.filter { it.name.contains(search, ignoreCase = true) }.forEach { reciter ->
                ListItem(headlineContent = { Text(reciter.name) },
                    leadingContent = { QariPortrait(reciter.id, reciter.name, language, Modifier.size(52.dp)) },
                    supportingContent = { Text(label(language, "Sorotan ayat di halaman · kata di rincian", "Page verse highlights · word highlights in details")) },
                    trailingContent = { RadioButton(selected = qari == reciter.id, onClick = null) },
                    modifier = Modifier.clickable { qari = reciter.id; scope.launch { app.settings.setReaderQari(reciter.id) } })
            }
        }
    }
}

@Composable
private fun VerseInputs(language: String, surah: String, onSurah: (String) -> Unit, ayah: String, onAyah: (String) -> Unit, tag: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(surah, { onSurah(it.filter(Char::isDigit).take(3)) }, label = { Text("Surah 1–114") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f).testTag("audio_${tag}_surah"))
        OutlinedTextField(ayah, { onAyah(it.filter(Char::isDigit).take(3)) }, label = { Text(label(language, "Ayat", "Verse")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f).testTag("audio_${tag}_ayah"))
    }
}

@Composable
private fun RepeatRow(title: String, count: Int, less: Boolean, more: Boolean, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$title: $count×", Modifier.weight(1f))
        TextButton(onClick = onLess, enabled = less) { Text("−") }
        TextButton(onClick = onMore, enabled = more) { Text("+") }
    }
}
