package com.nuraya.app.media

import android.content.Context
import org.json.JSONObject

/** A verified edition with all 6,236 individual ayah files at [bitrateKbps]. */
data class VerseReciter(
    val id: String,
    val name: String,
    val bitrateKbps: Int,
    val supportsWordSync: Boolean = false,
    val timingFolder: String? = null,
) {
    fun audioUrl(globalAyah: Int): String {
        require(globalAyah in 1..6236)
        return "https://cdn.islamic.network/quran/audio/$bitrateKbps/$id/$globalAyah.mp3"
    }
}

data class WordTiming(val position: Int, val startMs: Long, val endMs: Long)
data class VerseTiming(val ayah: Int, val startMs: Long, val endMs: Long, val words: List<WordTiming>, val usable: Boolean = true) {
    /** Segment positions are absolute within the exact paired chapter recording. */
    fun wordAt(absolutePositionMs: Long): Int = if (!usable) 0 else words.firstOrNull {
        absolutePositionMs >= it.startMs && absolutePositionMs < it.endMs
    }?.position ?: 0
}
data class TimedChapter(val surah: Int, val audioUrl: String, val verses: List<VerseTiming>, val source: String = "Quran.com · QuranicAudio")

/** Metadata is bundled offline; recordings stream from the source URL paired with the timing. */
class VerseAudioCatalog(context: Context) {
    private val assets = context.applicationContext.assets
    val reciters: List<VerseReciter> by lazy {
        val root = assets.open("verse_reciters.json").bufferedReader().use { JSONObject(it.readText()) }
        require(root.getInt("schemaVersion") == 1)
        val array = root.getJSONArray("reciters")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            VerseReciter(item.getString("id"), item.getString("name"), item.getInt("bitrateKbps"),
                item.optBoolean("supportsWordSync", false),
                if (item.isNull("timingFolder")) null else item.getString("timingFolder"))
        }.also { list -> require(list.map { it.id }.distinct().size == list.size) }
    }

    /** Call on an IO dispatcher. Timing files are small and loaded only for the selected chapter. */
    fun timing(reciter: VerseReciter, surah: Int): TimedChapter? {
        require(surah in 1..114)
        val folder = reciter.timingFolder ?: return null
        require(folder.matches(Regex("[a-z0-9_]+")))
        val root = assets.open("audio_timing/$folder/$surah.json").bufferedReader().use { JSONObject(it.readText()) }
        require(root.getInt("surah") == surah)
        val url = root.getString("audioUrl")
        if (reciter.id == "ar.alafasy") {
            require(root.getInt("recitationId") == 7)
            require(url == "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/$surah.mp3")
        } else {
            require(root.getString("reciterId") == reciter.id && root.getString("sourceSlug") == folder)
            val uri = java.net.URI(url)
            require(uri.scheme == "https" && uri.host in setOf("download.quranicaudio.com", "audio-cdn.tarteel.ai", "cdn.mp3quran.net") && uri.path.endsWith(".mp3"))
        }
        val array = root.getJSONArray("verses")
        val verses = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val segments = item.getJSONArray("segments")
            val start = item.getLong("clipFrom")
            val end = item.getLong("clipTo")
            require(start >= 0 && end > start && item.getInt("ayah") == index + 1)
            VerseTiming(index + 1, start, end, List(segments.length()) { j ->
                val segment = segments.getJSONArray(j)
                WordTiming(segment.getInt(0), segment.getLong(1), segment.getLong(2)).also {
                    require(it.position > 0 && it.startMs >= 0 && it.endMs > it.startMs)
                }
            }, usable = item.getBoolean("usable"))
        }
        return TimedChapter(surah, url, verses, root.optString("source", "Quran.com · QuranicAudio"))
    }
}

/** Queue order is independent of audio length, so repeats do not invent word timing. */
fun repeatedAyahRange(startAyah: Int, endAyah: Int, repeatEach: Int, ayahCount: Int): List<Int> {
    require(startAyah in 1..ayahCount && endAyah in startAyah..ayahCount)
    require(repeatEach in 1..20)
    return (startAyah..endAyah).flatMap { ayah -> List(repeatEach) { ayah } }
}

/** Finite passes: [1,2,3,1,2,3], distinct from repeating each verse [1,1,2,2,3,3]. */
fun repeatedVerseSequence(startAyah:Int,endAyah:Int,repeatEach:Int,rangeRepeats:Int,ayahCount:Int):List<Int>{
    require(rangeRepeats in 1..20 && repeatEach in 1..20 && rangeRepeats*repeatEach<=20)
    val pass=repeatedAyahRange(startAyah,endAyah,repeatEach,ayahCount)
    return List(rangeRepeats){pass}.flatten()
}
