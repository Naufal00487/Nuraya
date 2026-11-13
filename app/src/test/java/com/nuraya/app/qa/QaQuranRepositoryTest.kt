package com.nuraya.app.qa

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs against the real packaged corpus, without any network service or app workers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QaQuranRepositoryTest {
    private lateinit var quran: QuranRepository

    @Before fun loadPackagedCorpus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        quran = QuranRepository(context)
    }

    @Test fun allChaptersAndVersesArePresentAndOrdered() {
        assertEquals((1..114).toList(), quran.surahs.map { it.id })
        val verses = quran.surahs.flatMap { surah ->
            val chapter = quran.verses(surah.id)
            assertEquals("Verse count for chapter ${surah.id}", surah.ayahCount, chapter.size)
            assertEquals((1..surah.ayahCount).toList(), chapter.map { it.ayah })
            assertTrue(chapter.all { it.surah == surah.id })
            assertFalse(surah.name.isBlank())
            assertFalse(surah.arabicName.isBlank())
            chapter
        }
        assertEquals(6236, verses.size)
        assertEquals((1..6236).toList(), verses.map { it.number })
        assertEquals(6236, verses.map { it.surah to it.ayah }.toSet().size)
        assertEquals(7, quran.verses(1).size)
        assertEquals(286, quran.verses(2).size)
        assertEquals(3, quran.verses(108).size)
        assertEquals(6, quran.verses(114).size)
    }

    @Test fun everyVerseHasArabicAndBothAttributedTranslationLanguages() {
        quran.surahs.flatMap { quran.verses(it.id) }.forEach { verse ->
            val ref = "${verse.surah}:${verse.ayah}"
            assertTrue("Arabic missing at $ref", verse.arabic.any { it in '\u0600'..'\u06ff' })
            assertTrue("Indonesian missing at $ref", verse.indonesian.isNotBlank())
            assertTrue("English missing at $ref", verse.english.isNotBlank())
            assertTrue("Juz outside 1–30 at $ref", verse.juz in 1..30)
            assertEquals(verse, quran.verse(verse.surah, verse.ayah))
        }
    }

    @Test fun thirtyJuzCoverCorpusExactlyOnceWithKnownBoundaries() {
        val juzVerses = (1..30).flatMap { number ->
            val verses = quran.juz(number)
            assertTrue("Juz $number must have verses", verses.isNotEmpty())
            assertTrue(verses.all { it.juz == number })
            verses
        }
        assertEquals((1..6236).toList(), juzVerses.map { it.number })
        assertEquals(1 to 1, quran.juz(1).first().let { it.surah to it.ayah })
        assertEquals(2 to 142, quran.juz(2).first().let { it.surah to it.ayah })
        assertEquals(78 to 1, quran.juz(30).first().let { it.surah to it.ayah })
        assertEquals(114 to 6, quran.juz(30).last().let { it.surah to it.ayah })
    }

    @Test fun validLookupReturnsExactVerseAndInvalidLookupDoesNotInventContent() {
        assertNotNull(quran.verse(1, 1))
        assertNull(quran.verse(0, 1))
        assertNull(quran.verse(115, 1))
        assertNull(quran.verse(1, 0))
        assertNull(quran.verse(1, 8))
    }

    @Test fun recitersHaveDistinctIdsAndSecureSurahUrls() {
        assertTrue("Load the expanded provider catalogue of complete recording editions", quran.qaris.size >= 100)
        assertEquals(quran.qaris.size, quran.qaris.map { it.id }.toSet().size)
        val previousReleaseIds = setOf(
            "ar.alafasy", "ar.abdulbasitmurattal", "ar.saudalshuraim",
            "ar.muhammadsiddiqalminshawimujawwad", "ar.ahmedalajmi", "ar.abdullahbasfar",
            "ar.aliabdurrahmanalhuthaify", "ar.faresabbad", "ar.muhammadayyub",
            "ar.nasseralqatami", "ar.yasseraldossari", "ar.haniarrifai",
        )
        assertTrue("Existing downloads and favourites must retain their reciter identifiers",
            quran.qaris.map { it.id }.toSet().containsAll(previousReleaseIds))
        quran.qaris.forEach { qari ->
            assertTrue(qari.name.isNotBlank())
            assertTrue(qari.sourceUrl.startsWith("https://"))
            assertTrue(qari.audioUrl(1).startsWith("https://"))
            assertTrue(qari.audioUrl(114).startsWith("https://"))
            assertNotEquals(qari.audioUrl(1), qari.audioUrl(114))
        }
    }

    @Test fun readingSearchAndCitationsUseActualNumberedVerses() {
        assertEquals(112, quran.allVerses.count { it.openingBasmalah != null })
        assertEquals("الٓمٓ", quran.verse(2, 1)?.arabic)
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", quran.verse(112, 1)?.arabic)
        assertEquals("الٓمٓ", quran.search("2:1").single().arabic)
        assertEquals(setOf(1 to 1, 27 to 30),
            quran.search("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").map { it.surah to it.ayah }.toSet())
        assertNull(quran.verse(1, 1)?.openingBasmalah)
        assertNull(quran.verse(9, 1)?.openingBasmalah)
    }

    @Test fun theKnownAlafasyAliasHasOneVisibleCardWhileExistingIdsStillResolve() {
        val allIds = quran.qaris.map { it.id }.toSet()
        val visibleIds = quran.qariChoices.map { it.id }.toSet()
        assertTrue("ar.alafasy" in visibleIds)
        assertTrue("ar.misharyrashidalafasy" in allIds)
        assertFalse("ar.misharyrashidalafasy" in visibleIds)
        assertEquals(com.nuraya.app.media.CuratedReciters.chapterIds.toSet(), visibleIds)
        assertEquals(quran.qariChoices.size, visibleIds.size)
        // One visible edition per selected person; old identifiers still resolve for saved data.
        assertTrue("ar.abdulbasitmurattal" in visibleIds)
        assertFalse("ar.abdulbasitmujawwad" in visibleIds)
    }
}
