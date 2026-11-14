package com.nuraya.app.media

import android.content.Context
import org.json.JSONObject

private val canonicalAyahCounts = intArrayOf(7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6)

private val canonicalAyahEnds = canonicalAyahCounts.runningFold(0, Int::plus).drop(1)

/** A verified edition with all 6,236 individual ayah files at [bitrateKbps]. */
data class VerseReciter(
    val id: String,
    val name: String,
    val bitrateKbps: Int,
    val supportsWordSync: Boolean = false,
    val timingFolder: String? = null,
    val ayahBaseUrl: String? = null,
) {
    val source: String get() = if (ayahBaseUrl == null) "Islamic Network" else "EveryAyah"
    fun audioUrl(globalAyah: Int): String {
        require(globalAyah in 1..6236)
        ayahBaseUrl?.let { base ->
            require(base.startsWith("https://everyayah.com/data/"))
            val chapter = canonicalAyahEnds.indexOfFirst { globalAyah <= it }
            val ayah = globalAyah - (if (chapter == 0) 0 else canonicalAyahEnds[chapter - 1])
            return "${base.trimEnd('/')}/${(chapter + 1).toString().padStart(3, '0')}${ayah.toString().padStart(3, '0')}.mp3"
        }
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
                if (item.isNull("timingFolder")) null else item.getString("timingFolder"),
                item.optString("ayahBaseUrl").takeIf { it.isNotBlank() })
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
