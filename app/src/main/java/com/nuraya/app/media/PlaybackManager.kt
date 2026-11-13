package com.nuraya.app.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nuraya.app.data.Qari
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.data.Surah
import com.nuraya.app.storage.DownloadRepository
import com.nuraya.app.storage.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackState(
    val title: String = "",
    val qari: String = "",
    val qariId: String = "",
    val surahId: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val repeat: Boolean = false,
    val error: String? = null,
    val loading: Boolean = false,
    val ayahId: Int = 0,
    val verseMode: Boolean = false,
    val wordPosition: Int = 0,
    val rangeStart: Int = 0,
    val rangeEnd: Int = 0,
    val rangePass: Int = 1,
    val rangePasses: Int = 1,
    val wordTimingAvailable: Boolean = false,
    val source: String = "Islamic Network",
)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlaybackManager(
    context: Context,
    private val quran: QuranRepository,
    private val library: LibraryRepository,
    private val downloads: DownloadRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()
    private var controller: MediaController? = null
    private val verseCatalog = VerseAudioCatalog(appContext)
    val verseReciters: List<VerseReciter> get() = CuratedReciters.verseIds.mapNotNull { id -> verseCatalog.reciters.firstOrNull { it.id == id }?.let { it.copy(name = CuratedReciters.names[id] ?: it.name) } }
    private var timedChapter: TimedChapter? = null
    private data class VerseRequest(val surah: Int, val start: Int, val end: Int,
        val reciter: VerseReciter, val repeatEach: Int, val repeatRange: Boolean, val rangeRepeats: Int = 1)
    private data class QueueRequest(val tracks: List<Pair<Surah, Qari>> = emptyList(), val index: Int = 0,
        val verse: VerseRequest? = null)
    private var pending: QueueRequest? = null
    private var generation = 0
    private var preparingQueue = false
    private var connecting = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Clearing an invalid selection must not erase its actionable error.
            if (mediaItem != null) mutableState.update { it.copy(error = null) }
            if (controller?.isPlaying == true) recordHistory(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) recordHistory(controller?.currentMediaItem)
        }

        override fun onPlayerError(error: PlaybackException) {
            val player = controller
            val fallback = player?.currentMediaItem?.let { individualVerseFallback(it, verseCatalog, quran) }
            if (player != null && fallback != null) {
                val index = player.currentMediaItemIndex
                player.replaceMediaItem(index, fallback)
                player.seekTo(index, 0L)
                mutableState.update { it.copy(error = null, loading = true, wordPosition = 0, wordTimingAvailable = false, source = "Islamic Network") }
                player.prepare(); player.play()
                return
            }
            mutableState.update {
                it.copy(
                    isPlaying = false,
                    loading = false,
                    error = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Koneksi audio gagal. Periksa internet atau pilih unduhan. / Audio connection failed. Check your connection or use a download."
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            "Rekaman tidak tersedia. Coba lagi atau pilih qari lain. / Recording unavailable. Retry or choose another reciter."
                        else -> "Audio tidak dapat diputar (${error.errorCodeName}). / Audio playback failed (${error.errorCodeName})."
                    },
                )
            }
        }
    }

    init {
        connect()
        scope.launch {
            while (isActive) {
                updateState()
                delay(100)
            }
        }
    }

    private fun connect() {
        if (connecting || controller != null) return
        connecting = true
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
        ).setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                this@PlaybackManager.controller = null
                preparingQueue = false
                mutableState.update { it.copy(isPlaying = false, loading = false,
                    error = "Pemutar terputus. Pilih rekaman untuk mencoba lagi. / Player disconnected. Select a recording to retry.") }
            }
        }).buildAsync()
        future.addListener({
            connecting = false
            try {
                controller = future.get().also { it.addListener(listener) }
                updateState()
                pending?.also { pending = null; startQueue(it) }
            } catch (exception: Exception) {
                pending = null
                preparingQueue = false
                mutableState.update {
                    it.copy(loading = false, error = "Pemutar belum tersedia. Pilih rekaman untuk mencoba lagi. / Player unavailable. Select a recording to retry.")
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(surah: Surah, qari: Qari) {
        val player = controller
        val mediaId = "${qari.id}:${surah.id}"
        if (player != null && !preparingQueue && player.currentMediaItem?.mediaId == mediaId && player.playerError == null) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
            return
        }
        val catalogQari = quran.qaris.firstOrNull { it.id == qari.id } ?: return
        val tracks = quran.surahs.map { it to catalogQari }
        val index = tracks.indexOfFirst { it.first.id == surah.id }
        if (index >= 0) startQueue(QueueRequest(tracks, index))
    }

    /** Plays the personal collection in the caller's visible order, including mixed reciters. */
    fun playCollection(tracks: List<Pair<Surah, Qari>>) {
        val catalogSurahs = quran.surahs.associateBy { it.id }
        val catalogQaris = quran.qaris.associateBy { it.id }
        val verified = tracks.take(500).mapNotNull { (surah, qari) ->
            val knownSurah = catalogSurahs[surah.id]
            val knownQari = catalogQaris[qari.id]
            if (knownSurah != null && knownQari != null) knownSurah to knownQari else null
        }
        if (verified.isNotEmpty()) startQueue(QueueRequest(verified, 0))
    }

    fun playVerseRange(surahId: Int, startAyah: Int, endAyah: Int, qariId: String,
        repeatEach: Int = 1, repeatRange: Boolean = false, rangeRepeats: Int = 1) {
        val surah = quran.surahs.firstOrNull { it.id == surahId } ?: return
        if (startAyah !in 1..surah.ayahCount || endAyah !in startAyah..surah.ayahCount || repeatEach !in 1..20 || rangeRepeats !in 1..20 || repeatEach*rangeRepeats>20) return
        val reciter = verseReciters.firstOrNull { it.id == qariId }
        if (reciter == null) {
            // A stale saved ID must never select a different person's voice.
            // Stop also invalidates queued preparation and any pending connection.
            stop()
            mutableState.value = PlaybackState(
                title = "${surah.name} · $startAyah", qariId = qariId,
                surahId = surahId, ayahId = startAyah, verseMode = true,
                rangeStart = startAyah, rangeEnd = endAyah,
                error = "Qari pilihan tidak tersedia. Pilih ulang qari di pengaturan bacaan. / Selected reciter is unavailable. Choose a reciter again in reading settings.",
            )
            return
        }
        startQueue(QueueRequest(verse = VerseRequest(surahId, startAyah, endAyah, reciter, repeatEach, repeatRange, rangeRepeats)))
    }

    private fun startVerseQueue(queue: QueueRequest) {
        val verse = queue.verse ?: return
        val surah = quran.surahs[verse.surah - 1]
        val request = ++generation
        preparingQueue = true
        timedChapter = null
        mutableState.update { it.copy(title = "${surah.name} · ${verse.start}", qari = verse.reciter.name,
            qariId = verse.reciter.id, surahId = verse.surah, ayahId = verse.start, verseMode = true,
            rangeStart = verse.start, rangeEnd = verse.end, rangePass = 1, rangePasses = verse.rangeRepeats, wordPosition = 0, wordTimingAvailable = false,
            loading = true, isPlaying = false, positionMs = 0, durationMs = 0, error = null) }
        val player = controller
        if (player == null) { pending = queue; connect(); return }
        player.pause()
        scope.launch {
            try {
                val (chapter, items) = withContext(Dispatchers.IO) {
                    val chapter = verseCatalog.timing(verse.reciter, verse.surah)
                    require(chapter == null || chapter.verses.size == surah.ayahCount)
                    val items = repeatedVerseSequence(verse.start, verse.end, verse.repeatEach, verse.rangeRepeats, surah.ayahCount).mapIndexed { index, ayah ->
                        val timing = chapter?.verses?.getOrNull(ayah - 1)?.takeIf { it.usable }
                        val extras = Bundle().apply {
                            putString("qariId", verse.reciter.id); putInt("surahId", verse.surah)
                            putInt("ayahId", ayah); putBoolean("verseMode", true)
                            putInt("rangeStart", verse.start); putInt("rangeEnd", verse.end)
                            putInt("rangePass",index/((verse.end-verse.start+1)*verse.repeatEach)+1);putInt("rangePasses",verse.rangeRepeats)
                            putBoolean("wordTiming", timing != null)
                            putLong("clipStartMs", timing?.startMs ?: 0L)
                            putString("source", if (timing != null) requireNotNull(chapter).source else "Islamic Network")
                        }
                        MediaItem.Builder()
                            .setMediaId("verse:${verse.reciter.id}:${verse.surah}:$ayah:$index")
                            .setUri(if (timing != null) requireNotNull(chapter).audioUrl else verse.reciter.audioUrl(requireNotNull(quran.verse(verse.surah, ayah)).number))
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("${surah.name} · $ayah")
                                .setArtist(verse.reciter.name).setAlbumTitle("Nuraya · Al-Qur'an")
                                .setIsPlayable(true).setExtras(extras).build())
                            .apply { if (timing != null) setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(timing.startMs).setEndPositionMs(timing.endMs).build()) }
                            .build()
                    }
                    chapter to items
                }
                if (request != generation) return@launch
                timedChapter = chapter
                preparingQueue = false
                player.repeatMode = if (verse.repeatRange) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                player.setMediaItems(items, 0, 0L)
                player.prepare()
                player.play()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (exception: Exception) {
                if (request == generation) {
                    preparingQueue = false
                    mutableState.update { it.copy(loading = false, isPlaying = false,
                        error = "Audio ayat belum tersedia. Coba lagi. / Verse audio unavailable. Please retry.") }
                }
            }
        }
    }

    private fun startQueue(queue: QueueRequest) {
        if (queue.verse != null) { startVerseQueue(queue); return }
        timedChapter = null
        val (surah, qari) = queue.tracks[queue.index]
        val request = ++generation
        preparingQueue = true
        mutableState.update {
            it.copy(title = surah.name, qari = qari.name, qariId = qari.id, surahId = surah.id,
                loading = true, isPlaying = false, positionMs = 0, durationMs = 0, error = null,
                verseMode = false, ayahId = 0, wordPosition = 0, rangeStart = 0, rangeEnd = 0, rangePass = 1, rangePasses = 1,
                wordTimingAvailable = false, source = "Islamic Network")
        }
        val player = controller
        if (player == null) { pending = queue; connect(); return }
        player.pause()
        scope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    queue.tracks.map { (item, reciter) ->
                        val local = downloads.localFile(reciter.id, item.id)
                        val extras = Bundle().apply {
                            putString("qariId", reciter.id)
                            putInt("surahId", item.id)
                        }
                        MediaItem.Builder()
                            .setMediaId("${reciter.id}:${item.id}")
                            .setUri(local?.let { Uri.fromFile(it) } ?: Uri.parse(reciter.audioUrl(item.id)))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(item.name)
                                    .setArtist(reciter.name)
                                    .setAlbumTitle("Nuraya · Al-Qur'an")
                                    .setIsPlayable(true)
                                    .setExtras(extras)
                                    .build(),
                            )
                            .build()
                    }
                }
                if (request != generation) return@launch
                preparingQueue = false
                if (player.repeatMode == Player.REPEAT_MODE_ALL) player.repeatMode = Player.REPEAT_MODE_OFF
                player.setMediaItems(items, queue.index, 0L)
                player.prepare()
                player.play()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (request == generation) {
                    preparingQueue = false
                    mutableState.update {
                        it.copy(loading = false, isPlaying = false, error = "Audio belum tersedia. Coba lagi. / Audio unavailable. Please retry.")
                    }
                }
            }
        }
    }

    /** Used before microphone practice; cancelling preparation also prevents late audio starts. */
    fun pause() {
        ++generation
        preparingQueue = false
        pending = null
        controller?.pause()
        mutableState.update { it.copy(loading = false, isPlaying = false) }
    }

    fun toggle() {
        if (preparingQueue) {
            ++generation
            preparingQueue = false
            pending = null
            controller?.pause()
            mutableState.update { it.copy(loading = false, isPlaying = false) }
            return
        }
        controller?.run {
            if (isPlaying || playWhenReady) pause() else {
                if (playbackState == Player.STATE_ENDED) seekTo(0L)
                if (playerError != null || playbackState == Player.STATE_IDLE) {
                    mutableState.update { it.copy(error = null) }
                    prepare()
                }
                play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.let { player ->
            val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
            player.seekTo(positionMs.coerceIn(0L, duration))
            updateState()
        }
    }

    fun setSpeed(speed: Float) {
        if (speed.isFinite()) controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    fun setRepeat(enabled: Boolean) {
        controller?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun next() {
        controller?.run { if (hasNextMediaItem()) seekToNextMediaItem() }
    }

    fun previous() {
        controller?.run {
            if (currentPosition > 3_000L) seekTo(0L)
            else if (hasPreviousMediaItem()) seekToPreviousMediaItem()
            else seekTo(0L)
        }
    }

    fun stop() {
        ++generation
        preparingQueue = false
        pending = null
        controller?.stop()
        controller?.clearMediaItems()
        timedChapter = null
        mutableState.value = PlaybackState()
    }

    private fun recordHistory(item: MediaItem?) {
        if (item == null) return
        val surahId = item.mediaMetadata.extras?.getInt("surahId") ?: return
        val qariId = item.mediaMetadata.extras?.getString("qariId") ?: return
        scope.launch(Dispatchers.IO) {
            try {
                library.save(
                    type = if (item.mediaMetadata.extras?.getBoolean("verseMode") == true) "verse_audio_history" else "audio_history",
                    key = if (item.mediaMetadata.extras?.getBoolean("verseMode") == true) "$qariId:$surahId:${item.mediaMetadata.extras?.getInt("ayahId")}" else "$qariId:$surahId",
                    title = item.mediaMetadata.title?.toString().orEmpty(),
                    subtitle = item.mediaMetadata.artist?.toString().orEmpty(),
                    extra = qariId,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("Nuraya", "Playback history could not be saved", error)
            }
        }
    }

    private fun updateState() {
        if (preparingQueue) return
        val player = controller ?: return
        val item = player.currentMediaItem
        val extras = item?.mediaMetadata?.extras
        val verseMode = extras?.getBoolean("verseMode") == true
        val ayah = if (verseMode) extras?.getInt("ayahId") ?: 0 else 0
        val hasTiming = verseMode && extras?.getBoolean("wordTiming") == true && timedChapter?.surah == extras.getInt("surahId")
        val word = if (hasTiming && player.isPlaying) timedChapter?.verses?.getOrNull(ayah - 1)
            ?.wordAt(player.currentPosition + (extras?.getLong("clipStartMs") ?: 0L)) ?: 0 else 0
        mutableState.update { old ->
            old.copy(
                title = item?.mediaMetadata?.title?.toString() ?: old.title,
                qari = item?.mediaMetadata?.artist?.toString() ?: old.qari,
                qariId = item?.mediaMetadata?.extras?.getString("qariId") ?: old.qariId,
                surahId = item?.mediaMetadata?.extras?.getInt("surahId") ?: old.surahId,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
                speed = player.playbackParameters.speed,
                repeat = player.repeatMode == Player.REPEAT_MODE_ONE,
                loading = player.playbackState == Player.STATE_BUFFERING,
                verseMode = verseMode,
                ayahId = ayah,
                wordPosition = word,
                rangeStart = if (verseMode) extras?.getInt("rangeStart") ?: 0 else 0,
                rangeEnd = if (verseMode) extras?.getInt("rangeEnd") ?: 0 else 0,
                wordTimingAvailable = hasTiming,
                rangePass = if(verseMode) extras?.getInt("rangePass",1) ?: 1 else 1,
                rangePasses = if(verseMode) extras?.getInt("rangePasses",1) ?: 1 else 1,
                source = extras?.getString("source") ?: "Islamic Network",
            )
        }
    }
}

/** One fallback attempt, preserving selected identity and finite queue order. */
internal fun individualVerseFallback(item: MediaItem, catalog: VerseAudioCatalog, quran: QuranRepository): MediaItem? {
    val extras = item.mediaMetadata.extras ?: return null
    if (!extras.getBoolean("verseMode") || !extras.getBoolean("wordTiming")) return null
    val reciter = catalog.reciters.firstOrNull { it.id == extras.getString("qariId") } ?: return null
    val verse = quran.verse(extras.getInt("surahId"), extras.getInt("ayahId")) ?: return null
    val fallbackExtras = Bundle(extras).apply { putBoolean("wordTiming", false); putLong("clipStartMs", 0); putString("source", "Islamic Network") }
    return item.buildUpon().setUri(reciter.audioUrl(verse.number))
        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().build())
        .setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(fallbackExtras).build()).build()
}
