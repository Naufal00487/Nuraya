package com.nuraya.app.hadith

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.nuraya.app.data.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.GZIPInputStream

data class Hadith(val id: Long, val collection: String, val number: String, val section: String, val arabic: String, val indonesian: String, val grades: String) {
    val collectionName get() = if (collection == "bukhari") "Shahih al-Bukhari" else "Shahih Muslim"
    val reference get() = "$collectionName · No. $number"
}
data class HadithSearchResult(val items: List<Hadith>, val total: Int)

/** Source text remains verbatim. FTS normalization is isolated to the search index. */
class HadithRepository(context: Context) {
    private val app = context.applicationContext
    val sources: JSONObject by lazy { app.assets.open("hadith/sources.json").bufferedReader().use { JSONObject(it.readText()) } }
    private fun database(): SQLiteDatabase {
        val target = app.getDatabasePath("nuraya-hadith-${sources.getString("databaseSha256").take(12)}.sqlite")
        synchronized(copyLock) {
            if (!target.exists() || target.length() != sources.getLong("databaseBytes")) {
                target.parentFile!!.mkdirs()
                val temp = File(target.parentFile, target.name + ".tmp")
                try {
                    GZIPInputStream(app.assets.open("hadith/hadith.database")).use { input -> temp.outputStream().use { input.copyTo(it) } }
                    check(temp.length() == sources.getLong("databaseBytes"))
                    check(temp.renameTo(target))
                } finally { if (temp.exists()) temp.delete() }
            }
        }
        return SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS)
    }
    suspend fun search(query: String, collection: String? = null, offset: Int = 0): HadithSearchResult = withContext(Dispatchers.IO) {
        require(query.length <= 200 && offset in 0..20_000 && collection in listOf(null, "bukhari", "muslim"))
        val normalized = QuranRepository.normalizeSearch(query).lowercase().trim()
        val reference = Regex("^(?:(?:hr[. ]*)?(bukhari|muslim)[ :#]*)?(\\d+(?:\\.\\d+)?)$").matchEntire(normalized)
        val selected = reference?.groupValues?.get(1)?.takeIf(String::isNotBlank) ?: collection
        val where = mutableListOf<String>(); val args = mutableListOf<String>()
        if (selected != null) { where += "h.collection = ?"; args += selected }
        val from: String
        if (reference != null) {
            from = "hadith h"; where += "h.number = ?"; args += reference.groupValues[2]
        } else {
            val tokens = Regex("[\\p{L}\\p{N}]{2,}").findAll(normalized).map { it.value }.take(10).toList()
            if (tokens.isEmpty()) return@withContext HadithSearchResult(emptyList(), 0)
            from = "hadith_search JOIN hadith h ON h.id = hadith_search.docid"
            where += "hadith_search MATCH ?"; args += tokens.joinToString(" ") { "\"$it\"*" }
        }
        database().use { db ->
            val condition = where.joinToString(" AND ")
            val count = db.rawQuery("SELECT count(*) FROM $from WHERE $condition", args.toTypedArray()).use { it.moveToFirst(); it.getInt(0) }
            val items = db.rawQuery("SELECT h.id,h.collection,h.number,h.section,h.arabic,h.indonesian,h.grades FROM $from WHERE $condition ORDER BY h.collection,h.sort_number LIMIT 40 OFFSET $offset", args.toTypedArray()).use { cursor ->
                buildList { while (cursor.moveToNext()) add(Hadith(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6))) }
            }
            HadithSearchResult(items, count)
        }
    }
    suspend fun get(id: Long): Hadith? = withContext(Dispatchers.IO) {
        database().use { db -> db.rawQuery("SELECT id,collection,number,section,arabic,indonesian,grades FROM hadith WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (!c.moveToFirst()) null else Hadith(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6))
        } }
    }
    companion object { private val copyLock = Any() }
}
