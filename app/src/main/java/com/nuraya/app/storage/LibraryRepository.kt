package com.nuraya.app.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Personal data stays on this device; no account or cloud service is required. */
class LibraryRepository(private val dao: LibraryDao) {
    // Old releases may have saved removed content types. Preserve their rows for
    // upgrades, but never offer them as current app destinations.
    val entries: Flow<List<UserEntry>> = dao.observeAll().map { rows -> rows.filter { it.type in entryTypes } }
    private val mutations = Mutex()

    fun observe(type: String): Flow<List<UserEntry>> {
        require(type in entryTypes) { "Unsupported collection type" }
        return dao.observe(type)
    }

    suspend fun save(
        type: String,
        key: String,
        title: String,
        subtitle: String = "",
        extra: String = "",
    ) = mutations.withLock {
        require(type in entryTypes) { "Unsupported collection type" }
        require(key.isNotBlank() && key.length <= 500) { "Invalid collection identifier" }
        dao.upsert(UserEntry("$type:$key", type, key, title, subtitle, extra))
        if (type.endsWith("_history")) dao.trimHistory(type)
    }

    suspend fun remove(type: String, key: String) = mutations.withLock {
        dao.delete("$type:$key")
    }

    /** Returns true when the item was added, false when it was removed. */
    suspend fun toggle(
        type: String,
        key: String,
        title: String,
        subtitle: String = "",
        extra: String = "",
    ): Boolean = mutations.withLock {
        require(type in entryTypes) { "Unsupported collection type" }
        require(key.isNotBlank() && key.length <= 500) { "Invalid collection identifier" }
        val id = "$type:$key"
        if (dao.get(id) == null) {
            dao.upsert(UserEntry(id, type, key, title, subtitle, extra))
            true
        } else {
            dao.delete(id)
            false
        }
    }

    suspend fun clearHistory() = mutations.withLock {
        listOf("read_history", "audio_history", "verse_audio_history", "video_history").forEach { dao.clear(it) }
    }

    companion object {
        val entryTypes = setOf(
            "bookmark", "read_history", "audio_history", "verse_audio_history", "audio_favorite", "playlist",
        )
    }
}
