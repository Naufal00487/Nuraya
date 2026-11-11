package com.nuraya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.data.Qari
import com.nuraya.app.data.Surah
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.DownloadEntity
import com.nuraya.app.storage.UserEntry
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MurottalScreen(app: NurayaApplication, language: String, qariId: String, onQari: (String) -> Unit,
    entries: List<UserEntry>, downloads: List<DownloadEntity>, playback: PlaybackState, perform: (suspend () -> Unit) -> Unit) {
    var query by rememberSaveable(qariId) { mutableStateOf("") }
    val qari = app.quran.qaris.firstOrNull { it.id == qariId }
    val choices = app.quran.qariChoices
    val matches = remember(query, qariId) { app.quran.surahs.filter { query.isBlank() || it.name.contains(query,true) || it.arabicName.contains(query) || it.id.toString() == query.trim() } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (qari != null) {
                QariPortrait(qari.id, qari.name, language, Modifier.size(112.dp))
                Spacer(Modifier.height(8.dp))
                QariPortraitCredit(qari.id, language)
                Spacer(Modifier.height(12.dp))
            }
            if(qari==null) SectionArtwork("murottal","Murottal",label(language,"Temukan lantunan pilihan Anda.","Find your favourite recitation."))
            else Text(qari.name,style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(if (qari == null) label(language,"${choices.size} pilihan rekaman · 114 surah per pilihan","${choices.size} recitation editions · 114 surahs each") else label(language,"114 surah · pilih untuk mendengarkan","114 surahs · choose one to listen"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (qari != null) Text(qari.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = query, onValueChange = { query = it.take(150) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Outlined.Search,null) },
                label = { Text(if (qari == null) label(language,"Cari qari","Search reciters") else label(language,"Cari surah","Search surahs")) })
        }
        if(qari!=null) item {
            QariAboutCard(qari,language)
        }
        if (qari != null) item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Headphones, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(label(language,
                        "Ketuk surah untuk memutar melalui internet. Gunakan tombol unduh agar rekaman qari ini dapat didengarkan offline setelah unduhan selesai.",
                        "Tap a surah to stream over the internet. Use the download button to listen to this reciter offline once the download finishes."),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
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
                            }
                        }
                    }
                    if(row.size==1)Spacer(Modifier.weight(1f))
                }
            }
            if (qaris.isEmpty()) item { EmptyState(Icons.Outlined.SearchOff, label(language,"Qari tidak ditemukan","No reciters found"),label(language,"Coba kata pencarian lain.","Try a different search.")) }
            item { Text(label(language,"Foto berasal dari Quran.com dan dokumen penyusun. Foto yang belum tersedia atau perlu verifikasi ditampilkan dengan inisial.", "Photographs come from Quran.com and the maintainer’s document. Unavailable or unconfirmed photographs use initials."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Text(label(language,"Sumber rekaman: Islamic Network. Internet diperlukan untuk streaming. Audio yang selesai diunduh dapat diputar offline.","Recordings from Islamic Network. Streaming requires internet. Completed downloads can be played offline."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(matches, key = { it.id }) { surah ->
                val key = "${qari.id}:${surah.id}"
                val favorite = entries.any { it.type == "audio_favorite" && it.key == key }
                val download = downloads.firstOrNull { it.id == key }
                val active = !playback.verseMode && playback.qariId == qari.id && playback.surahId == surah.id
                Surface(shape = RoundedCornerShape(18.dp), color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable { app.playback.play(surah,qari) }.padding(start=16.dp,top=12.dp,bottom=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp),contentAlignment=Alignment.Center) {
                                if (active && playback.isPlaying) Icon(Icons.Outlined.GraphicEq,null) else Text("${surah.id}",style=MaterialTheme.typography.labelLarge)
                            }
                            Column(Modifier.weight(1f).padding(horizontal=12.dp)) {
                                Text(surah.name,style=MaterialTheme.typography.titleMedium)
                                Text(if(download?.status=="complete") label(language,"Tersedia offline","Available offline") else "${surah.ayahCount} ${label(language,"ayat","verses")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Button(onClick={app.playback.play(surah,qari)}) {
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FullPlayer(app: NurayaApplication, language: String, state: PlaybackState, entries: List<UserEntry>, downloads: List<DownloadEntity>, perform: (suspend () -> Unit) -> Unit, onClose: () -> Unit, onRead: (Int, Int) -> Unit = { _, _ -> }) {
    val surah = app.quran.surahs.firstOrNull { it.id==state.surahId }
    val qari = app.quran.qaris.firstOrNull { it.id==state.qariId }
    val key = "${state.qariId}:${state.surahId}"
    val favorite = entries.any { it.type=="audio_favorite" && it.key==key }
    val inCollection = entries.any { it.type=="playlist" && it.key==key }
    val download = downloads.firstOrNull { it.id==key }
    var seeking by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label(language,"SEDANG DIDENGARKAN","NOW LISTENING"),style=MaterialTheme.typography.labelMedium,letterSpacing=2.sp,color=MaterialTheme.colorScheme.secondary)
        QariPortrait(state.qariId, state.qari, language, Modifier.size(144.dp))
        QariPortraitCredit(state.qariId, language)
        Text(surah?.arabicName ?: "Nuraya",fontFamily=QuranArabicFont,fontSize=38.sp,color=MaterialTheme.colorScheme.primary,textAlign=TextAlign.Center)
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(state.title,style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
            Text(state.qari,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
            if (!state.verseMode && download?.status=="complete") Text(label(language,"Audio tersimpan di perangkat","Audio saved on this device"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
            if (state.verseMode) {
                Text(label(language, "Ayat ${state.ayahId} · rentang ${state.rangeStart}–${state.rangeEnd}", "Verse ${state.ayahId} · range ${state.rangeStart}–${state.rangeEnd}"), style = MaterialTheme.typography.bodySmall)
                if(state.verseMode&&state.rangePasses>1)Text(label(language,"Putaran ${state.rangePass} / ${state.rangePasses}","Pass ${state.rangePass} / ${state.rangePasses}"),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onRead(state.surahId, state.ayahId.coerceAtLeast(1)) }) {
                    Icon(Icons.Outlined.MenuBook, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label(language, "Ikuti teks bacaan", "Follow the recitation text"))
                }
            }
        }
        if(state.error!=null) Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(localizedError(state.error,language),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)
                if (state.verseMode) TextButton(onClick = {
                    app.playback.playVerseRange(state.surahId, state.ayahId.coerceAtLeast(1), state.rangeEnd.coerceAtLeast(state.ayahId), state.qariId)
                }) { Text(label(language, "Coba lagi", "Try again")) }
                else if(surah!=null && qari!=null) TextButton(onClick={app.playback.play(surah,qari)}) { Text(label(language,"Coba lagi","Try again")) }
            }
        }
        Column {
            Slider(value=seeking ?: state.positionMs.toFloat().coerceIn(0f,state.durationMs.coerceAtLeast(1).toFloat()),
                onValueChange={seeking=it},onValueChangeFinished={seeking?.let{app.playback.seekTo(it.toLong())};seeking=null},
                valueRange=0f..state.durationMs.coerceAtLeast(1).toFloat(),enabled=state.durationMs>0)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(formatDuration(state.positionMs),style=MaterialTheme.typography.labelMedium);Text(formatDuration(state.durationMs),style=MaterialTheme.typography.labelMedium) }
        }
        Button(onClick={app.playback.toggle()},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {
            if(state.loading)CircularProgressIndicator(Modifier.size(26.dp),color=MaterialTheme.colorScheme.onPrimary,strokeWidth=2.dp)
            else Icon(if(state.isPlaying)Icons.Outlined.Pause else Icons.Outlined.PlayArrow,null,modifier=Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp)); Text(label(language,if(state.isPlaying)"Jeda" else "Putar",if(state.isPlaying)"Pause" else "Play"))
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick={app.playback.previous()}) {
                Icon(Icons.Outlined.SkipPrevious,null,Modifier.size(24.dp)); Spacer(Modifier.width(6.dp))
                Text(label(language,"Sebelumnya","Previous"))
            }
            OutlinedButton(onClick={app.playback.next()}) {
                Text(label(language,"Berikutnya","Next")); Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.SkipNext,null,Modifier.size(24.dp))
            }
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilterChip(selected=state.repeat,onClick={app.playback.setRepeat(!state.repeat)},label={Text(label(language,"Ulangi","Repeat"))},leadingIcon={Icon(Icons.Outlined.RepeatOne,null,Modifier.size(18.dp))})
            AssistChip(onClick={ val speeds=listOf(.75f,1f,1.25f,1.5f,2f);app.playback.setSpeed(speeds[(speeds.indexOf(state.speed)+1).mod(speeds.size)]) },label={Text("${state.speed}×")},leadingIcon={Icon(Icons.Outlined.Speed,null,Modifier.size(18.dp))})
            if (!state.verseMode) OutlinedButton(onClick={if(surah!=null && qari!=null)perform { app.library.toggle("audio_favorite",key,surah.name,qari.name,qari.id) }}) {
                Icon(if(favorite)Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,null,Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp)); Text(label(language,if(favorite)"Favorit tersimpan" else "Favoritkan",if(favorite)"Favourited" else "Favourite"))
            }
        }
        if(!state.verseMode && surah!=null && qari!=null) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={perform{app.downloads.enqueue(surah,qari)}},enabled=download?.status !in listOf("complete","queued","downloading"),modifier=Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                    Text(if(download?.status=="complete")label(language,"Tersimpan","Saved") else label(language,"Unduh","Download"))
                }
                OutlinedButton(onClick={perform{app.library.toggle("playlist",key,surah.name,qari.name,qari.id)}},modifier=Modifier.weight(1f)) {
                    Icon(if(inCollection)Icons.Outlined.PlaylistAddCheck else Icons.Outlined.PlaylistAdd,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(label(language,"Koleksi","Collection"))
                }
            }
            if(download?.status in listOf("queued","downloading")) Text("${label(language,"Mengunduh","Downloading")} ${download?.progress ?: 0}%",style=MaterialTheme.typography.bodySmall)
        }
        Text(if (state.verseMode) "${state.source}\n${label(language, "Mode sinkron · membutuhkan internet", "Synchronized mode · requires internet")}" else "Islamic Network · alquran.cloud",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        TextButton(onClick={app.playback.stop();onClose()}) { Text(label(language,"Tutup pemutar","Close player")) }
        Spacer(Modifier.height(12.dp))
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


@Composable
private fun QariAboutCard(qari:Qari,language:String){
    var expanded by rememberSaveable(qari.id){mutableStateOf(true)}
    OutlinedCard(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().testTag("qari_about")) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(label(language,"Tentang qari & rekaman","About this reciter & recording"),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                IconButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,label(language,"Rincian","Details"))}
            }
            if(expanded){
                Text(qari.name,style=MaterialTheme.typography.titleLarge)
                Text(qari.description,style=MaterialTheme.typography.bodyLarge)
                Text(label(language,"Katalog rekaman Al-Qur’an berbahasa Arab dengan 114 surah. Pilih surah di bawah untuk mendengarkan lantunan atau menyimpannya di perangkat.","An Arabic Qur’an recording collection covering 114 surahs. Choose a surah below to listen or save it on your device."),style=MaterialTheme.typography.bodyMedium)
                Text(label(language,"Sumber rekaman: Islamic Network · alquran.cloud. Keterangan edisi mengikuti metadata penyedia.","Recording source: Islamic Network · alquran.cloud. Edition descriptions follow provider metadata."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
