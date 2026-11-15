package com.nuraya.app.reminders

import android.content.Context
import android.util.JsonReader
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.random.Random

data class ReminderText(val title: String, val body: String)
data class QuranReminderMessage(val id: Int, val text: String, val reference: String, val surah: Int, val firstAyah: Int, val lastAyah: Int)

/** User-supplied excerpts; the full, attributed translation remains available in the reader. */
class ReminderContent(context: Context) {
    private val assets = context.applicationContext.assets
    private val prayer by lazy { JSONObject(assets.open("notifications/prayer.json").bufferedReader().use { it.readText() }) }
    val quran: List<QuranReminderMessage> get() = cachedQuran ?: synchronized(ReminderContent::class.java) {
        cachedQuran ?: loadQuran().also { cachedQuran = it }
    }
    private fun loadQuran(): List<QuranReminderMessage> {
        val rows = JSONArray(assets.open("notifications/quran.json").bufferedReader().use { it.readText() })
        // Read only the small chapter index, never the full verse corpus on a receiver/main thread.
        val counts = mutableMapOf<Int,Int>()
        JsonReader(assets.open("quran.json").bufferedReader()).use { reader ->
            reader.beginObject()
            while(reader.hasNext()) {
                if(reader.nextName() != "surahs") { reader.skipValue(); continue }
                reader.beginArray()
                while(reader.hasNext()) {
                    var id=0;var count=0;reader.beginObject()
                    while(reader.hasNext()) when(reader.nextName()) { "id"->id=reader.nextInt();"ayahCount"->count=reader.nextInt();else->reader.skipValue() }
                    reader.endObject();counts[id]=count
                }
                reader.endArray();break
            }
        }
        require(counts.size == 114)
        return List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val ref = row.getString("ref")
            val match = requireNotNull(Regex("\\[(\\d+)]\\s*:\\s*(\\d+)(?:-(\\d+))?").find(ref))
            val surah = match.groupValues[1].toInt(); val first = match.groupValues[2].toInt()
            val last = match.groupValues[3].toIntOrNull() ?: first
            require(first in 1..requireNotNull(counts[surah]) && last in first..requireNotNull(counts[surah]))
            QuranReminderMessage(row.getInt("id"), row.getString("text"), ref, surah, first, last)
        }.also { require(it.size >= 2 && it.map { row -> row.id }.distinct().size == it.size) }
    }
    companion object { @Volatile private var cachedQuran: List<QuranReminderMessage>? = null }
    fun beforePrayer(id: String, name: String, minutes: Int, date: LocalDate, hijriAdjustment: Int = 0, random: Random = Random.Default): ReminderText {
        val key = mapOf("fajr" to "subuh", "dhuhr" to "dzuhur", "asr" to "ashar", "maghrib" to "maghrib", "isha" to "isya")[id]
        val ramadan = runCatching { HijrahDate.from(date.plusDays(hijriAdjustment.toLong())).get(ChronoField.MONTH_OF_YEAR) == 9 }.getOrDefault(false)
        val options = (if (ramadan && key != null) prayer.optJSONObject("ramadan")?.optJSONArray(key) else null)
            ?: (if (date.dayOfWeek == DayOfWeek.FRIDAY && id == "dhuhr") prayer.optJSONArray("jumat") else null)
            ?: key?.let { prayer.optJSONObject("per_salat")?.optJSONArray(it) }
            ?: prayer.getJSONArray("umum")
        val selected = options.getJSONObject(random.nextInt(options.length()))
        fun fill(field: String) = selected.getString(field).replace("{minutes}", minutes.toString()).replace("{salat}", name)
        return ReminderText(fill("title"), fill("body"))
    }
}
