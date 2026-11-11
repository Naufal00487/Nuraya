package com.nuraya.app.quranreading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.ui.label
import com.nuraya.app.ui.BrandImage
import com.nuraya.app.ui.SectionArtwork

@Composable
fun QuranModesScreen(app: NurayaApplication, language: String, onMode: (String) -> Unit, onRead: (Int, Int) -> Unit) {
    var thematicOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(thematicOpen) { thematicOpen = false }
    if (thematicOpen) {
        ThematicQuranScreen(app, language, onBack = { thematicOpen = false }, onRead = onRead)
        return
    }
    val filtered = QuranReadingModes.all.filter { query.isBlank() ||
        (it.titleId + " " + it.titleEn + " " + it.descriptionId).contains(query.trim(), ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize().testTag("quran_modes"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionArtwork("quran","Al-Qur’an",label(language,"Baca, pahami, dan dekatkan hati.","Read, understand, and reconnect."))
            Spacer(Modifier.height(18.dp))
            Text(label(language, "Pilih cara membaca", "Choose how to read"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(label(language, "Mulai dengan Al-Qur’an per ayat, atau pilih bantuan belajar yang Anda perlukan.",
                "Start with verse-by-verse reading, or choose the learning aid you need."), style = MaterialTheme.typography.bodyLarge)
        }
        item {
            OutlinedTextField(value = query, onValueChange = { query = it.take(100) }, singleLine = true,
                label = { Text(label(language, "Cari cara membaca", "Find a reading mode")) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth().testTag("quran_mode_search"))
        }
        items(filtered, key = { it.id }) { mode ->
            OutlinedCard(onClick = { if (mode.id == "thematic") thematicOpen = true else onMode(mode.id) },
                modifier = Modifier.fillMaxWidth().testTag("quran_mode_${mode.id}"), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BrandImage("covers/${mode.id}",Modifier.width(94.dp).height(132.dp),description=mode.title(language))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(mode.title(language), style = MaterialTheme.typography.titleLarge)
                        Text(mode.description(language), style = MaterialTheme.typography.bodyMedium)
                        Text(label(language, "Buka bacaan", "Open reading"), style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
        if (filtered.isEmpty()) item { Text(label(language, "Tidak ada pilihan yang cocok. Coba “tajwid”, “Latin”, atau “hafalan”.", "No matching mode. Try tajwid, transliteration or memorization.")) }
        item {
            Text(label(language, "Teks ayat, nomor, terjemahan, serta basmalah tetap mengikuti sumber bacaan. Pilihan ini mengubah bantuan membaca, bukan isi Al-Qur’an.",
                "Verse text, numbering, translations and basmalahs retain their source. These options change reading aids, not the Qur’an itself."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ThematicQuranScreen(app: NurayaApplication, language: String, onBack: () -> Unit, onRead: (Int, Int) -> Unit) {
    var topicId by rememberSaveable { mutableStateOf<String?>(null) }
    val topic = QuranThemes.all.firstOrNull { it.id == topicId }
    BackHandler(topic != null) { topicId = null }
    LazyColumn(Modifier.fillMaxSize().testTag("quran_thematic"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            OutlinedButton(onClick = { if (topic != null) topicId = null else onBack() }, modifier = Modifier.heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(8.dp))
                Text(label(language, if (topic != null) "Daftar tema" else "Cara membaca", if (topic != null) "All themes" else "Reading modes"))
            }
            Spacer(Modifier.height(12.dp))
            Text(topic?.title(language) ?: label(language, "Terjemah tematik", "Thematic translations"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(label(language, "Pilihan rujukan ayat per tema. Buka ayat untuk membaca konteks dan tafsirnya.",
                "Selected verse references by theme. Open each verse to read its context and commentary."), style = MaterialTheme.typography.bodyLarge)
        }
        if (topic == null) items(QuranThemes.all, key = { it.id }) { theme ->
            OutlinedCard(onClick = { topicId = theme.id }, modifier = Modifier.fillMaxWidth().testTag("quran_theme_${theme.id}")) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(theme.title(language), style = MaterialTheme.typography.titleLarge)
                    Text(label(language, "${theme.references.size} rujukan ayat · buka", "${theme.references.size} verse references · open"))
                }
            }
        } else items(topic.references, key = { "${it.first}:${it.second}" }) { (surah, ayah) ->
            val verse = remember(surah, ayah) { app.quran.verse(surah, ayah) }
            if (verse != null) OutlinedCard(onClick = { onRead(surah, ayah) }, modifier = Modifier.fillMaxWidth().testTag("theme_verse_${surah}_$ayah")) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${app.quran.surahs[surah - 1].name} · $surah:$ayah", style = MaterialTheme.typography.titleLarge)
                    Text(verse.indonesian, style = MaterialTheme.typography.bodyLarge)
                    Text(label(language, "Baca ayat & tafsir", "Read verse & commentary"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text(label(language, "Terjemahan: Kementerian Agama RI · QuranEnc. Pengelompokan tema adalah indeks pilihan Nuraya, bukan tafsir baru atau daftar seluruh ayat tentang tema tersebut.",
            "Translation: Indonesian Ministry of Religious Affairs · QuranEnc. Themes are a selected Nuraya index, not a new commentary or an exhaustive list."), style = MaterialTheme.typography.bodySmall) }
    }
}
