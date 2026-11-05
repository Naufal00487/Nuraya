package com.nuraya.app.media

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VerseAudioCatalogTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun allTimedChaptersUseTheRecordingPairedWithTheirOwnRealSegments() {
        val catalog = VerseAudioCatalog(context)
        val quran = QuranRepository(context)
        val reciter = catalog.reciters.single { it.id == "ar.alafasy" }
        assertTrue(reciter.supportsWordSync)
        var verses = 0
        var segments = 0
        quran.surahs.forEach { surah ->
            val chapter = requireNotNull(catalog.timing(reciter, surah.id))
            assertEquals(surah.ayahCount, chapter.verses.size)
            assertEquals("https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/${surah.id}.mp3", chapter.audioUrl)
            chapter.verses.forEach { verse ->
                assertTrue(verse.endMs > verse.startMs)
                assertTrue(verse.words.all { it.position > 0 && it.startMs < it.endMs })
                if (verse.usable) assertTrue("Clip must contain every word without truncation",
                    verse.words.all { it.startMs >= verse.startMs && it.endMs <= verse.endMs })
                segments += verse.words.size
                verses++
            }
        }
        assertEquals(6236, verses)
        assertTrue(segments > 77000)
    }

    @Test fun silenceAndUnmappedTimesDoNotInventWordHighlights() {
        val timing = VerseTiming(1, 100, 500, listOf(WordTiming(1, 120, 190), WordTiming(2, 230, 380)))
        assertEquals(0, timing.wordAt(119))
        assertEquals(1, timing.wordAt(120))
        assertEquals(1, timing.wordAt(189))
        assertEquals(0, timing.wordAt(190))
        assertEquals(0, timing.wordAt(220))
        assertEquals(2, timing.wordAt(230))
        assertEquals(0, timing.wordAt(380))
        assertEquals(0, timing.wordAt(900))
    }

    @Test fun knownAlafasyTimingChangesAtRealFirstVerseBoundaries() {
        val catalog = VerseAudioCatalog(context)
        val verse = requireNotNull(catalog.timing(catalog.reciters.single { it.id == "ar.alafasy" }, 1)).verses.first()
        assertEquals(1, verse.wordAt(579))
        assertEquals(2, verse.wordAt(580))
        assertEquals(3, verse.wordAt(1409))
        assertEquals(4, verse.wordAt(2502))
        assertEquals(0, verse.wordAt(5840))
    }

    @Test fun inconsistentProviderTimestampsUseAyahFallbackRatherThanFalsePrecision() {
        val catalog = VerseAudioCatalog(context)
        val reciter = catalog.reciters.single { it.id == "ar.alafasy" }
        val malformedBoundary = requireNotNull(catalog.timing(reciter, 14)).verses[16]
        assertFalse(malformedBoundary.usable)
        assertEquals(0, malformedBoundary.wordAt(malformedBoundary.startMs))
        val incompatibleWordSegmentation = requireNotNull(catalog.timing(reciter, 37)).verses[129]
        assertFalse(incompatibleWordSegmentation.usable)
        assertEquals(0, incompatibleWordSegmentation.wordAt(incompatibleWordSegmentation.startMs))
        val firstChapter = requireNotNull(catalog.timing(reciter, 1)).verses
        assertEquals(6025L, firstChapter[1].startMs)
        assertEquals(firstChapter[0].endMs, firstChapter[1].startMs)
        assertTrue(firstChapter[1].startMs <= firstChapter[1].words.first().startMs)
    }

    @Test fun unsupportedRecitersNeverBorrowAlafasyTimestamps() {
        val catalog = VerseAudioCatalog(context)
        assertTrue(catalog.reciters.size >= 17)
        assertEquals(catalog.reciters.size, catalog.reciters.map { it.id }.toSet().size)
        catalog.reciters.filter { !it.supportsWordSync }.forEach { reciter ->
            assertNull(catalog.timing(reciter, 1))
            assertTrue(reciter.audioUrl(6236).endsWith("/${reciter.id}/6236.mp3"))
        }
    }

    @Test fun memorizationRepeatsEachAyahInOrderAndRejectsInvalidRanges() {
        assertEquals(listOf(2, 2, 2, 3, 3, 3, 4, 4, 4), repeatedAyahRange(2, 4, 3, 7))
        assertEquals(listOf(7), repeatedAyahRange(7, 7, 1, 7))
        listOf({ repeatedAyahRange(0, 2, 1, 7) }, { repeatedAyahRange(3, 2, 1, 7) },
            { repeatedAyahRange(1, 8, 1, 7) }, { repeatedAyahRange(1, 2, 0, 7) },
            { repeatedAyahRange(1, 2, 21, 7) }).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { invalid() }
        }
    }
}
