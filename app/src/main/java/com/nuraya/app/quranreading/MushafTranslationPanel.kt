package com.nuraya.app.quranreading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.data.Verse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A closable companion for the current printed page; never replaces or resizes its image. */
@Composable
fun MushafTranslationPanel(
    verses: List<Verse>,
    selected: Verse,
    surahName: String,
    language: String,
    showLatin: Boolean,
    onLatinChange: (Boolean) -> Unit,
    onSelect: (Verse) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val english = language == "en"
    val context = LocalContext.current
    var textSize by rememberSaveable { mutableFloatStateOf(17f) }
    val listState = rememberLazyListState()
    var latin by remember(verses) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    LaunchedEffect(verses, showLatin) {
        if (showLatin) latin = withContext(Dispatchers.IO) {
            val repository = LatinRepository.shared(context)
            verses.mapNotNull { verse -> repository.verse(verse.surah, verse.ayah)?.latin?.let { verse.number to it } }.toMap()
        }
    }
    LaunchedEffect(verses, selected.number) {
        val index = verses.indexOfFirst { it.number == selected.number }
        if (index >= 0) listState.scrollToItem(index)
    }
    Surface(modifier.testTag("mushaf_translation_panel"), tonalElevation = 2.dp) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (english) "Marmaduke Pickthall" else "Kementerian Agama RI · QuranEnc v1.0.1", style = MaterialTheme.typography.labelMedium)
                    Text("$surahName · ${selected.ayah}", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, if (english) "Close meaning panel" else "Tutup panel arti") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("mushaf_translation_list"), state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(verses, key = { it.number }) { verse ->
                    Row(Modifier.fillMaxWidth().clickable { onSelect(verse) }.testTag("mushaf_meaning_${verse.surah}_${verse.ayah}"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(verse.ayah.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.widthIn(min = 24.dp).padding(top = 4.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (showLatin) latin[verse.number]?.let { text ->
                                Text(text, fontSize = textSize.sp, lineHeight = (textSize * 1.45f).sp,
                                    fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("mushaf_latin_${verse.surah}_${verse.ayah}"))
                            }
                            Text(if (english) verse.english else verse.indonesian, fontSize = textSize.sp, lineHeight = (textSize * 1.45f).sp)
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aa", style = MaterialTheme.typography.labelLarge)
                Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 14f..26f,
                    modifier = Modifier.weight(1f).semantics { contentDescription = if (english) "Translation text size" else "Ukuran terjemahan" })
                Text("Latin", style = MaterialTheme.typography.labelMedium)
                Switch(checked = showLatin, onCheckedChange = onLatinChange, modifier = Modifier.testTag("mushaf_latin_switch")
                    .semantics { contentDescription = if (english) "Show transliteration" else "Tampilkan latin" })
            }
        }
    }
}
