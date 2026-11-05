package com.nuraya.app.study

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Offline attributed learning data. Call the first load for a chapter on Dispatchers.IO. */
class StudyRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val cache = object : LinkedHashMap<Int, List<StudyVerse>>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<StudyVerse>>?): Boolean = size > 3
    }
    private val metadata: JSONObject? by lazy {
        runCatching { assets.open("study/index.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) } }.getOrNull()
    }
    val available: Boolean get() = metadata?.optInt("schemaVersion") == 1
    val sources: List<String> get() = metadata?.optJSONArray("sources")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()

    @Synchronized
    fun chapter(surah: Int): List<StudyVerse> {
        if (surah !in 1..114 || !available) return emptyList()
        return cache.getOrPut(surah) {
            val file = assets.open("study/$surah.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            require(file.getInt("surah") == surah)
            val data = file.getJSONArray("verses")
            List(data.length()) { index ->
                val item = data.getJSONObject(index)
                val arabic = item.getString("arabic")
                val spans = item.optJSONArray("tajwidSpans") ?: JSONArray()
                val words = item.optJSONArray("words") ?: JSONArray()
                StudyVerse(
                    surah, item.getInt("ayah"), arabic,
                    List(spans.length()) { i -> spans.getJSONObject(i).let {
                        TajwidSpan(it.getInt("start"), it.getInt("end"), it.getString("rule")).also { span ->
                            require(span.start >= 0 && span.end > span.start && span.end <= arabic.length)
                        }
                    } },
                    List(words.length()) { i -> words.getJSONObject(i).let {
                        StudyWord(it.getInt("position"), it.getString("arabic"), it.nullableString("indonesian"),
                            it.nullableString("english"), it.nullableInt("start"), it.nullableInt("end"))
                    } },
                    item.nullableString("tafsir"), item.nullableString("tafsirSource"), item.nullableString("tafsirLanguage"),
                    file.nullableString("wordsSource"), file.nullableString("tajwidSource"),
                )
            }
        }
    }

    fun verse(surah: Int, ayah: Int): StudyVerse? = chapter(surah).firstOrNull { it.ayah == ayah }

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key).takeIf { it.isNotBlank() }
    private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)
}
