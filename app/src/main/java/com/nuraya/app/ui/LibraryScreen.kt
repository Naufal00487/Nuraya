package com.nuraya.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.storage.LibraryRepository
import com.nuraya.app.storage.DownloadEntity
import com.nuraya.app.storage.UserEntry
import com.nuraya.app.storage.resolveVerseAudioHistory
import com.nuraya.app.media.VerseAudioCatalog
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(app: NurayaApplication,language:String,entries:List<UserEntry>,downloads:List<DownloadEntity>,perform:(suspend ()->Unit)->Unit,onRead:(Int,Int)->Unit,
    onPlayVerseHistory: ((Int, Int, String) -> Unit)? = null) {
    var filter by rememberSaveable { mutableStateOf("bookmark") }
    var deleteDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var clearHistory by remember { mutableStateOf(false) }
    var entryUnavailable by remember { mutableStateOf(false) }
    val verseQariIds = remember(app) { VerseAudioCatalog(app).reciters.map { it.id }.toSet() }
    val categories = listOf(
        "bookmark" to label(language,"Penanda","Bookmarks"),
        "audio_favorite" to label(language,"Favorit","Favourites"),
        "playlist" to label(language,"Daftar putar","Playlist"),
        "download" to label(language,"Unduhan","Downloads"),
        "history" to label(language,"Riwayat","History")
    )
    LaunchedEffect(filter) { if (categories.none { it.first == filter }) filter = "bookmark" }
    val selected = entries.filter { entry ->
        entry.type in LibraryRepository.entryTypes &&
            if (filter == "history") entry.type.endsWith("_history") else entry.type == filter
    }
    val openEntry: (UserEntry)->Unit = { entry ->
        entryUnavailable = false
        when(entry.type) {
            "bookmark","read_history" -> {
                val parts=entry.key.split(":");val surah=parts.getOrNull(0)?.toIntOrNull();val verse=parts.getOrNull(1)?.toIntOrNull()
                if(surah!=null && verse!=null && app.quran.verse(surah,verse)!=null)onRead(surah,verse)
                else entryUnavailable = true
            }
            "verse_audio_history" -> {
                val reference = resolveVerseAudioHistory(entry, verseQariIds) { surah, ayah -> app.quran.verse(surah, ayah) != null }
                if (reference == null) entryUnavailable = true else {
                    if (onPlayVerseHistory != null) onPlayVerseHistory(reference.surah, reference.ayah, reference.qariId)
                    else app.playback.playVerseRange(reference.surah, reference.ayah, reference.ayah, reference.qariId)
                }
            }
            else -> {
                val surah=app.quran.surahs.firstOrNull{it.id==entry.key.substringAfterLast(":").toIntOrNull()}
                val qari=app.quran.qaris.firstOrNull{it.id==entry.extra}
                if(surah!=null && qari!=null)app.playback.play(surah,qari)
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Text(label(language,"Koleksi dan unduhan Anda","Your collection and downloads"),Modifier.padding(horizontal=20.dp,vertical=12.dp),style=MaterialTheme.typography.headlineMedium)
        FlowRow(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            categories.forEach{(key,title)->FilterChip(selected=filter==key,onClick={filter=key},label={Text(title)},modifier=Modifier.heightIn(min=48.dp).testTag("library_filter_$key"))}
        }
        if (entryUnavailable) Text(label(language,"Rujukan atau qari pada riwayat ini tidak tersedia. Pilih kembali bacaan atau qari dari menu Al-Qur’an.",
            "This saved reference or reciter is unavailable. Choose the verse or reciter again from the Qur’an menu."),
            modifier=Modifier.padding(horizontal=20.dp).testTag("library_entry_unavailable"),color=MaterialTheme.colorScheme.error)
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(filter=="download") {
                item {
                    val bytes=downloads.filter{it.status=="complete"}.sumOf{File(it.filePath).takeIf{f->f.isFile}?.length() ?: 0L}
                    Text("${downloads.count{it.status=="complete"}} ${label(language,"audio offline","offline recordings")} · ${String.format(Locale.ROOT,"%.1f",bytes/1048576.0)} MB",style=MaterialTheme.typography.titleMedium)
                    Text(label(language,"Tersimpan khusus di aplikasi. Menghapus aplikasi juga menghapus unduhannya.","Stored privately in this app. Uninstalling the app also removes downloads."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(downloads.isEmpty()) item { EmptyState(Icons.Outlined.Download,label(language,"Belum ada unduhan","No downloads yet"),label(language,"Unduh surah dari Murottal untuk didengarkan tanpa internet.","Download a surah from Murottal to listen without internet.")) }
                items(downloads,key={it.id}) { item ->
                    Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title,style=MaterialTheme.typography.titleMedium)
                                    Text(downloadStatus(item,language),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                if(item.status=="complete")FilledTonalButton(onClick={
                                    val surah=app.quran.surahs.firstOrNull{it.id==item.surahId};val qari=app.quran.qaris.firstOrNull{it.id==item.qariId}
                                    if(surah!=null && qari!=null)app.playback.play(surah,qari)
                                },modifier=Modifier.heightIn(min=52.dp)){
                                    Icon(Icons.Outlined.PlayArrow,null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(label(language,"Putar","Play"))
                                }
                                OutlinedButton(onClick={deleteDownload=item},modifier=Modifier.heightIn(min=52.dp)){
                                    Icon(Icons.Outlined.DeleteOutline,null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(label(language,"Hapus","Delete"))
                                }
                            }
                            if(item.status in listOf("queued","downloading")) {
                                LinearProgressIndicator(progress={item.progress/100f},modifier=Modifier.fillMaxWidth())
                                TextButton(onClick={perform{app.downloads.cancel(item.id)}}){Text(label(language,"Batalkan","Cancel"))}
                            }
                            if(item.status in listOf("failed","cancelled")) {
                                if(item.error.isNotBlank())Text(localizedError(item.error,language),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                                TextButton(onClick={
                                    val surah=app.quran.surahs.firstOrNull{it.id==item.surahId};val qari=app.quran.qaris.firstOrNull{it.id==item.qariId}
                                    if(surah!=null && qari!=null)perform{app.downloads.enqueue(surah,qari)}
                                }){Text(label(language,"Unduh kembali","Retry download"))}
                            }
                        }
                    }
                }
            } else {
                if(filter=="playlist" && selected.isNotEmpty()) item {
                    Button(onClick={
                        val tracks=selected.reversed().mapNotNull { entry ->
                            val surah=app.quran.surahs.firstOrNull{it.id==entry.key.substringAfterLast(":").toIntOrNull()}
                            val qari=app.quran.qaris.firstOrNull{it.id==entry.extra}
                            if(surah!=null && qari!=null)surah to qari else null
                        }
                        app.playback.playCollection(tracks)
                    },modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.PlayArrow,null);Spacer(Modifier.width(8.dp));Text(label(language,"Putar koleksi","Play collection"))}
                }
                if(filter=="history" && selected.isNotEmpty()) item { TextButton(onClick={clearHistory=true}){Text(label(language,"Hapus riwayat","Clear history"))} }
                if(selected.isEmpty()) item { EmptyState(Icons.Outlined.Bookmarks,label(language,"Ruang untuk pilihan Anda","A place for your favourites"), when(filter){
                    "bookmark"->label(language,"Ketuk ikon penanda pada ayat saat membaca Al-Qur’an.","Tap the bookmark on any verse while reading the Qur’an.")
                    "audio_favorite"->label(language,"Tandai surah favorit dari daftar murottal.","Favourite a surah from the recitation list.")
                    "playlist"->label(language,"Tambahkan surah melalui tombol Koleksi di pemutar.","Add surahs using Collection in the full player.")
                    else->label(language,"Bacaan dan audio yang diputar akan tercatat di sini.","Reading and played audio will appear here.")
                }) }
                items(selected,key={it.id}){entry->
                    Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.fillMaxWidth().testTag("library_entry_${entry.id}").clickable{openEntry(entry)}.padding(start=16.dp,top=12.dp,bottom=12.dp,end=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(if(entry.type in listOf("bookmark","read_history"))Icons.Outlined.MenuBook else Icons.Outlined.Headphones,null,tint=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
                                Text(entry.title,style=MaterialTheme.typography.titleMedium)
                                Text(entry.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick={perform{app.library.remove(entry.type,entry.key)}},modifier=Modifier.heightIn(min=52.dp)){Text(label(language,"Hapus","Remove"))}
                        }
                    }
                }
            }
        }
    }
    deleteDownload?.let{item->AlertDialog(onDismissRequest={deleteDownload=null},title={Text(label(language,"Hapus unduhan?","Delete download?"))},text={Text(label(language,"Audio ${item.title} akan dihapus dari perangkat. Anda dapat mengunduhnya lagi.","${item.title} will be removed from this device. You can download it again."))},confirmButton={TextButton(onClick={perform{app.downloads.delete(item.id)};deleteDownload=null}){Text(label(language,"Hapus","Delete"))}},dismissButton={TextButton(onClick={deleteDownload=null}){Text(label(language,"Batal","Cancel"))}})}
    if(clearHistory)AlertDialog(onDismissRequest={clearHistory=false},title={Text(label(language,"Hapus riwayat aktivitas?","Clear activity history?"))},text={Text(label(language,"Penanda, favorit, dan unduhan tetap tersimpan.","Bookmarks, favourites, and downloads will be kept."))},confirmButton={TextButton(onClick={perform{app.library.clearHistory()};clearHistory=false}){Text(label(language,"Hapus riwayat","Clear history"))}},dismissButton={TextButton(onClick={clearHistory=false}){Text(label(language,"Batal","Cancel"))}})
}

private fun downloadStatus(item:DownloadEntity,language:String):String=when(item.status){
    "complete"->label(language,"Siap didengarkan offline","Ready to listen offline")
    "queued"->label(language,"Menunggu koneksi atau giliran unduh","Waiting for connection or download slot")
    "downloading"->"${label(language,"Mengunduh","Downloading")} ${item.progress}%"
    "cancelled"->label(language,"Dibatalkan","Cancelled")
    else->label(language,"Unduhan gagal","Download failed")
}
