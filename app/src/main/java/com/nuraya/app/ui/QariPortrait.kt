package com.nuraya.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class QariPortraitEntry(
    val id: String,
    val name: String,
    val qariIds: Set<String>,
    val asset: String,
    val sourceName: String,
    val sourcePage: String,
    val sha256: String,
    val bytes: Long,
)

/** Explicit identity mappings: similar names must never receive another reciter's face. */
class QariPortraitCatalog(context: Context) {
    val entries: List<QariPortraitEntry> = context.assets.open("portraits/catalog.json").bufferedReader().use { reader ->
        val rows = JSONObject(reader.readText()).getJSONArray("portraits")
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val ids = row.getJSONArray("qariIds")
            QariPortraitEntry(row.getString("id"), row.getString("name"),
                (0 until ids.length()).map { ids.getString(it) }.toSet(), row.getString("asset"),
                row.getString("sourceName"), row.getString("sourcePage"), row.getString("sha256"), row.getLong("bytes"))
        }.also { portraits ->
            require(portraits.flatMap { it.qariIds }.let { it.size == it.distinct().size })
            require(portraits.all { it.asset.matches(Regex("portraits/[a-z]+\\.jpg")) })
        }
    }

    private val byQari = entries.flatMap { entry -> entry.qariIds.map { it to entry } }.toMap()
    fun find(qariId: String): QariPortraitEntry? = byQari[qariId]

    companion object {
        @Volatile private var instance: QariPortraitCatalog? = null
        fun shared(context: Context): QariPortraitCatalog = instance ?: synchronized(this) {
            instance ?: QariPortraitCatalog(context.applicationContext).also { instance = it }
        }
    }
}

@Composable
fun QariPortrait(qariId: String, name: String, language: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember(qariId) { QariPortraitCatalog.shared(context).find(qariId) }
    val picture by produceState<ImageBitmap?>(null, entry?.asset) {
        value = withContext(Dispatchers.IO) {
            entry?.let { runCatching { context.assets.open(it.asset).use(BitmapFactory::decodeStream)?.asImageBitmap() }.getOrNull() }
        }
    }
    val description = if (entry != null && picture != null) label(language,
        "Foto ${entry.name}. Sumber ${entry.sourceName}.", "Photograph of ${entry.name}. Source: ${entry.sourceName}.")
    else label(language, "Inisial $name. Foto belum tersedia.", "$name initials. Photograph unavailable.")
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
        .testTag("qari_portrait_$qariId").semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        if (picture != null) Image(picture!!, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        else Text(name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }
            .ifBlank { "Q" }, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun QariPortraitCredit(qariId: String, language: String) {
    val context = LocalContext.current
    val entry = remember(qariId) { QariPortraitCatalog.shared(context).find(qariId) }
    Text(if (entry == null) label(language, "Foto qari ini belum tersedia.", "This reciter's photograph is not available.")
        else label(language, "Foto: ${entry.sourceName} · hak milik pemilik foto.", "Photo: ${entry.sourceName} · rights remain with its owner."),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
