package com.nuraya.app.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** The complete bundled, aligned Hafs corpus. Reading/searching never needs a network. */
class QuranRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val corpus: JSONObject by lazy {
        assets.open("quran.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            .also { require(it.getInt("schemaVersion") == 1) }
    }

    val surahs: List<Surah> by lazy {
        val array = corpus.getJSONArray("surahs")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Surah(item.getInt("id"), item.getString("name"), item.getString("arabicName"),
                item.getString("englishName"), item.getInt("ayahCount"), item.getString("revelation"))
        }.also { require(it.size == 114) }
    }

    val allVerses: List<Verse> by lazy {
        val array = corpus.getJSONArray("verses")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val opening = QuranOpeningPolicy.split(item.getInt("surah"), item.getInt("ayah"), item.getString("arabic"))
            Verse(number = item.getInt("number"), surah = item.getInt("surah"), ayah = item.getInt("ayah"),
                arabic = opening.verseText, indonesian = item.getString("indonesian"),
                english = item.getString("english"), juz = item.getInt("juz"),
                indonesianFootnotes = if (item.isNull("indonesianFootnotes")) null else item.getString("indonesianFootnotes"),
                openingBasmalah = opening.openingBasmalah)
        }.also { require(it.size == 6236) }
    }

    /** One visible card for a confirmed same-person alias; stored IDs still resolve in qaris. */
    val qariChoices: List<Qari> by lazy {
        com.nuraya.app.media.CuratedReciters.chapterIds.mapNotNull { id -> qaris.firstOrNull { it.id == id } }
    }

    private val bySurah: Map<Int, List<Verse>> by lazy { allVerses.groupBy(Verse::surah) }
    private val byJuz: Map<Int, List<Verse>> by lazy { allVerses.groupBy(Verse::juz) }

    val qaris: List<Qari> by lazy {
        val catalogue = assets.open("reciters.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        require(catalogue.getInt("schemaVersion") == 1)
        val array = catalogue.getJSONArray("qaris")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Qari(item.getString("id"), item.getString("name"), item.getString("description"),
                item.getString("sourceUrl"), item.getString("baseUrl"))
        }
    }

    val attribution: String = "Arabic: Tanzil Project · Uthmani · CC BY 3.0 (verbatim). " +
        "Indonesia: Kementerian Agama RI · QuranEnc v1.0.1. " +
        "English: Marmaduke Pickthall (1930), public domain. " +
        "Dataset: Risan, Quran JSON · CC BY-SA 4.0. " +
        "Audio: Islamic Network; noncommercial use, copyright remains with the reciters. " +
        "Quran font: Amiri Quran · The Amiri Project Authors · SIL Open Font License 1.1."

    val sourceNotice: String get() = corpus.getJSONObject("metadata").getString("notice")

    fun verses(surah: Int): List<Verse> = bySurah[surah].orEmpty()
    fun verse(surah: Int, ayah: Int): Verse? = bySurah[surah]?.getOrNull(ayah - 1)
    fun juz(number: Int): List<Verse> = byJuz[number].orEmpty()

    private val searchIndex: List<String> by lazy {
        allVerses.map { normalizeSearch("${it.arabic} ${it.indonesian} ${it.english}") }
    }

    fun search(query: String): List<Verse> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val reference = Regex("^(\\d{1,3})\\s*[:.]\\s*(\\d{1,3})$").matchEntire(trimmed)
        if (reference != null) return listOfNotNull(verse(reference.groupValues[1].toInt(), reference.groupValues[2].toInt()))
        val needle = normalizeSearch(trimmed)
        if (needle.isBlank()) return emptyList()
        return allVerses.filterIndexed { index, _ -> searchIndex[index].contains(needle) }
    }

    /** Opens a separately attributed provider; no tafsir is invented or bundled. */
    fun tafsirUrl(surah: Int, ayah: Int): String {
        require(verse(surah, ayah) != null) { "Unknown verse reference" }
        return "https://quran.com/$surah:$ayah/tafsirs/en-tafisr-ibn-kathir"
    }

    companion object {
        /** This normalization is only for the search index, never for displayed source text. */
        fun normalizeSearch(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("ـ", "")
            .replace('ٱ', 'ا')
            .lowercase(Locale.ROOT)
    }
}
