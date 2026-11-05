package com.nuraya.app.quranreading

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.nuraya.app.data.Verse
import org.json.JSONObject

data class StopAnnotation(val afterWord: Int, val mark: String)

/** Sourced punctuation overlay; never edits canonical letters, numbering or tajwid offsets. */
class WaqafAnnotations(context: Context) {
    private val raw = runCatching {
        context.assets.open("waqaf.json").bufferedReader().use { JSONObject(it.readText()) }
    }.getOrNull()
    val available get() = raw != null
    val attribution = raw?.optString("attribution").orEmpty()
    private val verses = raw?.optJSONObject("verses")
    private val omitted = raw?.optJSONArray("omittedReferences")?.let { list ->
        (0 until list.length()).map { list.getString(it) }.toSet()
    }.orEmpty()
    fun verified(verse: Verse) = available && "${verse.surah}:${verse.ayah}" !in omitted
    fun forVerse(verse: Verse): List<StopAnnotation> {
        val count = Regex("\\S+").findAll(verse.arabic).count()
        val values = verses?.optJSONArray(verse.number.toString()) ?: return emptyList()
        return (0 until values.length()).mapNotNull { index ->
            val row = values.getJSONObject(index)
            val word = row.optInt("afterWord")
            val mark = row.optString("mark")
            if (word in 1..count && WaqafGuide.signs.any { it.mark == mark }) StopAnnotation(word, mark) else null
        }
    }
}

fun annotatedWaqafText(base: AnnotatedString, stops: List<StopAnnotation>): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    val byWord = stops.groupBy { it.afterWord }
    Regex("\\S+").findAll(base.text).forEachIndexed { index, word ->
        val end = word.range.last + 1
        // Appending slices preserves the original word's tajwid and audio-highlight spans.
        append(base.subSequence(cursor, end))
        byWord[index + 1].orEmpty().forEach { stop ->
            append(" ")
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(stop.mark)
            pop()
        }
        cursor = end
    }
    append(base.subSequence(cursor, base.length))
}
