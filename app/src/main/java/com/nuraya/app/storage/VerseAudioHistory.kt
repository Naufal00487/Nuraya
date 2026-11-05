package com.nuraya.app.storage

/** A history key carries a reciter, surah and ayah; it is not a whole-surah audio key. */
data class VerseAudioHistory(val qariId: String, val surah: Int, val ayah: Int)

fun resolveVerseAudioHistory(
    entry: UserEntry,
    availableQariIds: Set<String>,
    verseExists: (Int, Int) -> Boolean,
): VerseAudioHistory? {
    if (entry.type != "verse_audio_history") return null
    val parts = entry.key.split(':')
    if (parts.size != 3 || parts[0] !in availableQariIds) return null
    // Old metadata may omit extra, but conflicting identities must never choose another voice.
    if (entry.extra.isNotEmpty() && entry.extra != parts[0]) return null
    val surah = parts[1].toIntOrNull() ?: return null
    val ayah = parts[2].toIntOrNull() ?: return null
    if (!verseExists(surah, ayah)) return null
    return VerseAudioHistory(parts[0], surah, ayah)
}
