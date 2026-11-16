package com.nuraya.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuraya.app.NurayaApplication
import com.nuraya.app.media.CuratedReciters
import com.nuraya.app.media.PlaybackState
import com.nuraya.app.storage.DownloadEntity
import com.nuraya.app.storage.UserEntry
import com.nuraya.app.ui.icons.AppIcons as Icons

/** Full-screen listening controls. Collapsing keeps murottal playing; stopping is explicit. */
@Composable
fun FullPlayer(app: NurayaApplication, language: String, state: PlaybackState,
    entries: List<UserEntry>, downloads: List<DownloadEntity>, perform: (suspend () -> Unit) -> Unit,
    onClose: () -> Unit, onRead: (Int, Int) -> Unit = { _, _ -> }) {
    val surah = app.quran.surahs.firstOrNull { it.id == state.surahId }
    val qari = app.quran.qaris.firstOrNull { it.id == state.qariId }
    val key = "${state.qariId}:${state.surahId}"
    val favorite = entries.any { it.type == "audio_favorite" && it.key == key }
    val inCollection = entries.any { it.type == "playlist" && it.key == key }
    val download = downloads.firstOrNull { it.id == key }
    var seeking by remember(key, state.ayahId) { mutableStateOf<Float?>(null) }
    var more by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxSize().testTag("full_player"), color = colors.background) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.background)))
            .safeDrawingPadding()) {
            val portraitSize = (maxHeight * .36f).coerceIn(156.dp, 300.dp).coerceAtMost(maxWidth - 64.dp)
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("player_collapse")) {
                        Icon(Icons.Outlined.ExpandMore, label(language, "Kecilkan pemutar", "Minimize player"))
                    }
                    Text("Murottal", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    Box {
                        IconButton(onClick = { more = true }) { Icon(Icons.Outlined.MoreVert, label(language, "Pilihan pemutar", "Player options")) }
                        DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                            DropdownMenuItem(text = { Text(label(language, "Hentikan audio", "Stop audio")) },
                                leadingIcon = { Icon(Icons.Outlined.Stop, null) }, modifier = Modifier.testTag("player_stop"),
                                onClick = { more = false; app.playback.stop(); onClose() })
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    QariPortrait(state.qariId, state.qari, language, Modifier.size(portraitSize), RoundedCornerShape(28.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(state.qari, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                        if (state.verseMode) {
                            val range = if (state.rangeStartSurah > 0 && state.rangeStartSurah != state.rangeEndSurah)
                                "${state.rangeStartSurah}:${state.rangeStart}–${state.rangeEndSurah}:${state.rangeEnd}" else "${state.rangeStart}–${state.rangeEnd}"
                            Text(label(language, "Ayat ${state.ayahId} · rentang $range", "Verse ${state.ayahId} · range $range"), style = MaterialTheme.typography.bodySmall)
                            if (state.rangePasses > 1) Text(label(language, "Putaran ${state.rangePass} / ${state.rangePasses}", "Pass ${state.rangePass} / ${state.rangePasses}"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.error != null) Surface(color = colors.errorContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(localizedError(state.error, language), style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer)
                            TextButton(onClick = {
                                if (state.verseMode) app.playback.playVerseSpan(state.surahId, state.ayahId.coerceAtLeast(1),
                                    state.rangeEndSurah.takeIf { it > 0 } ?: state.surahId, state.rangeEnd.coerceAtLeast(1), state.qariId)
                                else if (surah != null && qari != null) app.playback.play(surah, qari)
                            }) { Text(label(language, "Coba lagi", "Try again")) }
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        val timelineLabel = label(language, "Posisi audio", "Audio position")
                        Slider(value = seeking ?: state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                            onValueChange = { seeking = it }, onValueChangeFinished = { seeking?.let { app.playback.seekTo(it.toLong()) }; seeking = null },
                            valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(), enabled = state.durationMs > 0,
                            modifier = Modifier.fillMaxWidth().testTag("player_timeline").semantics { contentDescription = timelineLabel })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(seeking?.toLong() ?: state.positionMs), style = MaterialTheme.typography.labelMedium)
                            Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 48.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        IconToggleButton(checked = state.repeat, onCheckedChange = { app.playback.setRepeat(it) }, modifier = Modifier.testTag("player_repeat")) {
                            Icon(Icons.Outlined.RepeatOne, label(language, "Ulangi audio", "Repeat audio"), tint = if (state.repeat) colors.primary else colors.onSurfaceVariant)
                        }
                        IconButton(onClick = { app.playback.previous() }, modifier = Modifier.size(48.dp).testTag("player_previous")) {
                            Icon(Icons.Outlined.SkipPrevious, label(language, "Sebelumnya", "Previous"), Modifier.size(28.dp))
                        }
                        FilledIconButton(onClick = { app.playback.toggle() }, modifier = Modifier.size(76.dp).testTag("player_toggle"), shape = CircleShape) {
                            val action = label(language, if (state.isPlaying) "Jeda" else "Putar", if (state.isPlaying) "Pause" else "Play")
                            if (state.loading) CircularProgressIndicator(Modifier.size(30.dp).semantics { contentDescription = action }, color = colors.onPrimary, strokeWidth = 2.dp)
                            else Icon(if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, action, Modifier.size(34.dp))
                        }
                        IconButton(onClick = { app.playback.next() }, modifier = Modifier.size(48.dp).testTag("player_next")) {
                            Icon(Icons.Outlined.SkipNext, label(language, "Berikutnya", "Next"), Modifier.size(28.dp))
                        }
                        Box {
                            TextButton(onClick = { speedMenu = true }, modifier = Modifier.widthIn(min = 48.dp).testTag("player_speed"), contentPadding = PaddingValues(0.dp)) {
                                Text("${state.speed}×", style = MaterialTheme.typography.labelLarge)
                            }
                            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                                listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                    DropdownMenuItem(text = { Text("$speed×") }, onClick = { app.playback.setSpeed(speed); speedMenu = false },
                                        trailingIcon = { if (state.speed == speed) Icon(Icons.Outlined.CheckCircle, null) })
                                }
                            }
                        }
                    }
                    }
                    if (!state.verseMode && surah != null && qari != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PlayerAction(if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                label(language, if (favorite) "Disukai" else "Favorit", if (favorite) "Liked" else "Favorite"), "player_favorite", Modifier.weight(1f), selected = favorite) {
                                perform { app.library.toggle("audio_favorite", key, surah.name, qari.name, qari.id) }
                            }
                            PlayerAction(if (download?.status == "complete") Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                                when (download?.status) { "complete" -> label(language, "Tersimpan", "Saved"); "queued", "downloading" -> "${download.progress}%"; else -> label(language, "Unduh", "Download") },
                                "player_download", Modifier.weight(1f), enabled = download?.status !in listOf("complete", "queued", "downloading")) {
                                perform { app.downloads.enqueue(surah, qari) }
                            }
                            PlayerAction(if (inCollection) Icons.Outlined.PlaylistAddCheck else Icons.Outlined.PlaylistAdd,
                                label(language, "Koleksi", "Collection"), "player_collection", Modifier.weight(1f), selected = inCollection) {
                                perform { app.library.toggle("playlist", key, surah.name, qari.name, qari.id) }
                            }
                        }
                    }
                    if (state.verseMode) {
                        OutlinedButton(onClick = { onRead(state.surahId, state.ayahId.coerceAtLeast(1)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(label(language, "Ikuti teks bacaan", "Follow the recitation text"))
                        }
                    } else if (surah != null) CuratedReciters.verseIdFor(state.qariId)?.let { verseId ->
                        OutlinedButton(onClick = {
                            perform { app.settings.setReaderQari(verseId) }
                            app.playback.playVerseRange(surah.id, 1, surah.ayahCount, verseId)
                            onRead(surah.id, 1)
                        }, modifier = Modifier.fillMaxWidth().testTag("player_read_from_start")) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(label(language, "Baca & dengarkan dari awal", "Read & listen from the start"), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerAction(icon: ImageVector, title: String, tag: String, modifier: Modifier,
    selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.testTag(tag), shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (!enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}
