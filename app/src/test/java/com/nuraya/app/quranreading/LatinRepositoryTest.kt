package com.nuraya.app.quranreading

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LatinRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun everyCanonicalReferenceHasOnePublishedNonemptyTransliteration() {
        val latin = LatinRepository(context)
        val quran = QuranRepository(context)
        var total = 0
        quran.surahs.forEach { surah ->
            val verses = latin.chapter(surah.id)
            assertEquals("Surah ${surah.id}", surah.ayahCount, verses.size)
            assertEquals((1..surah.ayahCount).toList(), verses.map { it.ayah })
            verses.forEach { row ->
                assertEquals(surah.id, row.surah)
                assertTrue("${row.surah}:${row.ayah}", row.latin.isNotBlank())
                assertNotNull(quran.verse(row.surah, row.ayah))
            }
            total += verses.size
        }
        assertEquals(6236, total)
    }

    @Test fun latinOpeningsKeepTheOriginalNumberedVersePolicy() {
        val latin = LatinRepository(context)
        assertEquals("Bismillāhir-raḥmānir-raḥīm(i).", latin.verse(1, 1)?.latin)
        assertEquals("Alif lām mīm.", latin.verse(2, 1)?.latin)
        assertTrue(requireNotNull(latin.verse(9, 1)).latin.startsWith("Barā'atum"))
        assertEquals("Qul a‘ūżu birabbin-nās(i).", latin.verse(114, 1)?.latin)
        assertFalse(requireNotNull(latin.verse(2, 1)).latin.startsWith("Bismill"))
        assertFalse(requireNotNull(latin.verse(9, 1)).latin.startsWith("Bismill"))
    }

    @Test fun invalidReferencesNeverFallBackToAnotherVerse() {
        val latin = LatinRepository(context)
        assertNull(latin.verse(0, 1))
        assertNull(latin.verse(115, 1))
        assertNull(latin.verse(1, 0))
        assertNull(latin.verse(1, 8))
        assertNull(latin.verse(114, Int.MAX_VALUE))
        assertSame(LatinRepository.shared(context), LatinRepository.shared(context))
    }
}
