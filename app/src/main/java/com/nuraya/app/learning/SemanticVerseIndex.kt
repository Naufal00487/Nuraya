package com.nuraya.app.learning

import android.content.Context
import org.json.JSONObject
import java.io.DataInputStream
import kotlin.math.ln
import kotlin.math.sqrt

/** Statistical meaning retrieval trained only on the bundled translations and attributed tafsir. */
class SemanticVerseIndex(context: Context) {
    private val assets = context.applicationContext.assets
    private data class Term(val idf: Float, val scale: Float, val vector: ByteArray)
    private data class Model(val dimensions: Int, val terms: Map<String, Term>, val scales: FloatArray, val documents: Array<ByteArray>)
    private val metadata by lazy { assets.open("semantic_index_sources.json").bufferedReader().use { JSONObject(it.readText()) } }
    private val stop by lazy { metadata.getJSONArray("stopWords").let { rows -> (0 until rows.length()).map { rows.getString(it) }.toSet() } }
    private val aliases by lazy { metadata.getJSONObject("aliases").let { root -> root.keys().asSequence().associateWith { root.getString(it) } } }
    private val model by lazy {
        DataInputStream(assets.open("semantic_index.bin").buffered()).use { input ->
            require(input.readInt() == 0x4e53454d)
            val dims = input.readUnsignedShort(); val count = input.readUnsignedShort(); val vocab = input.readInt()
            require(dims == 128 && count == 6236 && vocab in 1..20_000)
            val terms = HashMap<String, Term>(vocab)
            repeat(vocab) {
                val text = ByteArray(input.readUnsignedShort().also { require(it in 1..512) }).also(input::readFully).toString(Charsets.UTF_8)
                val idf = input.readFloat(); val scale = input.readFloat()
                require(idf.isFinite() && scale.isFinite() && scale > 0)
                terms[text] = Term(idf, scale, ByteArray(dims).also(input::readFully))
            }
            val scales = FloatArray(count)
            val docs = Array(count) { i -> scales[i] = input.readFloat(); require(scales[i].isFinite() && scales[i] > 0); ByteArray(dims).also(input::readFully) }
            require(input.read() == -1)
            Model(dims, terms, scales, docs)
        }
    }

    fun queryTerms(query: String): List<String> = LocalLearningRepository.normalize(query).split(' ')
        .filter { it.length >= 2 && it !in stop }.flatMap { (aliases[it] ?: it).split(' ') }.filter { it.length >= 2 && it !in stop }.take(32)

    /** Cosine scores are relevance hints, never probability of a religious answer being correct. */
    fun scores(query: String): FloatArray {
        val model = model
        val vector = FloatArray(model.dimensions)
        for ((word, frequency) in queryTerms(query).groupingBy { it }.eachCount()) {
            val term = model.terms[word] ?: continue
            val weight = (1 + ln(frequency.toDouble())).toFloat() * term.idf * term.scale
            for (d in vector.indices) vector[d] += term.vector[d] * weight
        }
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (norm <= 0) return FloatArray(6236)
        for (d in vector.indices) vector[d] /= norm
        return FloatArray(6236) { i ->
            val doc = model.documents[i]; var dot = 0f; var length = 0f
            for (d in vector.indices) { val value = doc[d] * model.scales[i]; dot += value * vector[d]; length += value * value }
            if (length > 0) (dot / sqrt(length)).coerceIn(-1f, 1f) else 0f
        }
    }
}
