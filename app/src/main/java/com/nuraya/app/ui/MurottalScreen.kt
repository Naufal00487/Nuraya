package com.nuraya.app.ui

import com.nuraya.app.ui.icons.AppIcons as Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.DownloadEntity
import com.nuraya.app.storage.UserEntry
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MurottalScreen(app: NurayaApplication, language: String, qariId: String, onQari: (String) -> Unit,
    entries: List<UserEntry>, downloads: List<DownloadEntity>, playback: PlaybackState, perform: (suspend () -> Unit) -> Unit, onOpenPlayer: () -> Unit = {}) {
    var query by rememberSaveable(qariId) { mutableStateOf("") }
    val qari = app.quran.qaris.firstOrNull { it.id == qariId }
    val savedSettings by app.settings.settings.collectAsState(initial = com.nuraya.app.storage.UserSettings())
    val selectedChapter = com.nuraya.app.media.CuratedReciters.chapterIdFor(savedSettings.readerQariId)
    val choices = app.quran.qariChoices
    val matches = remember(query, qariId) { app.quran.surahs.filter { query.isBlank() || it.name.contains(query,true) || it.arabicName.contains(query) || it.id.toString() == query.trim() } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (qari != null) {
                QariPortrait(qari.id, qari.name, language, Modifier.size(112.dp))
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
            }
            if(qari==null) SectionArtwork("murottal","Murottal",label(language,"Temukan lantunan pilihan Anda.","Find your favourite recitation."))
            else Text(qari.name,style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(if (qari == null) label(language,"${choices.size} pilihan rekaman · 114 surah per pilihan","${choices.size} recitation editions · 114 surahs each") else label(language,"114 surah · pilih untuk mendengarkan","114 surahs · choose one to listen"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = query, onValueChange = { query = it.take(150) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Outlined.Search,null) },
                label = { Text(if (qari == null) label(language,"Cari qari","Search reciters") else label(language,"Cari surah","Search surahs")) })
        }
        if (qari == null) {
            val qaris = choices.filter { it.name.contains(query,true) }
            items(qaris.chunked(2), key = { it.first().id }) { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        Card(onClick={onQari(item.id)},modifier=Modifier.weight(1f).testTag("qari_${item.id}"),shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.fillMaxWidth().padding(vertical=20.dp,horizontal=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                QariPortrait(item.id,item.name,language,Modifier.size(100.dp))
                                Text(item.name,style=MaterialTheme.typography.titleSmall,textAlign=TextAlign.Center)
                                if (item.id == selectedChapter) Text(label(language,"Qari pilihan", "Selected reciter"),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if(row.size==1)Spacer(Modifier.weight(1f))
                }
            }
            if (qaris.isEmpty()) item { EmptyState(Icons.Outlined.SearchOff, label(language,"Qari tidak ditemukan","No reciters found"),label(language,"Coba kata pencarian lain.","Try a different search.")) }
        } else {
            items(matches, key = { it.id }) { surah ->
                val key = "${qari.id}:${surah.id}"
                val favorite = entries.any { it.type == "audio_favorite" && it.key == key }
                val download = downloads.firstOrNull { it.id == key }
                val active = !playback.verseMode && playback.qariId == qari.id && playback.surahId == surah.id
                Surface(shape = RoundedCornerShape(18.dp), color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable { onQari(qari.id); app.playback.play(surah,qari); onOpenPlayer() }.padding(start=16.dp,top=12.dp,bottom=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp),contentAlignment=Alignment.Center) {
                                if (active && playback.isPlaying) Icon(Icons.Outlined.GraphicEq,null) else Text("${surah.id}",style=MaterialTheme.typography.labelLarge)
                            }
                            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                Text(surah.name,style=MaterialTheme.typography.titleMedium)
                                Text(if(download?.status=="complete") label(language,"Tersedia offline","Available offline") else "${surah.ayahCount} ${label(language,"ayat","verses")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={onQari(qari.id); app.playback.play(surah,qari); onOpenPlayer()}) {
                                Icon(Icons.Outlined.PlayArrow,null,Modifier.size(22.dp)); Spacer(Modifier.width(6.dp))
                                Text(label(language,"Dengarkan","Listen"))
                            }
                            OutlinedButton(onClick={perform { app.library.toggle("audio_favorite",key,surah.name,qari.name,qari.id) }}) {
                                Icon(if(favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,null,Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp)); Text(label(language,if(favorite)"Favorit tersimpan" else "Favoritkan",if(favorite)"Favourited" else "Favourite"))
                            }
                            OutlinedButton(onClick={perform{app.downloads.enqueue(surah,qari)}},enabled=download?.status !in listOf("queued","downloading","complete")) {
                                Icon(if(download?.status=="complete")Icons.Outlined.DownloadDone else Icons.Outlined.Download,null,Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp)); Text(when(download?.status) {
                                    "complete" -> label(language,"Tersimpan","Saved")
                                    "queued", "downloading" -> label(language,"Mengunduh","Downloading")
                                    else -> label(language,"Unduh","Download")
                                })
                            }
                        }
                        if(download?.status in listOf("queued","downloading")) LinearProgressIndicator(progress={ (download?.progress ?: 0)/100f },modifier=Modifier.fillMaxWidth())
                    }
                }
            }
            if(matches.isEmpty()) item { EmptyState(Icons.Outlined.SearchOff,label(language,"Surah tidak ditemukan","No surahs found"),label(language,"Coba nama atau nomor surah.","Try a surah name or number.")) }
        }
    }
}

@Composable
fun MiniPlayer(state: PlaybackState, language: String, onOpen: () -> Unit, onToggle: () -> Unit) {
    Surface(tonalElevation=3.dp,color=MaterialTheme.colorScheme.primaryContainer) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick=onOpen).padding(start=18.dp,end=8.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically) {
                QariPortrait(state.qariId, state.qari, language, Modifier.size(48.dp))
                Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
                    Text(state.title,style=MaterialTheme.typography.titleSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(if(state.error!=null)label(language,"Audio belum dapat diputar","Audio playback unavailable") else state.qari,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                if(state.loading) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp)
                IconButton(onClick=onToggle,modifier=Modifier.testTag("mini_player_toggle")) {
                    Icon(if(state.isPlaying)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,label(language,if(state.isPlaying)"Jeda" else "Putar",if(state.isPlaying)"Pause" else "Play"))
                }
                IconButton(onClick=onOpen) { Icon(Icons.Outlined.KeyboardArrowUp,label(language,"Buka pemutar","Open player")) }
            }
            if(state.durationMs>0) LinearProgressIndicator(progress={ (state.positionMs.toFloat()/state.durationMs).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

fun formatDuration(ms:Long):String = String.format(Locale.ROOT,"%d:%02d",(ms/1000)/60,(ms/1000)%60)
fun localizedError(message:String,language:String):String = message.split(" / ",limit=2).let { if(language=="en")it.last() else it.first() }

@Composable fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector,title:String,description:String) {
    Column(Modifier.fillMaxWidth().padding(vertical=32.dp,horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,null,modifier=Modifier.size(44.dp),tint=MaterialTheme.colorScheme.primary)
        Text(title,style=MaterialTheme.typography.titleLarge,textAlign=TextAlign.Center)
        Text(description,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
