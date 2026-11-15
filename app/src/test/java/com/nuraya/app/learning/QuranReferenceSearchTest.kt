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
class QuranReferenceSearchTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val quran = QuranRepository(context)
    private val search = QuranReferenceSearch(quran)
    private val repository = LocalLearningRepository(context, quran, StudyRepository(context))

    @Test fun everyCanonicalSurahNameAndNumberResolvesToItself() {
        quran.surahs.forEach { surah ->
            for (query in listOf(surah.name, surah.arabicName, "surat ${surah.id}", "surah ${surah.name} ayat 1")) {
                val result = search.find(query)!!
                assertEquals(query, listOf(surah.id), result.chapters.map { it.id })
                assertFalse(query, result.invalid)
                assertFalse(query, result.approximate)
            }
        }
    }

    @Test fun indonesianAliasesAndSpokenNumbersUseTheSameRepositoryAsTyping() = runBlocking {
        val cases = mapOf("surat Yasin" to 36, "ya sin" to 36, "Albaqarah" to 2, "Al-Baqoroh" to 2,
            "surah ke enam puluh tujuh" to 67, "surat satu satu empat" to 114,
            "surat seratus sebelas" to 111, "ar rohman" to 55, "Asy Syura" to 42,
            "al insyirah" to 94, "Ad Dhuha" to 93, "An Nas" to 114, "1" to 1)
        for ((query, expected) in cases) {
            val response = repository.find(query, semantic = true)
            assertEquals(query, listOf(expected), response.chapters.map { it.id })
            assertTrue(query, response.matches.isEmpty())
        }
        for (query in listOf("Al-Baqarah ayat dua ratus lima puluh lima", "surat dua ayat dua lima lima", "ayat kursi")) {
            assertEquals(query, quran.verse(2, 255), repository.find(query).matches.single().verse)
        }
    }

    @Test fun typosSuggestReferencesWithoutChangingCanonicalContent() = runBlocking {
        for (query in listOf("surat baqorah ayat 1", "al baqaroh ayat satu", "surat Yasim ayat satu")) {
            val response = repository.find(query)
            assertEquals(query, "matches", response.status)
            assertTrue(query, response.matches.isNotEmpty())
            response.matches.forEach { assertEquals(quran.verse(it.verse.surah, it.verse.ayah), it.verse) }
        }
        assertTrue(search.find("surat Yasim")!!.approximate)
        assertEquals(listOf(36), search.find("surat Yasim")!!.chapters.map { it.id })
    }

    @Test fun topicsAndUnrecognizedNamesAreNotGuessedAsSurahs() = runBlocking {
        for (query in listOf("sabar", "hijrah", "sedekah", "ayat tentang sabar", "surat tentang kesabaran", "cara sholat", "belajar makhraj")) {
            assertNull(query, search.find(query))
            assertTrue(query, repository.find(query, true).chapters.isEmpty())
        }
        assertEquals("no_results", repository.find("surat zxqvzzzz").status)
    }

    @Test fun invalidNumbersNeverSilentlyOpenAnotherReference() = runBlocking {
        for (query in listOf("surat 0", "surat 115", "surat -1", "-2:1", "2:-1", "2:287",
            "Al Fatihah ayat delapan", "surat dua ayat xxx", "surat dua ayat 0")) {
            assertEquals(query, "invalid_reference", repository.find(query).status)
        }
        assertNull(QuranReferenceSearch.spokenNumber(listOf("dua", "tentang", "sabar")))
    }

    @Test fun speechAlternativesCanRecoverNamesButNeverReplaceAClearTopic() {
        assertEquals("surat Yasin", search.preferredTranscript(listOf("surat Yasim", "surat Yasin")))
        assertEquals("ayat tentang sabar", search.preferredTranscript(listOf("ayat tentang sabar", "Saba")))
        assertEquals("hijrah", search.preferredTranscript(listOf("hijrah", "Al Hijr")))
        assertEquals("surat Al Mulk", search.preferredTranscript(listOf("surat Al Mulk", "surat Yasin")))
    }
}
