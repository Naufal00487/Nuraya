package com.nuraya.app.media

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
class CuratedRecitersTest {
    @Test fun twentyEightDistinctPeopleHaveBothChapterAndVerseSources() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val quran = QuranRepository(context)
        val catalog = VerseAudioCatalog(context)
        assertEquals(28, CuratedReciters.selections.size)
        assertEquals(28, CuratedReciters.selections.map { it.name }.toSet().size)
        for (choice in CuratedReciters.selections) {
            val chapter = quran.qariChoices.single { it.id == choice.chapterId }
            val verse = catalog.reciters.single { it.id == choice.verseId }
            assertEquals(choice.name, chapter.name)
            assertEquals(choice.name, verse.name)
            assertEquals(choice.verseId, CuratedReciters.verseIdFor(choice.chapterId))
            assertEquals(choice.chapterId, CuratedReciters.chapterIdFor(choice.verseId))
            assertTrue(chapter.audioUrl(1).endsWith("/001.mp3"))
            assertTrue(chapter.audioUrl(114).endsWith("/114.mp3"))
        }
    }
    @Test fun everyNewVerseUrlMatchesAll6236CanonicalReferencesIncludingTawbah() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val quran = QuranRepository(context)
        for (reciter in VerseAudioCatalog(context).reciters.filter { it.ayahBaseUrl != null }) {
            val urls = mutableSetOf<String>()
            for (verse in quran.allVerses) {
                val suffix = "/%03d%03d.mp3".format(java.util.Locale.ROOT, verse.surah, verse.ayah)
                assertTrue(reciter.audioUrl(verse.number).endsWith(suffix))
                assertTrue(urls.add(reciter.audioUrl(verse.number)))
            }
            assertEquals(6236, urls.size)
        }
    }
}
