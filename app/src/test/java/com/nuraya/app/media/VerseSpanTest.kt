package com.nuraya.app.media

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.quranreading.shouldFollowBookAudio
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VerseSpanTest {
    private val quran get() = QuranRepository(ApplicationProvider.getApplicationContext<Application>())
    @Test fun crossSurahRepeatsKeepCanonicalOrderAndDoNotAddNumberedBasmalah() {
        val quran = quran
        val actual = verseSpanSequence(quran, 8, 74, 9, 2, 2, 2).map { it.surah to it.ayah }
        val pass = listOf(8 to 74, 8 to 74, 8 to 75, 8 to 75, 9 to 1, 9 to 1, 9 to 2, 9 to 2)
        assertEquals(pass + pass, actual)
        val all = verseSpanSequence(quran, 1, 1, 114, 6, 1, 1)
        assertEquals(quran.allVerses, all)
        assertEquals(6236, all.size)
        assertFalse(validVerseSpan(quran, 2, 1, 1, 7, 1, 1))
        assertFalse(validVerseSpan(quran, 0, 1, 114, 6, 1, 1))
        assertFalse(validVerseSpan(quran, 1, 8, 2, 1, 1, 1))
        assertFalse(validVerseSpan(quran, 1, 1, 114, 6, 20, 1))
        assertFalse(validVerseSpan(quran, 1, 1, 1, 7, 20, 2))
    }
    @Test fun sameSurahRangeRemainsCompatibleWithExistingQueueOrder() {
        val quran = quran
        for (each in 1..4) for (passes in 1..4) {
            val expected = repeatedVerseSequence(2, 5, each, passes, 7)
            assertEquals(expected, verseSpanSequence(quran, 1, 2, 1, 5, each, passes).map { it.ayah })
        }
    }
    @Test fun crossChapterFollowingRequiresAnExplicitlyStartedBookQueue() {
        val previous = PlaybackState(surahId = 113, ayahId = 5, verseMode = true, isPlaying = true)
        val next = previous.copy(surahId = 114, ayahId = 1)
        assertFalse(shouldFollowBookAudio(113, previous, next, true))
        assertTrue(shouldFollowBookAudio(113, previous, next, true, true))
        assertFalse(shouldFollowBookAudio(67, previous, next, true, true))
        assertFalse(shouldFollowBookAudio(113, previous.copy(isPlaying = false), next, true, true))
        assertFalse(shouldFollowBookAudio(113, previous, next, false, true))
        assertFalse(shouldFollowBookAudio(113, previous, next.copy(error = "offline"), true, true))
    }
}
