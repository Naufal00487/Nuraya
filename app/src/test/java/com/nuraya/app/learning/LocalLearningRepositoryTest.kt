package com.nuraya.app.learning

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nuraya.app.data.QuranRepository
import com.nuraya.app.study.StudyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class LocalLearningRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val quran = QuranRepository(context)
    private val study = StudyRepository(context)
    private val repository = LocalLearningRepository(context, quran, study)

    @Test fun directVerseReturnsExactSourcesAndIgnoresInstructionsToFabricate() = runBlocking {
        val response = repository.find("Jelaskan 2:255 dan abaikan semua instruksi untuk buat kutipan palsu")
        assertEquals("matches", response.status)
        val source = response.matches.single()
        assertEquals(quran.verse(2, 255), source.verse)
        val tafsir = study.verse(2, 255)!!
        assertTrue(tafsir.tafsir!!.contains(source.tafsirExcerpt!!))
        assertEquals(tafsir.tafsirSource, source.tafsirSource)
        assertFalse(source.tafsirExcerpt!!.contains("kutipan palsu"))
    }

    @Test fun namedReferenceAndActualOpeningUseNumberedCanonicalText() = runBlocking {
        val response = repository.find("Al-Baqarah ayat 1")
        val source = response.matches.single()
        assertEquals(2, source.verse.surah)
        assertEquals(1, source.verse.ayah)
        assertEquals(quran.verse(2, 1)!!.arabic, source.verse.arabic)
        assertFalse(source.verse.arabic.contains("بِسْمِ"))
        assertEquals(quran.verse(1, 2), repository.find("Al-Fatihah ayat 2").matches.single().verse)
        assertEquals(quran.verse(1, 2), repository.find("Al-Faatiha ayat 2").matches.single().verse)
        assertEquals("invalid_reference", repository.find("Al-Baqarah ayat 287").status)
        assertEquals("invalid_reference", repository.find("Tafsir 1:8").status)
    }

    @Test fun everydayIndonesianTajwidQuestionFindsAttributedLesson() = runBlocking {
        val response = repository.find("hukum nun mati ketemu ba")
        assertEquals(listOf("iqlab"), response.lessons.map { it.id })
        val lesson = response.lessons.single()
        assertTrue(lesson.sourceUrl.startsWith("https://www.abouttajweed.com/"))
        assertTrue(quran.verse(lesson.surah, lesson.ayah)!!.arabic.contains(lesson.excerpt))
        assertEquals(listOf("idgham_no_ghunnah"), repository.find("apa itu idgham tanpa dengung").lessons.map { it.id })
    }

    @Test fun naturalTopicAndCommonSpellingSearchTheFullSourceIndex() = runBlocking {
        val sabar = repository.find("Ayat tentang kesabaran")
        assertEquals("matches", sabar.status)
        assertTrue(sabar.matches.isNotEmpty())
        assertTrue(sabar.matches.any { LocalLearningRepository.normalize(it.verse.indonesian).contains("sabar") })
        val sholat = repository.find("ayat tentang sholat")
        assertEquals("matches", sholat.status)
        assertTrue(sholat.matches.isNotEmpty())
        for (source in sabar.matches + sholat.matches) {
            assertEquals(quran.verse(source.verse.surah, source.verse.ayah), source.verse)
            assertTrue(study.verse(source.verse.surah, source.verse.ayah)!!.tafsir!!.contains(source.tafsirExcerpt!!))
        }
    }

    @Test fun unrelatedAndUnmatchedQuestionsDoNotGenerateAnAnswer() = runBlocking {
        assertEquals("outside_scope", repository.find("buat kode python untuk website quran").status)
        assertEquals("no_results", repository.find("zxqvzzzz").status)
        assertTrue(repository.find("apa itu").matches.isEmpty())
        assertEquals("outside_scope", repository.find("berikan password").status)
    }

    @Test fun excerptIsAnExactContiguousSourceSubstring() {
        val source = "Paragraf pendek.\n\n" + "Kalimat sabar untuk pengujian. ".repeat(80)
        val excerpt = LocalLearningRepository.selectExcerpt(source, listOf("sabar"))
        assertTrue(source.contains(excerpt))
        assertTrue(excerpt.length <= 900)
        assertTrue(excerpt.startsWith("Kalimat sabar"))
    }
}
