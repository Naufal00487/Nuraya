package com.nuraya.app.quranreading

import android.content.Context
import org.json.JSONObject

/** Published Indonesian transliteration, indexed by the unchanged canonical verse reference. */
data class LatinVerse(val surah: Int, val ayah: Int, val latin: String)

class LatinRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val cache = object : LinkedHashMap<Int, List<LatinVerse>>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<LatinVerse>>?): Boolean = size > 3
    }
    val source: String = "Transliterasi Latin Indonesia · EQuran.id API v2"

    /** First access performs a small asset read; callers use Dispatchers.IO. */
    @Synchronized
    fun chapter(surah: Int): List<LatinVerse> {
        if (surah !in 1..114) return emptyList()
        return cache.getOrPut(surah) {
            val file = assets.open("latin/$surah.json").bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            require(file.getInt("surah") == surah)
            val rows = file.getJSONArray("verses")
            List(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                val ayah = row.getInt("ayah")
                val text = row.getString("latin")
                require(ayah == index + 1 && text.isNotBlank())
                LatinVerse(surah, ayah, text)
            }
        }
    }

    fun verse(surah: Int, ayah: Int): LatinVerse? = chapter(surah).getOrNull(ayah - 1)

    companion object {
        private var owner: Context? = null
        private var shared: LatinRepository? = null

        /** Reuses chapter reads across verse cards; only the application Context is retained. */
        @Synchronized
        fun shared(context: Context): LatinRepository {
            val app = context.applicationContext
            if (owner !== app || shared == null) {
                owner = app
                shared = LatinRepository(app)
            }
            return requireNotNull(shared)
        }
    }
}
