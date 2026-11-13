package com.nuraya.app.quranreading

import com.nuraya.app.media.PlaybackState

/** Opening a book is an explicit navigation request, never a request to restore old audio. */
internal fun shouldFollowBookAudio(surah: Int, previous: PlaybackState, current: PlaybackState, enabled: Boolean): Boolean =
    enabled && current.verseMode && current.isPlaying && current.error == null &&
        current.surahId == surah && current.ayahId > 0 &&
        (previous.surahId != current.surahId || previous.ayahId != current.ayahId || !previous.isPlaying)
